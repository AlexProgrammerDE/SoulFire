/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.renderer;

import lombok.experimental.UtilityClass;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;

/// Performs ray casting operations for the software renderer.
/// Uses DDA voxel traversal for blocks and ray-AABB intersection for entities.
@UtilityClass
public class RayCaster {

  /// Casts a single ray and returns the resulting pixel color.
  ///
  /// @param ctx  The render context containing all necessary data
  /// @param dirX Normalized ray direction X
  /// @param dirY Normalized ray direction Y
  /// @param dirZ Normalized ray direction Z
  /// @return The ARGB color for this ray
  public static int castRay(RenderContext ctx, double dirX, double dirY, double dirZ) {
    // Pre-compute inverse directions for AABB tests
    var invDirX = 1.0 / dirX;
    var invDirY = 1.0 / dirY;
    var invDirZ = 1.0 / dirZ;

    // Check for entity hits first (they're usually fewer and closer)
    var closestEntityDist = Double.MAX_VALUE;
    for (var entity : ctx.sceneData().entities()) {
      var dist = rayAABBIntersect(ctx.camera(), invDirX, invDirY, invDirZ, entity.bbox(), ctx.maxDistance());
      if (dist >= 0 && dist < closestEntityDist) {
        closestEntityDist = dist;
      }
    }

    // Check for map frame hits
    var closestMapDist = Double.MAX_VALUE;
    SceneData.MapFrameData closestMapFrame = null;
    for (var mapFrame : ctx.sceneData().mapFrames()) {
      var dist = rayAABBIntersect(ctx.camera(), invDirX, invDirY, invDirZ, mapFrame.bbox(), ctx.maxDistance());
      if (dist >= 0 && dist < closestMapDist) {
        closestMapDist = dist;
        closestMapFrame = mapFrame;
      }
    }

    // DDA voxel traversal
    var posX = ctx.camera().eyeX();
    var posY = ctx.camera().eyeY();
    var posZ = ctx.camera().eyeZ();

    var mapX = (int) Math.floor(posX);
    var mapY = (int) Math.floor(posY);
    var mapZ = (int) Math.floor(posZ);

    var deltaDistX = Math.abs(invDirX);
    var deltaDistY = Math.abs(invDirY);
    var deltaDistZ = Math.abs(invDirZ);

    var stepX = dirX >= 0 ? 1 : -1;
    var stepY = dirY >= 0 ? 1 : -1;
    var stepZ = dirZ >= 0 ? 1 : -1;

    var sideDistX = dirX >= 0 ? (mapX + 1 - posX) * deltaDistX : (posX - mapX) * deltaDistX;
    var sideDistY = dirY >= 0 ? (mapY + 1 - posY) * deltaDistY : (posY - mapY) * deltaDistY;
    var sideDistZ = dirZ >= 0 ? (mapZ + 1 - posZ) * deltaDistZ : (posZ - mapZ) * deltaDistZ;

    var distance = 0.0;
    var side = 0;

    // Mutable BlockPos to avoid allocations
    var blockPos = new BlockPos.MutableBlockPos();

    while (distance < ctx.maxDistance()) {
      // Early exit if we've passed all entity/map hits
      var minOtherHit = Math.min(closestEntityDist, closestMapDist);
      if (distance > minOtherHit) {
        if (closestEntityDist < closestMapDist) {
          return applyFog(RenderConstants.ENTITY_HITBOX_COLOR, closestEntityDist, ctx.invMaxDistance());
        } else {
          return getMapColor(ctx, closestMapFrame, dirX, dirY, dirZ, closestMapDist);
        }
      }

      // Step to next voxel
      if (sideDistX < sideDistY && sideDistX < sideDistZ) {
        distance = sideDistX;
        sideDistX += deltaDistX;
        mapX += stepX;
        side = 0;
      } else if (sideDistY < sideDistZ) {
        distance = sideDistY;
        sideDistY += deltaDistY;
        mapY += stepY;
        side = 1;
      } else {
        distance = sideDistZ;
        sideDistZ += deltaDistZ;
        mapZ += stepZ;
        side = 2;
      }

      // Check world bounds
      if (mapY < ctx.minY() || mapY > ctx.maxY()) {
        if (minOtherHit < distance) {
          if (closestEntityDist < closestMapDist) {
            return applyFog(RenderConstants.ENTITY_HITBOX_COLOR, closestEntityDist, ctx.invMaxDistance());
          } else {
            return getMapColor(ctx, closestMapFrame, dirX, dirY, dirZ, closestMapDist);
          }
        }
        return mapY < ctx.minY() ? RenderConstants.VOID_COLOR : RenderConstants.SKY_COLOR;
      }

      // Get block state
      blockPos.set(mapX, mapY, mapZ);
      var blockState = ctx.level().getBlockState(blockPos);

      if (!blockState.isAir() && blockState.getBlock() != Blocks.VOID_AIR && blockState.isSolidRender()) {
        // Check if entity/map hit is closer
        if (minOtherHit < distance) {
          if (closestEntityDist < closestMapDist) {
            return applyFog(RenderConstants.ENTITY_HITBOX_COLOR, closestEntityDist, ctx.invMaxDistance());
          } else {
            return getMapColor(ctx, closestMapFrame, dirX, dirY, dirZ, closestMapDist);
          }
        }

        return getBlockColor(ctx.level(), blockPos, blockState, side, stepY, distance, ctx.invMaxDistance());
      }
    }

    // Check for hits at max distance
    if (closestEntityDist < ctx.maxDistance()) {
      return applyFog(RenderConstants.ENTITY_HITBOX_COLOR, closestEntityDist, ctx.invMaxDistance());
    }
    if (closestMapDist < ctx.maxDistance()) {
      return getMapColor(ctx, closestMapFrame, dirX, dirY, dirZ, closestMapDist);
    }

    return RenderConstants.SKY_COLOR;
  }

  /// Fast inverse square root (Quake III style, but with double precision).
  public static double fastInvSqrt(double x) {
    var xhalf = 0.5 * x;
    var i = Double.doubleToLongBits(x);
    i = 0x5fe6eb50c7b537a9L - (i >> 1);
    x = Double.longBitsToDouble(i);
    x *= 1.5 - xhalf * x * x; // Newton-Raphson iteration
    return x;
  }

  private static double rayAABBIntersect(
    Camera camera,
    double invDirX,
    double invDirY,
    double invDirZ,
    AABB box,
    double maxDist) {

    var ox = camera.eyeX();
    var oy = camera.eyeY();
    var oz = camera.eyeZ();

    var t1 = (box.minX - ox) * invDirX;
    var t2 = (box.maxX - ox) * invDirX;
    var t3 = (box.minY - oy) * invDirY;
    var t4 = (box.maxY - oy) * invDirY;
    var t5 = (box.minZ - oz) * invDirZ;
    var t6 = (box.maxZ - oz) * invDirZ;

    var tMin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
    var tMax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

    if (tMax < 0 || tMin > tMax) {
      return -1;
    }

    var t = tMin >= 0 ? tMin : tMax;
    return t <= maxDist ? t : -1;
  }

  private static int getBlockColor(
    ClientLevel level,
    BlockPos pos,
    BlockState blockState,
    int side,
    int stepY,
    double distance,
    double invMaxDistance) {

    var mapColor = blockState.getMapColor(level, pos);
    var baseColor = mapColor.col;

    // Shading based on face
    var shadeFactor = switch (side) {
      case 0 -> 0.8;
      case 1 -> stepY > 0 ? 1.0 : 0.5;
      case 2 -> 0.9;
      default -> 1.0;
    };

    // Distance fog
    shadeFactor *= Math.max(0.0, 1.0 - distance * invMaxDistance * 0.5);

    var r = (int) (((baseColor >> 16) & 0xFF) * shadeFactor);
    var g = (int) (((baseColor >> 8) & 0xFF) * shadeFactor);
    var b = (int) ((baseColor & 0xFF) * shadeFactor);

    return 0xFF000000 | (r << 16) | (g << 8) | b;
  }

  private static int getMapColor(
    RenderContext ctx,
    SceneData.MapFrameData mapFrame,
    double dirX,
    double dirY,
    double dirZ,
    double distance) {

    if (mapFrame == null) {
      return RenderConstants.SKY_COLOR;
    }

    var camera = ctx.camera();

    // Calculate hit point
    var hitX = camera.eyeX() + dirX * distance;
    var hitY = camera.eyeY() + dirY * distance;
    var hitZ = camera.eyeZ() + dirZ * distance;

    var direction = mapFrame.direction();
    double u;
    double v;

    if (direction == Direction.UP || direction == Direction.DOWN) {
      u = hitX - (mapFrame.posX() - 0.5);
      v = hitZ - (mapFrame.posZ() - 0.5);
      if (direction == Direction.DOWN) {
        v = 1.0 - v;
      }
    } else {
      var localX = hitX - mapFrame.posX();
      var localY = hitY - mapFrame.posY();
      var localZ = hitZ - mapFrame.posZ();

      u = switch (direction) {
        case NORTH -> 0.5 - localX;
        case SOUTH -> 0.5 + localX;
        case WEST -> 0.5 + localZ;
        case EAST -> 0.5 - localZ;
        default -> 0.5;
      };
      v = 0.5 - localY;
    }

    // Apply rotation
    var rotation = mapFrame.rotation();
    for (var i = 0; i < rotation; i++) {
      var oldU = u;
      u = v;
      v = 1.0 - oldU;
    }

    // Clamp and get pixel
    var pixelX = Math.max(0, Math.min(127, (int) (u * 128)));
    var pixelY = Math.max(0, Math.min(127, (int) (v * 128)));

    var colorIndex = mapFrame.colors()[pixelX + pixelY * 128];
    var color = MapColor.getColorFromPackedId(colorIndex);

    return applyFog(color, distance, ctx.invMaxDistance());
  }

  private static int applyFog(int color, double distance, double invMaxDistance) {
    var fogFactor = Math.max(0.5, 1.0 - distance * invMaxDistance * 0.3);
    var r = (int) (((color >> 16) & 0xFF) * fogFactor);
    var g = (int) (((color >> 8) & 0xFF) * fogFactor);
    var b = (int) ((color & 0xFF) * fogFactor);
    return 0xFF000000 | (r << 16) | (g << 8) | b;
  }
}
