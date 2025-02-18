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
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.container.PlayerInventoryContainer;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.mcstructs.AABB;
import com.soulfiremc.server.util.mcstructs.MoverType;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class Player extends LivingEntity {
  public static final float CROUCH_BB_HEIGHT = 1.5F;
  public static final float SWIMMING_BB_WIDTH = 0.6F;
  public static final float SWIMMING_BB_HEIGHT = 0.6F;
  public static final float DEFAULT_EYE_HEIGHT = 1.62F;
  public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.8F)
    .withEyeHeight(DEFAULT_EYE_HEIGHT);
  private static final Map<Pose, EntityDimensions> POSES = ImmutableMap.<Pose, EntityDimensions>builder()
    .put(Pose.STANDING, STANDING_DIMENSIONS)
    .put(Pose.SLEEPING, SLEEPING_DIMENSIONS)
    .put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(Pose.SWIMMING, EntityDimensions.scalable(SWIMMING_BB_WIDTH, SWIMMING_BB_HEIGHT).withEyeHeight(0.4F))
    .put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(Pose.SNEAKING, EntityDimensions.scalable(0.6F, CROUCH_BB_HEIGHT).withEyeHeight(1.27F))
    .put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(DEFAULT_EYE_HEIGHT))
    .build();
  public static final int CLIENT_LOADED_TIMEOUT_TIME = 60;
  protected final GameProfile gameProfile;
  protected final float defaultFlySpeed = 0.02F;
  @Getter
  private final PlayerInventoryContainer inventory = new PlayerInventoryContainer();
  @Getter
  private final AbilitiesState abilitiesState = new AbilitiesState();
  @Getter
  private final Object2IntMap<Key> itemCoolDowns = Object2IntMaps.synchronize(new Object2IntOpenHashMap<>());
  public int experienceLevel;
  public int totalExperience;
  public float experienceProgress;
  protected FoodData foodData = new FoodData();
  protected boolean wasUnderwater = false;
  protected int clientLoadedTimeoutTimer = CLIENT_LOADED_TIMEOUT_TIME;
  protected int autoSpinAttackTicks;
  protected float autoSpinAttackDmg;
  @Nullable
  protected ItemStack autoSpinAttackItemStack;
  private boolean reducedDebugInfo;
  private boolean clientLoaded = false;
  private Optional<GlobalPos> lastDeathLocation = Optional.empty();

  public Player(Level level, Vector3i spawnBlockPos, float spawnYRot, GameProfile gameProfile) {
    super(EntityType.PLAYER, level);
    uuid(gameProfile.getId());
    this.gameProfile = gameProfile;
    this.moveTo((double) spawnBlockPos.getX() + 0.5, spawnBlockPos.getY() + 1, (double) spawnBlockPos.getZ() + 0.5, spawnYRot, 0.0F);
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

    this.attackStrengthTicker++;

    tickCooldowns();
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
    if (this.abilitiesState().flying) {
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

  public boolean isReducedDebugInfo() {
    return this.reducedDebugInfo;
  }

  public void setReducedDebugInfo(boolean reducedDebugInfo) {
    this.reducedDebugInfo = reducedDebugInfo;
  }

  private void touch(Entity entity) {
    entity.playerTouch(this);
  }

  @Override
  public void travel(Vector3d travelVector) {
    if (this.isSwimming()) {
      var lookAngleY = this.getLookAngle().getY();
      var fixedAngleMultiplier = lookAngleY < -0.2 ? 0.085 : 0.06;
      if (lookAngleY <= 0.0 || this.jumping || !this.level().getBlockState(Vector3i.from(this.x(), this.y() + 1.0 - 0.1, this.z())).fluidState().empty()) {
        var deltaMovement = this.getDeltaMovement();
        this.setDeltaMovement(deltaMovement.add(0.0, (lookAngleY - deltaMovement.getY()) * fixedAngleMultiplier, 0.0));
      }
    }

    if (this.abilitiesState().flying) {
      var yDelta = this.getDeltaMovement().getY();
      super.travel(travelVector);
      var deltaMovement = this.getDeltaMovement();
      this.setDeltaMovement(Vector3d.from(deltaMovement.getX(), yDelta * 0.6, deltaMovement.getZ()));
    } else {
      super.travel(travelVector);
    }
  }

  public float getCurrentItemAttackStrengthDelay() {
    return (float) (1.0 / this.attributeValue(AttributeType.ATTACK_SPEED) * 20.0);
  }

  public float getAttackStrengthScale(float adjustTicks) {
    // https://github.com/ViaVersion/ViaFabricPlus/blob/44c391a92414d85fb725ad61af69d844f5bf266c/src/main/java/com/viaversion/viafabricplus/injection/mixin/features/interaction/attack_cooldown/MixinPlayerEntity.java
    if (BotConnection.CURRENT.get().protocolVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
      return 1F;
    }

    return MathHelper.clamp(((float) this.attackStrengthTicker + adjustTicks) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
  }

  public void resetAttackStrengthTicker() {
    this.attackStrengthTicker = 0;
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
      } else if (this.isShiftKeyDown() && !this.abilitiesState.flying) {
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
    return !this.abilitiesState.flying && !this.isSpectator() && super.isSwimming();
  }

  public abstract boolean isCreative();

  @Override
  public boolean isPushedByFluid() {
    return !this.abilitiesState.flying;
  }

  @Override
  protected float getBlockSpeedFactor() {
    return !this.abilitiesState.flying && !this.isFallFlying() ? super.getBlockSpeedFactor() : 1.0F;
  }

  public boolean isLocalPlayer() {
    return false;
  }

  public int getScore() {
    return this.metadataState.getMetadata(NamedEntityData.PLAYER__SCORE, MetadataTypes.INT);
  }

  @Override
  public boolean canBeSeenAsEnemy() {
    return !this.abilitiesState().invulnerable && super.canBeSeenAsEnemy();
  }

  @Override
  public boolean onClimbable() {
    return !this.abilitiesState().flying && super.onClimbable();
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
    return !this.abilitiesState().flying && super.canGlide();
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
    if (this.abilitiesState.flying) {
      return this.isSprinting() ? this.abilitiesState.flySpeed() * 2.0F : this.abilitiesState.flySpeed();
    } else {
      return this.isSprinting() ? 0.025999999F : defaultFlySpeed;
    }
  }

  @Override
  protected Vector3d maybeBackOffFromEdge(Vector3d vec, MoverType mover) {
    var maxUpStep = this.maxUpStep();
    if (!this.abilitiesState.flying
      && !(vec.getY() > 0.0)
      && (mover == MoverType.SELF || mover == MoverType.PLAYER)
      && this.isStayingOnGroundSurface()
      && this.isAboveGround(maxUpStep)) {
      var currentX = vec.getX();
      var currentZ = vec.getZ();
      var min = 0.05;
      var h = Math.signum(currentX) * min;

      double zBackoff;
      for (zBackoff = Math.signum(currentZ) * min; currentX != 0.0 && this.canFallAtLeast(currentX, 0.0, maxUpStep); currentX -= h) {
        if (Math.abs(currentX) <= min) {
          currentX = 0.0;
          break;
        }
      }

      while (currentZ != 0.0 && this.canFallAtLeast(0.0, currentZ, maxUpStep)) {
        if (Math.abs(currentZ) <= min) {
          currentZ = 0.0;
          break;
        }

        currentZ -= zBackoff;
      }

      while (currentX != 0.0 && currentZ != 0.0 && this.canFallAtLeast(currentX, currentZ, maxUpStep)) {
        if (Math.abs(currentX) <= min) {
          currentX = 0.0;
        } else {
          currentX -= h;
        }

        if (Math.abs(currentZ) <= min) {
          currentZ = 0.0;
        } else {
          currentZ -= zBackoff;
        }
      }

      return Vector3d.from(currentX, vec.getY(), currentZ);
    } else {
      return vec;
    }
  }

  private boolean isAboveGround(float maxUpStep) {
    return this.onGround() || this.fallDistance < maxUpStep && !this.canFallAtLeast(0.0, 0.0, maxUpStep - this.fallDistance);
  }

  private boolean canFallAtLeast(double x, double z, float distance) {
    var bb = this.getBoundingBox();
    return this.level().noCollision(new AABB(bb.minX + x, bb.minY - (double) distance - 1.0E-5F, bb.minZ + z, bb.maxX + x, bb.minY, bb.maxZ + z));
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
    if (!this.abilitiesState.flying) {
      super.makeStuckInBlock(motionMultiplier);
    }
  }

  public boolean canUseGameMasterBlocks() {
    return this.abilitiesState().instabuild && this.permissionLevel() >= 2;
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

  @Override
  protected int getFireImmuneTicks() {
    return 20;
  }

  public Optional<GlobalPos> getLastDeathLocation() {
    return this.lastDeathLocation;
  }

  public void setLastDeathLocation(Optional<GlobalPos> lastDeathLocation) {
    this.lastDeathLocation = lastDeathLocation;
  }

  public FoodData getFoodData() {
    return this.foodData;
  }

  private void tickCooldowns() {
    synchronized (itemCoolDowns) {
      var iterator = itemCoolDowns.object2IntEntrySet().iterator();
      while (iterator.hasNext()) {
        var entry = iterator.next();
        var ticks = entry.getIntValue() - 1;
        if (ticks <= 0) {
          iterator.remove();
        } else {
          entry.setValue(ticks);
        }
      }
    }
  }

  public void startAutoSpinAttack(int ticks, float damage, ItemStack itemStack) {
    this.autoSpinAttackTicks = ticks;
    this.autoSpinAttackDmg = damage;
    this.autoSpinAttackItemStack = itemStack;
  }

  public void attack(Entity target) {
    if (target.isAttackable()) {
      if (!target.skipAttackInteraction(this)) {
        var f = this.isAutoSpinAttack() ? this.autoSpinAttackDmg : (float) this.attributeValue(AttributeType.ATTACK_DAMAGE);
        var strengthScale = this.getAttackStrengthScale(0.5F);
        f *= 0.2F + strengthScale * strengthScale * 0.8F;
        this.resetAttackStrengthTicker();

        if (f > 0.0F) {
          var highStrength = strengthScale > 0.9F;
          var extraKnockback = this.isSprinting() && highStrength;

          if (target.hurtClient()) {
            var knockback = this.getKnockback() + (extraKnockback ? 1.0F : 0.0F);
            if (knockback > 0.0F) {
              if (target instanceof LivingEntity lv6) {
                lv6.knockback(
                  knockback * 0.5F,
                  MathHelper.sin(this.yRot() * (float) (Math.PI / 180.0)),
                  -MathHelper.cos(this.yRot() * (float) (Math.PI / 180.0))
                );
              } else {
                target.push(
                  -MathHelper.sin(this.yRot() * (float) (Math.PI / 180.0)) * knockback * 0.5F,
                  0.1,
                  MathHelper.cos(this.yRot() * (float) (Math.PI / 180.0)) * knockback * 0.5F
                );
              }

              this.setDeltaMovement(this.getDeltaMovement().mul(0.6, 1.0, 0.6));
              this.setSprinting(false);
            }
          }
        }
      }
    }
  }
}
