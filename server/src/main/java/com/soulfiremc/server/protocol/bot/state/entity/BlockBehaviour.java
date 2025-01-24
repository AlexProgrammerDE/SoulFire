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
package com.soulfiremc.server.protocol.bot.state.entity;

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.data.EffectType;
import com.soulfiremc.server.data.block.BlockProperties;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

@SuppressWarnings("SameParameterValue")
public class BlockBehaviour {
  public static void updateEntityMovementAfterFallOn(BlockType blockType, Entity entity) {
    if (blockType.bedBlock()) {
      updateEntityMovementAfterFallOnBed(entity);
    } else if (blockType == BlockType.SLIME_BLOCK) {
      updateEntityMovementAfterFallOnSlime(entity);
    } else {
      updateEntityMovementAfterFallOnDefault(entity);
    }
  }

  public static void stepOn(BlockType blockType, Entity entity) {
    if (blockType == BlockType.SLIME_BLOCK) {
      stepOnSlime(entity);
    }
  }

  public static void stepOnSlime(Entity entity) {
    var d = Math.abs(entity.getDeltaMovement().getY());
    if (d < 0.1 && !entity.isSteppingCarefully()) {
      var e = 0.4 + d * 0.2;
      entity.setDeltaMovement(entity.getDeltaMovement().mul(e, 1.0, e));
    }
  }

  public static void onInsideBlock(Vector3i pos, BlockState state, Entity entity) {
    if (state.blockType() == BlockType.BUBBLE_COLUMN) {
      onInsideBubbleColumn(pos, state, entity);
    } else if (state.blockType() == BlockType.HONEY_BLOCK) {
      onInsideHoneyBlock(pos, entity);
    } else if (state.blockType() == BlockType.COBWEB) {
      onInsideWebBlock(entity);
    } else if (state.blockType() == BlockType.POWDER_SNOW) {
      onInsidePowderSnow(entity);
    }
  }

  public static void onInsideBubbleColumn(Vector3i pos, BlockState state, Entity entity) {
    var upBlockState = entity.level().getBlockState(pos.up());
    if (upBlockState.blockType().air()) {
      entity.onAboveBubbleCol(state.properties().get(BlockProperties.DRAG));
    } else {
      entity.onInsideBubbleColumn(state.properties().get(BlockProperties.DRAG));
    }
  }

  public static void onInsideHoneyBlock(Vector3i pos, Entity entity) {
    if (isSlidingDownHoneyBlock(pos, entity)) {
      doSlideMovementHoneyBlock(entity);
    }
  }

  public static void onInsideWebBlock(Entity entity) {
    var multiplier = Vector3d.from(0.25, 0.05F, 0.25);
    if (entity instanceof LivingEntity le && le.effectState().hasEffect(EffectType.WEAVING)) {
      multiplier = Vector3d.from(0.5, 0.25, 0.5);
    }

    entity.makeStuckInBlock(multiplier);
  }

  public static void onInsidePowderSnow(Entity entity) {
    if (!(entity instanceof LivingEntity) || entity.getInBlockState().blockType() == BlockType.POWDER_SNOW) {
      entity.makeStuckInBlock(Vector3d.from(0.9F, 1.5, 0.9F));
    }

    entity.isInPowderSnow(true);
  }

  private static double getOldDeltaYHoneyBlock(double d) {
    return d / 0.98F + 0.08;
  }

  private static double getNewDeltaYHoneyBlock(double d) {
    return (d - 0.08) * 0.98F;
  }

  private static boolean isSlidingDownHoneyBlock(Vector3i pos, Entity entity) {
    if (entity.onGround()) {
      return false;
    } else if (entity.y() > (double) pos.getY() + 0.9375 - 1.0E-7) {
      return false;
    } else if (getOldDeltaYHoneyBlock(entity.getDeltaMovement().getY()) >= -0.08) {
      return false;
    } else {
      var x = Math.abs((double) pos.getX() + 0.5 - entity.x());
      var z = Math.abs((double) pos.getZ() + 0.5 - entity.z());
      var bb = 0.4375 + (double) (entity.getBbWidth() / 2.0F);
      return x + 1.0E-7 > bb || z + 1.0E-7 > bb;
    }
  }

  private static void doSlideMovementHoneyBlock(Entity entity) {
    var deltaMovement = entity.getDeltaMovement();
    if (getOldDeltaYHoneyBlock(entity.getDeltaMovement().getY()) < -0.13) {
      var multiplier = -0.05 / getOldDeltaYHoneyBlock(entity.getDeltaMovement().getY());
      entity.setDeltaMovement(Vector3d.from(deltaMovement.getX() * multiplier, getNewDeltaYHoneyBlock(-0.05), deltaMovement.getZ() * multiplier));
    } else {
      entity.setDeltaMovement(Vector3d.from(deltaMovement.getX(), getNewDeltaYHoneyBlock(-0.05), deltaMovement.getZ()));
    }

    entity.resetFallDistance();
  }

  private static void updateEntityMovementAfterFallOnDefault(Entity entity) {
    entity.setDeltaMovement(entity.getDeltaMovement().mul(1.0, 0.0, 1.0));
  }

  private static void updateEntityMovementAfterFallOnBed(Entity entity) {
    if (entity.isSuppressingBounce()) {
      updateEntityMovementAfterFallOnDefault(entity);
    } else {
      bounceUpBed(entity);
    }
  }

  private static void updateEntityMovementAfterFallOnSlime(Entity entity) {
    if (entity.isSuppressingBounce()) {
      updateEntityMovementAfterFallOnDefault(entity);
    } else {
      bounceUpSlimeBlock(entity);
    }
  }

  private static void bounceUpBed(Entity entity) {
    var deltaMovement = entity.getDeltaMovement();
    if (deltaMovement.getY() < 0.0) {
      var bouncyMultiplier = entity instanceof LivingEntity ? 1.0 : 0.8;
      entity.setDeltaMovement(deltaMovement.getX(), -deltaMovement.getY() * 0.66F * bouncyMultiplier, deltaMovement.getZ());
    }
  }

  private static void bounceUpSlimeBlock(Entity entity) {
    var deltaMovement = entity.getDeltaMovement();
    if (deltaMovement.getY() < 0.0) {
      var bouncyMultiplier = entity instanceof LivingEntity ? 1.0 : 0.8;
      entity.setDeltaMovement(deltaMovement.getX(), -deltaMovement.getY() * bouncyMultiplier, deltaMovement.getZ());
    }
  }
}
