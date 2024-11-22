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

import com.google.common.collect.ImmutableMap;
import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.data.EntityDimensions;
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.data.FluidTags;
import com.soulfiremc.server.protocol.bot.model.AbilitiesData;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.VectorHelper;
import com.soulfiremc.server.util.mcstructs.AABB;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;

import java.util.Map;

@Getter
@Setter
public abstract class Player extends LivingEntity {
  private final AbilitiesData abilitiesData = new AbilitiesData();
  public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.8F)
    .withEyeHeight(1.62F);
  private static final Map<Pose, EntityDimensions> POSES = ImmutableMap.<Pose, EntityDimensions>builder()
    .put(Pose.STANDING, STANDING_DIMENSIONS)
    .put(Pose.SLEEPING, SLEEPING_DIMENSIONS)
    .put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(Pose.SNEAKING, EntityDimensions.scalable(0.6F, 1.5F).withEyeHeight(1.27F))
    .put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(1.62F))
    .build();
  protected final GameProfile gameProfile;
  protected boolean wasUnderwater = false;

  public Player(Level level, GameProfile gameProfile) {
    super(EntityType.PLAYER, level);
    this.gameProfile = gameProfile;
    uuid(gameProfile.getId());
  }

  @Override
  public void tick() {
    this.noPhysics = this.isSpectator();
    if (this.isSpectator()) {
      this.setOnGround(false);
    }

    this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
    super.tick();

    var x = MathHelper.clamp(this.x(), -2.9999999E7, 2.9999999E7);
    var z = MathHelper.clamp(this.z(), -2.9999999E7, 2.9999999E7);
    if (x != this.x() || z != this.z()) {
      this.setPos(x, this.y(), z);
    }

    this.updatePlayerPose();
  }

  @Override
  public void aiStep() {
    if (this.jumpTriggerTime > 0) {
      this.jumpTriggerTime--;
    }

    if (this.abilitiesData().flying) {
      this.resetFallDistance();
    }

    super.aiStep();
    this.setSpeed((float) this.attributeValue(AttributeType.MOVEMENT_SPEED));
    float f;
    if (this.onGround() && !this.isDeadOrDying() && !this.isSwimming()) {
      f = Math.min(0.1F, (float) VectorHelper.horizontalDistance(this.getDeltaMovement()));
    } else {
      f = 0.0F;
    }
  }

  protected void updatePlayerPose() {
    if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {
      Pose mainPose;
      if (this.isFallFlying()) {
        mainPose = Pose.FALL_FLYING;
      } else if (this.isSleeping()) {
        mainPose = Pose.SLEEPING;
      } else if (this.isSwimming()) {
        mainPose = Pose.SWIMMING;
      } else if (this.isAutoSpinAttack()) {
        mainPose = Pose.SPIN_ATTACK;
      } else if (this.isShiftKeyDown() && !this.abilitiesData.flying) {
        mainPose = Pose.SNEAKING;
      } else {
        mainPose = Pose.STANDING;
      }

      Pose fallbackPose;
      if (this.isSpectator() || this.canPlayerFitWithinBlocksAndEntitiesWhen(mainPose)) {
        fallbackPose = mainPose;
      } else if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SNEAKING)) {
        fallbackPose = Pose.SNEAKING;
      } else {
        fallbackPose = Pose.SWIMMING;
      }

      this.setPose(fallbackPose);
    }
  }

  protected boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose) {
    return this.level().noCollision(this.getDimensions(pose).makeBoundingBox(this.pos()).deflate(AABB.EPSILON));
  }

  @Override
  public EntityDimensions getDefaultDimensions(Pose pose) {
    return POSES.getOrDefault(pose, STANDING_DIMENSIONS);
  }

  @Override
  public boolean isSwimming() {
    return !this.abilitiesData.flying && !this.isSpectator() && super.isSwimming();
  }

  public abstract boolean isCreative();

  @Override
  public boolean isPushedByFluid() {
    return !this.abilitiesData.flying;
  }

  @Override
  protected float getBlockSpeedFactor() {
    return !this.abilitiesData.flying && !this.isFallFlying() ? super.getBlockSpeedFactor() : 1.0F;
  }

  public boolean isLocalPlayer() {
    return false;
  }

  public void onUpdateAbilities() {
  }

  public boolean tryToStartFallFlying() {
    if (!this.isFallFlying() && this.canGlide() && !this.isInWater()) {
      this.startFallFlying();
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected boolean canGlide() {
    return !this.abilitiesData().flying && super.canGlide();
  }

  public void startFallFlying() {
    this.setSharedFlag(FLAG_FALL_FLYING, true);
  }

  public void stopFallFlying() {
    this.setSharedFlag(FLAG_FALL_FLYING, true);
    this.setSharedFlag(FLAG_FALL_FLYING, false);
  }
}
