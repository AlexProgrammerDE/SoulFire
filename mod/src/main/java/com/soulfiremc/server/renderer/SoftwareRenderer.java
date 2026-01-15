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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/// Software 3D renderer using ray casting.
/// Renders the Minecraft world from a player's perspective to a BufferedImage.
@UtilityClass
public class SoftwareRenderer {

  /// Renders the scene from the player's perspective.
  ///
  /// @param level       The client level to render
  /// @param player      The player whose view to render from
  /// @param width       Image width in pixels
  /// @param height      Image height in pixels
  /// @param fov         Field of view in degrees
  /// @param maxDistance Maximum render distance in blocks
  /// @return The rendered image
  public static BufferedImage render(
    ClientLevel level,
    LocalPlayer player,
    int width,
    int height,
    double fov,
    int maxDistance) {

    return render(
      level,
      player,
      player.getEyePosition(),
      player.getYRot(),
      player.getXRot(),
      width,
      height,
      fov,
      maxDistance
    );
  }

  /// Renders the scene from a custom camera position.
  ///
  /// @param level       The client level to render
  /// @param localPlayer The local player (excluded from rendering)
  /// @param eyePos      Camera eye position
  /// @param yRot        Camera yaw rotation in degrees
  /// @param xRot        Camera pitch rotation in degrees
  /// @param width       Image width in pixels
  /// @param height      Image height in pixels
  /// @param fov         Field of view in degrees
  /// @param maxDistance Maximum render distance in blocks
  /// @return The rendered image
  public static BufferedImage render(
    ClientLevel level,
    LocalPlayer localPlayer,
    Vec3 eyePos,
    float yRot,
    float xRot,
    int width,
    int height,
    double fov,
    int maxDistance) {

    // Create camera with pre-computed direction vectors
    var camera = new Camera(eyePos, yRot, xRot, width, height, fov);

    // Collect scene data
    var sceneData = SceneCollector.collect(
      level,
      localPlayer,
      eyePos.x,
      eyePos.y,
      eyePos.z,
      maxDistance
    );

    // Create render context
    var ctx = RenderContext.create(level, camera, sceneData, maxDistance);

    // Create output image with direct pixel access
    var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    var pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

    // Use parallel streams with ForkJoinPool for multithreading
    var parallelism = Runtime.getRuntime().availableProcessors();
    var pool = new ForkJoinPool(parallelism);

    try {
      pool.submit(() ->
        IntStream.range(0, height).parallel().forEach(y ->
          renderRow(ctx, camera, pixels, width, y)
        )
      ).get();
    } catch (Exception e) {
      throw new RuntimeException("Render failed", e);
    } finally {
      pool.shutdown();
    }

    return image;
  }

  private static void renderRow(RenderContext ctx, Camera camera, int[] pixels, int width, int y) {
    var screenY = camera.screenYOffset() - y * camera.screenYMult();
    var rowOffset = y * width;

    for (var x = 0; x < width; x++) {
      var screenX = camera.screenXOffset() - x * camera.screenXMult();

      // Calculate ray direction
      var rayDirX = camera.forwardX() + screenX * camera.rightX() + screenY * camera.upX();
      var rayDirY = camera.forwardY() + screenY * camera.upY();
      var rayDirZ = camera.forwardZ() + screenX * camera.rightZ() + screenY * camera.upZ();

      // Fast inverse square root normalization
      var lenSq = rayDirX * rayDirX + rayDirY * rayDirY + rayDirZ * rayDirZ;
      var invLen = RayCaster.fastInvSqrt(lenSq);
      rayDirX *= invLen;
      rayDirY *= invLen;
      rayDirZ *= invLen;

      pixels[rowOffset + x] = RayCaster.castRay(ctx, rayDirX, rayDirY, rayDirZ);
    }
  }
}
