/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.soulfiremc.server.command.CommandSourceStack;
import com.soulfiremc.server.util.SFPathConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.soulfiremc.server.command.brigadier.BrigadierHelper.*;

public final class ExportBasicRenderCommand {
  private static final int DEFAULT_WIDTH = 854;
  private static final int DEFAULT_HEIGHT = 480;
  private static final double DEFAULT_FOV = 70.0;
  private static final int SKY_COLOR = 0xFF87CEEB; // Light blue sky
  private static final int VOID_COLOR = 0xFF1A1A2E; // Dark void color
  private static final int ENTITY_HITBOX_COLOR = 0xFFFF0000; // Red for entity hitboxes

  private ExportBasicRenderCommand() {}

  private static int exportBasicRender(
    CommandContext<CommandSourceStack> context,
    int width,
    int height) throws CommandSyntaxException {
    var currentTime = System.currentTimeMillis();
    return forEveryBot(
      context,
      bot -> {
        var level = bot.minecraft().level;
        var player = bot.minecraft().player;
        if (level == null || player == null) {
          context.getSource().source().sendWarn("No level loaded!");
          return Command.SINGLE_SUCCESS;
        }

        // Get render distance from settings (in chunks) and convert to blocks
        var renderDistanceChunks = bot.minecraft().options.getEffectiveRenderDistance();
        var maxDistance = renderDistanceChunks * 16;

        context.getSource().source().sendInfo("Rendering {}x{} image (render distance: {} chunks / {} blocks)...",
          width, height, renderDistanceChunks, maxDistance);

        var renderStart = System.currentTimeMillis();

        var eyePos = player.getEyePosition();
        var yRot = player.getYRot();
        var xRot = player.getXRot();

        // Collect all item frames with maps
        var mapFrames = collectMapItemFrames(level, eyePos, maxDistance);

        // Collect all other entities (excluding item frames and local player)
        var entities = collectEntities(level, player, eyePos, maxDistance);

        var image = renderScene(level, eyePos, yRot, xRot, width, height, DEFAULT_FOV, maxDistance, mapFrames, entities);

        var renderTime = System.currentTimeMillis() - renderStart;
        context.getSource().source().sendInfo("Render completed in {}ms", renderTime);

        var fileName = "render_%d_%s.png".formatted(currentTime, bot.accountName());
        try {
          var rendersDirectory = SFPathConstants.getRendersDirectory(bot.instanceManager().getInstanceObjectStoragePath());
          Files.createDirectories(rendersDirectory);
          var file = rendersDirectory.resolve(fileName);
          ImageIO.write(image, "png", file.toFile());
          context.getSource().source().sendInfo("Exported render to {}", file);
        } catch (IOException e) {
          context.getSource().source().sendError("Failed to export render!", e);
        }

        return Command.SINGLE_SUCCESS;
      });
  }

  private static List<MapFrameData> collectMapItemFrames(ClientLevel level, Vec3 eyePos, int maxDistance) {
    var result = new ArrayList<MapFrameData>();

    StreamSupport.stream(level.entitiesForRendering().spliterator(), false)
      .flatMap(entity -> entity instanceof ItemFrame itemFrame ? Stream.of(itemFrame) : Stream.empty())
      .filter(frame -> frame.position().distanceTo(eyePos) <= maxDistance)
      .forEach(frame -> {
        var mapData = extractMapData(level, frame);
        if (mapData != null) {
          result.add(mapData);
        }
      });

    return result;
  }

  private static @Nullable MapFrameData extractMapData(ClientLevel level, ItemFrame frame) {
    var item = frame.getItem();
    var mapId = item.get(DataComponents.MAP_ID);
    if (mapId != null) {
      var mapData = level.getMapData(mapId);
      if (mapData != null) {
        return new MapFrameData(frame, mapData);
      }
    }

    return null;
  }

  private static List<EntityData> collectEntities(ClientLevel level, LocalPlayer localPlayer, Vec3 eyePos, int maxDistance) {
    var result = new ArrayList<EntityData>();

    StreamSupport.stream(level.entitiesForRendering().spliterator(), false)
      .filter(entity -> !(entity instanceof ItemFrame)) // Exclude item frames (handled separately)
      .filter(entity -> entity != localPlayer) // Exclude local player
      .filter(entity -> entity.position().distanceTo(eyePos) <= maxDistance)
      .forEach(entity -> result.add(new EntityData(entity, entity.getBoundingBox())));

    return result;
  }

  private static BufferedImage renderScene(
    ClientLevel level,
    Vec3 eyePos,
    float yRot,
    float xRot,
    int width,
    int height,
    double fov,
    int maxDistance,
    List<MapFrameData> mapFrames,
    List<EntityData> entities) {

    var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    var fovRad = Math.toRadians(fov);
    var aspectRatio = (double) width / height;

    // Pre-calculate base direction vectors
    // Minecraft: yRot is yaw (0 = south, 90 = west, 180 = north, 270 = east)
    // Minecraft: xRot is pitch (-90 = up, 0 = horizon, 90 = down)
    var yRotRad = Math.toRadians(yRot);
    var xRotRad = Math.toRadians(xRot);

    // Forward direction (where player is looking)
    // In Minecraft coordinate system: -Z is north, +X is east, +Y is up
    var forwardX = -Math.sin(yRotRad) * Math.cos(xRotRad);
    var forwardY = -Math.sin(xRotRad);
    var forwardZ = Math.cos(yRotRad) * Math.cos(xRotRad);

    // Right vector (perpendicular to forward, in horizontal plane)
    var rightX = Math.cos(yRotRad);
    var rightY = 0.0;
    var rightZ = Math.sin(yRotRad);

    // Up vector (cross product of forward and right, then negate for correct orientation)
    var upX = rightY * forwardZ - rightZ * forwardY;
    var upY = rightZ * forwardX - rightX * forwardZ;
    var upZ = rightX * forwardY - rightY * forwardX;

    // Normalize up vector
    var upLen = Math.sqrt(upX * upX + upY * upY + upZ * upZ);
    upX /= upLen;
    upY /= upLen;
    upZ /= upLen;

    var halfHeight = Math.tan(fovRad / 2);
    var halfWidth = halfHeight * aspectRatio;

    for (var y = 0; y < height; y++) {
      for (var x = 0; x < width; x++) {
        // Calculate normalized screen coordinates (-1 to 1)
        // screenX: flip so image x=0 (left) shows what's on the left in game
        // screenY: flip so image y=0 (top) shows what's above in game
        var screenX = (1.0 - 2.0 * x / width) * halfWidth;
        var screenY = (2.0 * y / height - 1.0) * halfHeight;

        // Calculate ray direction
        var rayDirX = forwardX + screenX * rightX + screenY * upX;
        var rayDirY = forwardY + screenX * rightY + screenY * upY;
        var rayDirZ = forwardZ + screenX * rightZ + screenY * upZ;

        // Normalize ray direction
        var rayLen = Math.sqrt(rayDirX * rayDirX + rayDirY * rayDirY + rayDirZ * rayDirZ);
        rayDirX /= rayLen;
        rayDirY /= rayLen;
        rayDirZ /= rayLen;

        var color = castRay(level, eyePos, rayDirX, rayDirY, rayDirZ, maxDistance, mapFrames, entities);
        image.setRGB(x, y, color);
      }
    }

    return image;
  }

  private static int castRay(
    ClientLevel level,
    Vec3 origin,
    double dirX,
    double dirY,
    double dirZ,
    int maxDistance,
    List<MapFrameData> mapFrames,
    List<EntityData> entities) {

    // Check for item frame map hits
    var mapHit = checkMapFrameHit(origin, dirX, dirY, dirZ, maxDistance, mapFrames);
    var mapHitDistance = mapHit != null ? mapHit.distance : Double.MAX_VALUE;

    // Check for entity hitbox hits
    var entityHit = checkEntityHit(origin, dirX, dirY, dirZ, maxDistance, entities);
    var entityHitDistance = entityHit != null ? entityHit.distance : Double.MAX_VALUE;

    // DDA (Digital Differential Analyzer) algorithm for voxel traversal
    var posX = origin.x;
    var posY = origin.y;
    var posZ = origin.z;

    var mapX = Mth.floor(posX);
    var mapY = Mth.floor(posY);
    var mapZ = Mth.floor(posZ);

    // Length of ray from one side to the next
    var deltaDistX = Math.abs(1 / dirX);
    var deltaDistY = Math.abs(1 / dirY);
    var deltaDistZ = Math.abs(1 / dirZ);

    // Direction to step in (+1 or -1)
    var stepX = dirX >= 0 ? 1 : -1;
    var stepY = dirY >= 0 ? 1 : -1;
    var stepZ = dirZ >= 0 ? 1 : -1;

    // Distance to next grid boundary
    var sideDistX = dirX >= 0
      ? (mapX + 1 - posX) * deltaDistX
      : (posX - mapX) * deltaDistX;
    var sideDistY = dirY >= 0
      ? (mapY + 1 - posY) * deltaDistY
      : (posY - mapY) * deltaDistY;
    var sideDistZ = dirZ >= 0
      ? (mapZ + 1 - posZ) * deltaDistZ
      : (posZ - mapZ) * deltaDistZ;

    var distance = 0.0;
    var side = 0; // 0 = X, 1 = Y, 2 = Z

    while (distance < maxDistance) {
      // Check if map hit is closer
      if (mapHit != null && distance > mapHitDistance) {
        return mapHit.color;
      }

      // Check if entity hit is closer
      if (entityHit != null && distance > entityHitDistance) {
        return entityHit.color;
      }

      // Jump to next grid cell
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

      // Check if outside world bounds
      if (mapY < level.getMinY() || mapY > level.getMaxY()) {
        // Check for closer entity/map hits first
        var closestHitDist = Math.min(mapHitDistance, entityHitDistance);
        if (closestHitDist < distance) {
          return mapHitDistance < entityHitDistance ? mapHit.color : entityHit.color;
        }
        return mapY < level.getMinY() ? VOID_COLOR : SKY_COLOR;
      }

      // Get block at current position
      var blockPos = new BlockPos(mapX, mapY, mapZ);
      var blockState = level.getBlockState(blockPos);

      // Skip air and transparent blocks
      if (!blockState.isAir() && blockState.getBlock() != Blocks.VOID_AIR) {
        // Check if block is solid/visible
        if (blockState.isSolidRender()) {
          // Check if map hit or entity hit is closer than this block
          var closestHitDist = Math.min(mapHitDistance, entityHitDistance);
          if (closestHitDist < distance) {
            return mapHitDistance < entityHitDistance ? mapHit.color : entityHit.color;
          }

          // Get the map color for this block
          var mapColor = blockState.getMapColor(level, blockPos);
          var baseColor = mapColor.col;

          // Apply simple shading based on which face was hit and distance
          var shadeFactor = switch (side) {
            case 0 -> 0.8; // X face (East/West)
            case 1 -> stepY > 0 ? 1.0 : 0.5; // Y face (Top brighter, bottom darker)
            case 2 -> 0.9; // Z face (North/South)
            default -> 1.0;
          };

          // Apply distance fog
          var fogFactor = Math.max(0, 1.0 - distance / maxDistance * 0.5);
          shadeFactor *= fogFactor;

          // Extract RGB components and apply shading
          var r = (int) (((baseColor >> 16) & 0xFF) * shadeFactor);
          var g = (int) (((baseColor >> 8) & 0xFF) * shadeFactor);
          var b = (int) ((baseColor & 0xFF) * shadeFactor);

          return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
      }
    }

    // Check for hits before max distance
    var closestHitDist = Math.min(mapHitDistance, entityHitDistance);
    if (closestHitDist < maxDistance) {
      return mapHitDistance < entityHitDistance ? mapHit.color : entityHit.color;
    }

    // No hit within max distance - return sky color
    return SKY_COLOR;
  }

  private static @Nullable EntityHitResult checkEntityHit(
    Vec3 origin,
    double dirX,
    double dirY,
    double dirZ,
    int maxDistance,
    List<EntityData> entities) {

    EntityHitResult closest = null;

    for (var entityData : entities) {
      var bbox = entityData.boundingBox;

      // Ray-AABB intersection
      var hit = rayIntersectsAABB(origin, dirX, dirY, dirZ, bbox, maxDistance);
      if (hit == null) {
        continue;
      }

      var distance = hit.distance;
      if (closest != null && distance >= closest.distance) {
        continue;
      }

      // Apply shading based on distance
      var fogFactor = Math.max(0.5, 1.0 - distance / maxDistance * 0.3);
      var r = (int) (((ENTITY_HITBOX_COLOR >> 16) & 0xFF) * fogFactor);
      var g = (int) (((ENTITY_HITBOX_COLOR >> 8) & 0xFF) * fogFactor);
      var b = (int) ((ENTITY_HITBOX_COLOR & 0xFF) * fogFactor);
      var color = 0xFF000000 | (r << 16) | (g << 8) | b;

      closest = new EntityHitResult(distance, color);
    }

    return closest;
  }

  private static @Nullable MapHitResult checkMapFrameHit(
    Vec3 origin,
    double dirX,
    double dirY,
    double dirZ,
    int maxDistance,
    List<MapFrameData> mapFrames) {

    MapHitResult closest = null;

    for (var mapFrame : mapFrames) {
      var frame = mapFrame.frame;
      var mapData = mapFrame.mapData;

      // Get the item frame's bounding box
      var bbox = frame.getBoundingBox();

      // Ray-AABB intersection
      var hit = rayIntersectsAABB(origin, dirX, dirY, dirZ, bbox, maxDistance);
      if (hit == null) {
        continue;
      }

      var distance = hit.distance;
      if (closest != null && distance >= closest.distance) {
        continue;
      }

      // Calculate UV coordinates on the map
      var hitPoint = origin.add(dirX * distance, dirY * distance, dirZ * distance);
      var direction = frame.getDirection();

      // Calculate local coordinates on the frame
      var framePos = frame.position();
      double u, v;

      // Map coordinates based on which direction the frame is facing
      if (direction == Direction.UP || direction == Direction.DOWN) {
        // Horizontal frame (on floor or ceiling)
        u = (hitPoint.x - (framePos.x - 0.5)) / 1.0;
        v = (hitPoint.z - (framePos.z - 0.5)) / 1.0;
        if (direction == Direction.DOWN) {
          v = 1.0 - v;
        }
      } else {
        // Vertical frame (on wall)
        var localX = hitPoint.x - framePos.x;
        var localY = hitPoint.y - framePos.y;
        var localZ = hitPoint.z - framePos.z;

        u = switch (direction) {
          case NORTH -> 0.5 - localX;
          case SOUTH -> 0.5 + localX;
          case WEST -> 0.5 + localZ;
          case EAST -> 0.5 - localZ;
          default -> 0.5;
        };
        v = 0.5 - localY;
      }

      // Apply item frame rotation
      var rotation = frame.getRotation();
      for (var i = 0; i < rotation; i++) {
        var oldU = u;
        u = v;
        v = 1.0 - oldU;
      }

      // Clamp and convert to pixel coordinates
      u = Math.clamp(u, 0.0, 0.999);
      v = Math.clamp(v, 0.0, 0.999);

      var pixelX = (int) (u * 128);
      var pixelY = (int) (v * 128);

      // Get color from map data
      var colorIndex = mapData.colors[pixelX + pixelY * 128];
      var color = MapColor.getColorFromPackedId(colorIndex);

      // Apply slight shading based on distance
      var fogFactor = Math.max(0.5, 1.0 - distance / maxDistance * 0.3);
      var r = (int) (((color >> 16) & 0xFF) * fogFactor);
      var g = (int) (((color >> 8) & 0xFF) * fogFactor);
      var b = (int) ((color & 0xFF) * fogFactor);
      color = 0xFF000000 | (r << 16) | (g << 8) | b;

      closest = new MapHitResult(distance, color);
    }

    return closest;
  }

  private static @Nullable RayHit rayIntersectsAABB(Vec3 origin, double dirX, double dirY, double dirZ, AABB box, double maxDist) {
    var invDirX = 1.0 / dirX;
    var invDirY = 1.0 / dirY;
    var invDirZ = 1.0 / dirZ;

    var t1 = (box.minX - origin.x) * invDirX;
    var t2 = (box.maxX - origin.x) * invDirX;
    var t3 = (box.minY - origin.y) * invDirY;
    var t4 = (box.maxY - origin.y) * invDirY;
    var t5 = (box.minZ - origin.z) * invDirZ;
    var t6 = (box.maxZ - origin.z) * invDirZ;

    var tMin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
    var tMax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

    // If tMax < 0, the box is behind us
    if (tMax < 0) {
      return null;
    }

    // If tMin > tMax, no intersection
    if (tMin > tMax) {
      return null;
    }

    // Use tMin if we're outside the box, tMax if we're inside
    var t = tMin >= 0 ? tMin : tMax;

    if (t > maxDist) {
      return null;
    }

    return new RayHit(t);
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
      literal("export-basic-render")
        .executes(
          help(
            "Exports an image of a rudimentary camera render of the world the bot sees.",
            c -> exportBasicRender(c, DEFAULT_WIDTH, DEFAULT_HEIGHT)))
        .then(
          argument("width", IntegerArgumentType.integer(1, 3840))
            .then(
              argument("height", IntegerArgumentType.integer(1, 2160))
                .executes(
                  help(
                    "Exports an image with custom resolution.",
                    c -> exportBasicRender(
                      c,
                      IntegerArgumentType.getInteger(c, "width"),
                      IntegerArgumentType.getInteger(c, "height")))))));
  }

  private record MapFrameData(ItemFrame frame, MapItemSavedData mapData) {}

  private record EntityData(Entity entity, AABB boundingBox) {}

  private record MapHitResult(double distance, int color) {}

  private record EntityHitResult(double distance, int color) {}

  private record RayHit(double distance) {}
}
