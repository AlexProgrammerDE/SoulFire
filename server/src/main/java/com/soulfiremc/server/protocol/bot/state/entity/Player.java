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
import com.google.common.collect.Lists;
import com.soulfiremc.server.data.*;
import com.soulfiremc.server.protocol.bot.container.PlayerInventoryContainer;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.model.AbilitiesData;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.mcstructs.AABB;
import com.soulfiremc.server.util.mcstructs.MoverType;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
public abstract class Player extends LivingEntity {
  private final PlayerInventoryContainer inventory = new PlayerInventoryContainer();
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
  private boolean reducedDebugInfo;
  protected int clientLoadedTimeoutTimer = 60;
  private boolean clientLoaded = false;

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
  public void handleEntityEvent(EntityEvent event) {
    switch (event) {
      case PLAYER_ENABLE_REDUCED_DEBUG -> this.reducedDebugInfo = true;
      case PLAYER_DISABLE_REDUCED_DEBUG -> this.reducedDebugInfo = false;
      default -> super.handleEntityEvent(event);
    }
  }

  @Override
  public void aiStep() {
    if (this.abilitiesData().flying) {
      this.resetFallDistance();
    }

    super.aiStep();
    this.setSpeed((float) this.attributeValue(AttributeType.MOVEMENT_SPEED));

    if (this.getHealth() > 0.0F && !this.isSpectator()) {
      var bb = this.getBoundingBox().inflate(1.0, 0.5, 1.0);
      var list = this.level().getEntities(bb);
      List<Entity> orbList = Lists.newArrayList();

      for (var entity : list) {
        if (entity.entityType() == EntityType.EXPERIENCE_ORB) {
          orbList.add(entity);
        } else if (!entity.isRemoved()) {
          this.touch(entity);
        }
      }

      if (!orbList.isEmpty()) {
        this.touch(SFHelpers.getRandomEntry(orbList));
      }
    }
  }

  private void touch(Entity entity) {
    entity.playerTouch(this);
  }

  @Override
  public void travel(Vector3d travelVector) {
    if (this.isSwimming()) {
      var d = this.getLookAngle().getY();
      var e = d < -0.2 ? 0.085 : 0.06;
      if (d <= 0.0 || this.jumping || !this.level().getBlockState(Vector3i.from(this.x(), this.y() + 1.0 - 0.1, this.z())).fluidState().empty()) {
        var lv = this.getDeltaMovement();
        this.setDeltaMovement(lv.add(0.0, (d - lv.getY()) * e, 0.0));
      }
    }

    if (this.abilitiesData().flying) {
      var d = this.getDeltaMovement().getY();
      super.travel(travelVector);
      var deltaMovement = this.getDeltaMovement();
      this.setDeltaMovement(Vector3d.from(deltaMovement.getX(), d * 0.6, deltaMovement.getZ()));
    } else {
      super.travel(travelVector);
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

  @Override
  public Optional<SFItemStack> getItemBySlot(EquipmentSlot slot) {
    return inventory.getEquipmentSlotItem(slot);
  }

  @Override
  public void setItemSlot(EquipmentSlot slot, @Nullable SFItemStack item) {
    inventory.setEquipmentSlotItem(slot, item);
  }

  public void startFallFlying() {
    this.setSharedFlag(FLAG_FALL_FLYING, true);
  }

  public void stopFallFlying() {
    this.setSharedFlag(FLAG_FALL_FLYING, true);
    this.setSharedFlag(FLAG_FALL_FLYING, false);
  }

  @Override
  public boolean canSprint() {
    return true;
  }

  @Override
  protected float getFlyingSpeed() {
    if (this.abilitiesData.flying) {
      return this.isSprinting() ? this.abilitiesData.flySpeed() * 2.0F : this.abilitiesData.flySpeed();
    } else {
      return this.isSprinting() ? 0.025999999F : 0.02F;
    }
  }

  @Override
  protected Vector3d maybeBackOffFromEdge(Vector3d vec, MoverType mover) {
    var maxUpStep = this.maxUpStep();
    if (!this.abilitiesData.flying
      && !(vec.getY() > 0.0)
      && (mover == MoverType.SELF || mover == MoverType.PLAYER)
      && this.isStayingOnGroundSurface()
      && this.isAboveGround(maxUpStep)) {
      var d = vec.getX();
      var e = vec.getZ();
      var min = 0.05;
      var h = Math.signum(d) * min;

      double i;
      for (i = Math.signum(e) * min; d != 0.0 && this.canFallAtLeast(d, 0.0, maxUpStep); d -= h) {
        if (Math.abs(d) <= min) {
          d = 0.0;
          break;
        }
      }

      while (e != 0.0 && this.canFallAtLeast(0.0, e, maxUpStep)) {
        if (Math.abs(e) <= min) {
          e = 0.0;
          break;
        }

        e -= i;
      }

      while (d != 0.0 && e != 0.0 && this.canFallAtLeast(d, e, maxUpStep)) {
        if (Math.abs(d) <= min) {
          d = 0.0;
        } else {
          d -= h;
        }

        if (Math.abs(e) <= min) {
          e = 0.0;
        } else {
          e -= i;
        }
      }

      return Vector3d.from(d, vec.getY(), e);
    } else {
      return vec;
    }
  }

  private boolean isAboveGround(float maxUpStep) {
    return this.onGround() || this.fallDistance < maxUpStep && !this.canFallAtLeast(0.0, 0.0, maxUpStep - this.fallDistance);
  }

  private boolean canFallAtLeast(double x, double z, float distance) {
    var lv = this.getBoundingBox();
    return this.level().noCollision(new AABB(lv.minX + x, lv.minY - (double) distance - 1.0E-5F, lv.minZ + z, lv.maxX + x, lv.minY, lv.maxZ + z));
  }

  public boolean isSecondaryUseActive() {
    return this.isShiftKeyDown();
  }

  protected boolean wantsToStopRiding() {
    return this.isShiftKeyDown();
  }

  protected boolean isStayingOnGroundSurface() {
    return this.isShiftKeyDown();
  }

  @Override
  public float getSpeed() {
    return (float) this.attributeValue(AttributeType.MOVEMENT_SPEED);
  }

  @Override
  public void makeStuckInBlock(Vector3d motionMultiplier) {
    if (!this.abilitiesData.flying) {
      super.makeStuckInBlock(motionMultiplier);
    }
  }

  public boolean canUseGameMasterBlocks() {
    return this.abilitiesData().instabuild && this.permissionLevel() >= 2;
  }

  public int permissionLevel() {
    return 0;
  }

  public boolean hasPermissions(int i) {
    return this.permissionLevel() >= i;
  }

  public boolean hasClientLoaded() {
    return this.clientLoaded || this.clientLoadedTimeoutTimer <= 0;
  }

  public void tickClientLoadTimeout() {
    if (!this.clientLoaded) {
      this.clientLoadedTimeoutTimer--;
    }
  }

  public void setClientLoaded(boolean clientLoaded) {
    this.clientLoaded = clientLoaded;
    if (!this.clientLoaded) {
      this.clientLoadedTimeoutTimer = 60;
    }
  }
}
