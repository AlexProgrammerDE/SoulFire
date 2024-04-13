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

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.data.FluidTags;
import com.soulfiremc.server.data.ResourceKey;
import com.soulfiremc.server.protocol.bot.state.EntityAttributeState;
import com.soulfiremc.server.protocol.bot.state.EntityMetadataState;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Nullable;

@Slf4j
@Getter
@Setter
public abstract class Entity {
  public static final float BREATHING_DISTANCE_BELOW_EYES = 0.11111111F;
  private final EntityMetadataState metadataState = new EntityMetadataState();
  private final EntityAttributeState attributeState = new EntityAttributeState();
  private final int entityId;
  private final UUID uuid;
  private final EntityType entityType;
  protected Level level;
  public boolean blocksBuilding;
  private ImmutableList<Entity> passengers = ImmutableList.of();
  protected int boardingCooldown;
  protected Random random = new Random();
  @Nullable
  private Entity vehicle;
  public double xo;
  public double yo;
  public double zo;
  private Vector3d position;
  private Vector3i blockPosition;
  private Vector3d deltaMovement = Vector3d.ZERO;
  private float yRot;
  private float xRot;
  public float yRotO;
  public float xRotO;
  private boolean onGround;
  public boolean horizontalCollision;
  public boolean verticalCollision;
  public boolean verticalCollisionBelow;
  public boolean minorHorizontalCollision;
  public boolean hurtMarked;
  public float walkDistO;
  public float walkDist;
  public float moveDist;
  public float flyDist;
  public float fallDistance;
  private float nextStep = 1.0F;
  public double xOld;
  public double yOld;
  public double zOld;
  private float maxUpStep;
  public boolean noPhysics;
  public int tickCount;
  protected boolean wasTouchingWater;
  protected boolean wasEyeInWater;
  protected Object2DoubleMap<ResourceKey> fluidHeight = new Object2DoubleArrayMap<>(2);
  private final Set<ResourceKey> fluidOnEyes = new HashSet<>();
  public int invulnerableTime;
  protected boolean firstTick = true;
  protected static final int FLAG_ONFIRE = 0;
  private static final int FLAG_SHIFT_KEY_DOWN = 1;
  private static final int FLAG_SPRINTING = 3;
  private static final int FLAG_SWIMMING = 4;
  private static final int FLAG_INVISIBLE = 5;
  protected static final int FLAG_GLOWING = 6;
  protected static final int FLAG_FALL_FLYING = 7;
  public boolean noCulling;
  public boolean hasImpulse;
  private int portalCooldown;
  protected boolean isInsidePortal;
  protected int portalTime;
  private boolean invulnerable;
  private boolean hasGlowingTag;
  private final Set<String> tags = Sets.newHashSet();
  private final double[] pistonDeltas = new double[] {0.0, 0.0, 0.0};
  private long pistonDeltasGameTime;
  private float eyeHeight;
  public boolean isInPowderSnow;
  public boolean wasInPowderSnow;
  public boolean wasOnFire;
  private boolean onGroundNoBlocks = false;
  private float crystalSoundIntensity;
  private int lastCrystalSoundPlayTick;
  private boolean hasVisualFire;

  public Entity(int entityId, UUID uuid, EntityType entityType,
                Level level,
                double x, double y, double z,
                float yaw, float pitch, float headYaw,
                double motionX, double motionY, double motionZ) {
    this.entityId = entityId;
    this.uuid = uuid;
    this.entityType = entityType;
    this.level = level;
  }

  public void tick() {
    baseTick();
  }

  public void baseTick() {
    this.feetBlockState = null;

    if (this.boardingCooldown > 0) {
      --this.boardingCooldown;
    }

    this.walkDistO = this.walkDist;
    this.xRotO = this.getXRot();
    this.yRotO = this.getYRot();

    this.wasInPowderSnow = this.isInPowderSnow;
    this.isInPowderSnow = false;
    this.updateInWaterStateAndDoFluidPushing();
    this.updateFluidOnEyes();
    this.updateSwimming();

    if (this.isInLava()) {
      this.fallDistance *= 0.5F;
    }

    this.firstTick = false;
  }

  public void handleEntityEvent(EntityEvent event) {
    log.debug("Unhandled entity event for entity {}: {}", entityId, event.name());
  }

  public double attributeValue(AttributeType type) {
    return attributeState.getOrCreateAttribute(type).calculateValue();
  }

  public Vector3d getDeltaMovement() {
    return this.deltaMovement;
  }

  public void setDeltaMovement(Vector3d deltaMovement) {
    this.deltaMovement = deltaMovement;
  }

  public void addDeltaMovement(Vector3d addend) {
    this.setDeltaMovement(this.getDeltaMovement().add(addend));
  }

  public void setDeltaMovement(double x, double y, double z) {
    this.setDeltaMovement(Vector3d.from(x, y, z));
  }

  public final int blockX() {
    return this.blockPosition.getX();
  }

  public final double x() {
    return this.position.getX();
  }

  public double x(double scale) {
    return this.position.getX() + (double) this.getBbWidth() * scale;
  }

  public double randomX(double scale) {
    return this.x((2.0 * this.random.nextDouble() - 1.0) * scale);
  }

  public final int blockY() {
    return this.blockPosition.getY();
  }

  public final double y() {
    return this.position.getY();
  }

  public double y(double scale) {
    return this.position.getY() + (double) this.getBbHeight() * scale;
  }

  public double randomY() {
    return this.y(this.random.nextDouble());
  }

  public double eyeY() {
    return this.position.getY() + (double) this.eyeHeight;
  }

  public final int blockZ() {
    return this.blockPosition.getZ();
  }

  public final double z() {
    return this.position.getZ();
  }

  public double z(double scale) {
    return this.position.getZ() + (double) this.getBbWidth() * scale;
  }

  public double randomZ(double scale) {
    return this.z((2.0 * this.random.nextDouble() - 1.0) * scale);
  }

  public void setIsInPowderSnow(boolean isInPowderSnow) {
    this.isInPowderSnow = isInPowderSnow;
  }

  public float getYRot() {
    return this.yRot;
  }

  public float getVisualRotationYInDegrees() {
    return this.getYRot();
  }

  public void setYRot(float yRot) {
    if (!Float.isFinite(yRot)) {
      return;
    }

    this.yRot = yRot;
  }

  public float getXRot() {
    return this.xRot;
  }

  public void setXRot(float xRot) {
    if (!Float.isFinite(xRot)) {
      return;
    }

    this.xRot = xRot;
  }

  public boolean canSprint() {
    return false;
  }

  public float maxUpStep() {
    return this.maxUpStep;
  }

  public void setMaxUpStep(float maxUpStep) {
    this.maxUpStep = maxUpStep;
  }

  public boolean isEyeInFluid(ResourceKey fluidTag) {
    return this.fluidOnEyes.contains(fluidTag);
  }

  public boolean isInLava() {
    return !this.firstTick && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
  }

  public boolean isPassenger() {
    return this.getVehicle() != null;
  }

  @Nullable
  public Entity getVehicle() {
    return this.vehicle;
  }

  public boolean isSpectator() {
    return false;
  }

  public void updateSwimming() {
    if (this.isSwimming()) {
      this.setSwimming(this.isSprinting() && this.isInWater() && !this.isPassenger());
    } else {
      this.setSwimming(this.isSprinting() && this.isUnderWater() && !this.isPassenger() &&
        this.level().getFluidState(this.blockPosition).is(FluidTags.WATER));
    }
  }

  protected boolean updateInWaterStateAndDoFluidPushing() {
    this.fluidHeight.clear();
    this.updateInWaterStateAndDoWaterCurrentPushing();
    double d = this.level().dimensionType().ultraWarm() ? 0.007 : 0.0023333333333333335;
    boolean bl = this.updateFluidHeightAndDoFluidPushing(FluidTags.LAVA, d);
    return this.isInWater() || bl;
  }

  void updateInWaterStateAndDoWaterCurrentPushing() {
    Entity var2 = this.getVehicle();
    if (var2 instanceof Boat boat && !boat.isUnderWater()) {
      this.wasTouchingWater = false;
      return;
    }

    if (this.updateFluidHeightAndDoFluidPushing(FluidTags.WATER, 0.014)) {
      this.resetFallDistance();
      this.wasTouchingWater = true;
    } else {
      this.wasTouchingWater = false;
    }
  }

  public boolean isInWater() {
    return this.wasTouchingWater;
  }

  private void updateFluidOnEyes() {
    this.wasEyeInWater = this.isEyeInFluid(FluidTags.WATER);
    this.fluidOnEyes.clear();
    double d = this.eyeY() - 0.11111111F;
    Entity entity = this.getVehicle();
    if (entity instanceof Boat boat && !boat.isUnderWater() && boat.getBoundingBox().maxY >= d &&
      boat.getBoundingBox().minY <= d) {
      return;
    }

    Vector3i blockPos = Vector3i.from(this.x(), d, this.z());
    FluidState fluidState = this.level().getFluidState(blockPos);
    double e = (double) ((float) blockPos.getY() + fluidState.getHeight(this.level(), blockPos));
    if (e > d) {
      fluidState.getTags().forEach(this.fluidOnEyes::add);
    }
  }

  public void checkSlowFallDistance() {
    if (this.getDeltaMovement().getY() > -0.5 && this.fallDistance > 1.0F) {
      this.fallDistance = 1.0F;
    }
  }

  public void resetFallDistance() {
    this.fallDistance = 0.0F;
  }

  @Nullable
  public LivingEntity getControllingPassenger() {
    return null;
  }
}
