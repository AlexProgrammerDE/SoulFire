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

import net.minecraft.world.phys.Vec3;

/// Represents a camera in 3D space with position, rotation, and projection settings.
/// Pre-computes direction vectors for efficient ray generation.
public final class Camera {
  // Position
  private final double eyeX;
  private final double eyeY;
  private final double eyeZ;

  // Pre-computed direction vectors
  private final double forwardX;
  private final double forwardY;
  private final double forwardZ;
  private final double rightX;
  private final double rightZ;
  private final double upX;
  private final double upY;
  private final double upZ;

  // Screen projection parameters
  private final double screenXMult;
  private final double screenYMult;
  private final double screenXOffset;
  private final double screenYOffset;

  /// Creates a camera from position, rotation, and viewport settings.
  ///
  /// @param eyePos The eye position in world coordinates
  /// @param yRot   Yaw rotation in degrees (0 = south, 90 = west)
  /// @param xRot   Pitch rotation in degrees (-90 = up, 90 = down)
  /// @param width  Viewport width in pixels
  /// @param height Viewport height in pixels
  /// @param fov    Field of view in degrees
  public Camera(Vec3 eyePos, float yRot, float xRot, int width, int height, double fov) {
    this.eyeX = eyePos.x;
    this.eyeY = eyePos.y;
    this.eyeZ = eyePos.z;

    // Pre-compute trigonometric values
    var yRotRad = Math.toRadians(yRot);
    var xRotRad = Math.toRadians(xRot);
    var cosYRot = Math.cos(yRotRad);
    var sinYRot = Math.sin(yRotRad);
    var cosXRot = Math.cos(xRotRad);
    var sinXRot = Math.sin(xRotRad);

    // Forward direction (where camera is looking)
    this.forwardX = -sinYRot * cosXRot;
    this.forwardY = -sinXRot;
    this.forwardZ = cosYRot * cosXRot;

    // Right vector (perpendicular to forward, in horizontal plane)
    this.rightX = cosYRot;
    this.rightZ = sinYRot;

    // Up vector
    this.upX = sinYRot * sinXRot;
    this.upY = cosXRot;
    this.upZ = -cosYRot * sinXRot;

    // Screen projection parameters
    var fovRad = Math.toRadians(fov);
    var aspectRatio = (double) width / height;
    var halfHeight = Math.tan(fovRad / 2);
    var halfWidth = halfHeight * aspectRatio;

    this.screenXMult = 2.0 * halfWidth / width;
    this.screenYMult = 2.0 * halfHeight / height;
    this.screenXOffset = halfWidth;
    this.screenYOffset = halfHeight;
  }

  public double eyeX() {
    return eyeX;
  }

  public double eyeY() {
    return eyeY;
  }

  public double eyeZ() {
    return eyeZ;
  }

  public double forwardX() {
    return forwardX;
  }

  public double forwardY() {
    return forwardY;
  }

  public double forwardZ() {
    return forwardZ;
  }

  public double rightX() {
    return rightX;
  }

  public double rightZ() {
    return rightZ;
  }

  public double upX() {
    return upX;
  }

  public double upY() {
    return upY;
  }

  public double upZ() {
    return upZ;
  }

  public double screenXMult() {
    return screenXMult;
  }

  public double screenYMult() {
    return screenYMult;
  }

  public double screenXOffset() {
    return screenXOffset;
  }

  public double screenYOffset() {
    return screenYOffset;
  }
}
