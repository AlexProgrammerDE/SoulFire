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
package com.soulfiremc.server.pathfinding.world;

/// Represents the state of a block in the world for pathfinding purposes.
/// This is an abstraction that allows the pathfinding library to work
/// without depending on Minecraft-specific code.
public interface BlockState {
  /// Returns a unique identifier for this block state within a global registry.
  int globalId();

  /// Returns the block type of this state.
  BlockType blockType();

  /// Returns true if this block state has an empty collision shape.
  boolean isCollisionShapeEmpty();

  /// Returns true if this block state has a full block collision shape.
  boolean isCollisionShapeFullBlock();

  /// Returns true if the top face of this block is full (suitable for standing).
  boolean isTopFaceFull();

  /// Returns true if this block is empty (air-like but not void air).
  boolean isEmpty();

  /// Returns true if this block is void air (unloaded chunk marker).
  boolean isVoidAir();

  /// Returns true if this block is a fluid.
  boolean isFluid();

  /// Returns true if this block is a falling block (sand, gravel, etc.).
  boolean isFallingBlock();

  /// Returns true if touching this block hurts the player.
  boolean isHurtOnTouch();

  /// Returns true if standing on this block hurts the player.
  boolean isHurtOnStand();

  /// Returns true if this block affects movement speed when touched.
  boolean affectsMovementSpeed();

  /// Returns true if this block blocks motion (solid).
  boolean blocksMotion();

  /// Returns true if this block can be replaced (like grass, water, etc.).
  boolean canBeReplaced();

  /// Returns true if this block is in the stairs tag.
  boolean isStairs();
}
