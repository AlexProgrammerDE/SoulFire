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

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import com.soulfiremc.server.data.EntityType;
import com.soulfiremc.server.data.FluidTags;
import com.soulfiremc.server.protocol.bot.state.EntityEffectState;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3d;

/**
 * Represents the bot itself as an entity.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class LivingEntity extends Entity {
  private final EntityEffectState effectState = new EntityEffectState();
  public boolean swinging;
  private boolean discardFriction = false;
  public int swingTime;
  public int removeArrowTime;
  public int removeStingerTime;
  public int hurtTime;
  public int hurtDuration;
  public int deathTime;
  public float oAttackAnim;
  public float attackAnim;
  protected int attackStrengthTicker;
  public final int invulnerableDuration = 20;
  public final float timeOffs;
  public final float rotA;
  public float yBodyRot;
  public float yBodyRotO;
  public float yHeadRot;
  public float yHeadRotO;
  protected boolean dead;
  protected int noActionTime;
  protected float oRun;
  protected float run;
  protected float animStep;
  protected float animStepO;
  protected float rotOffs;
  protected int deathScore;
  protected float lastHurt;
  protected boolean jumping;
  public float xxa;
  public float yya;
  public float zza;
  protected int lerpSteps;
  protected double lerpX;
  protected double lerpY;
  protected double lerpZ;
  protected double lerpYRot;
  protected double lerpXRot;
  protected double lerpYHeadRot;
  protected int lerpHeadSteps;
  private boolean effectsDirty = true;
  private float speed;
  private int noJumpDelay;
  private float absorptionAmount;
  protected int useItemRemaining;
  protected int fallFlyTicks;
  private long lastDamageStamp;
  protected int autoSpinAttackTicks;
  private float swimAmount;
  private float swimAmountO;
  private float heaslth;

  public LivingEntity(int entityId, UUID uuid, EntityType entityType, Level level, double x, double y, double z,
                      float yaw, float pitch, float headYaw,
                      double motionX, double motionY, double motionZ) {
    super(entityId, uuid, entityType, level, x, y, z, yaw, pitch, headYaw, motionX, motionY, motionZ);
  }

  @Override
  public void tick() {
    super.tick();
    this.updatingUsingItem();
    this.updateSwimAmount();

    this.aiStep();

    var d = this.x() - this.xo;
    var e = this.z() - this.zo;
    var f = (float) (d * d + e * e);
    var g = this.yBodyRot;
    var h = 0.0F;
    this.oRun = this.run;
    var k = 0.0F;
    if (f > 0.0025000002F) {
      k = 1.0F;
      h = (float) Math.sqrt((double) f) * 3.0F;
      var l = (float) Math.atan2(e, d) * (180.0F / (float) Math.PI) - 90.0F;
      var m = Math.abs(MathHelper.wrapDegrees(this.getYRot()) - l);
      if (95.0F < m && m < 265.0F) {
        g = l - 180.0F;
      } else {
        g = l;
      }
    }

    if (this.attackAnim > 0.0F) {
      g = this.getYRot();
    }

    if (!this.onGround()) {
      k = 0.0F;
    }

    this.run += (k - this.run) * 0.3F;

    h = this.tickHeadTurn(g, h);

    while (this.getYRot() - this.yRotO < -180.0F) {
      this.yRotO -= 360.0F;
    }

    while (this.getYRot() - this.yRotO >= 180.0F) {
      this.yRotO += 360.0F;
    }

    while (this.yBodyRot - this.yBodyRotO < -180.0F) {
      this.yBodyRotO -= 360.0F;
    }

    while (this.yBodyRot - this.yBodyRotO >= 180.0F) {
      this.yBodyRotO += 360.0F;
    }

    while (this.getXRot() - this.xRotO < -180.0F) {
      this.xRotO -= 360.0F;
    }

    while (this.getXRot() - this.xRotO >= 180.0F) {
      this.xRotO += 360.0F;
    }

    while (this.yHeadRot - this.yHeadRotO < -180.0F) {
      this.yHeadRotO -= 360.0F;
    }

    while (this.yHeadRot - this.yHeadRotO >= 180.0F) {
      this.yHeadRotO += 360.0F;
    }

    this.animStep += h;
    if (this.isFallFlying()) {
      ++this.fallFlyTicks;
    } else {
      this.fallFlyTicks = 0;
    }

    if (this.isSleeping()) {
      this.setXRot(0.0F);
    }

    this.refreshDirtyAttributes();
  }

  @Override
  public void baseTick() {
    this.oAttackAnim = this.attackAnim;
    if (this.firstTick) {
      this.getSleepingPos().ifPresent(this::setPosToBed);
    }

    super.baseTick();

    if (this.hurtTime > 0) {
      --this.hurtTime;
    }

    if (this.invulnerableTime > 0) {
      --this.invulnerableTime;
    }

    effectState.tick();
    this.animStepO = this.animStep;
    this.yBodyRotO = this.yBodyRot;
    this.yHeadRotO = this.yHeadRot;
    this.yRotO = this.getYRot();
    this.xRotO = this.getXRot();
  }

  protected float tickHeadTurn(float yRot, float animStep) {
    var f = MathHelper.wrapDegrees(yRot - this.yBodyRot);
    this.yBodyRot += f * 0.3F;
    var g = MathHelper.wrapDegrees(this.getYRot() - this.yBodyRot);
    var h = this.getMaxHeadRotationRelativeToBody();
    if (Math.abs(g) > h) {
      this.yBodyRot += g - (float)MathHelper.sign((double)g) * h;
    }

    var bl = g < -90.0F || g >= 90.0F;
    if (bl) {
      animStep *= -1.0F;
    }

    return animStep;
  }

  protected float getMaxHeadRotationRelativeToBody() {
    return 50.0F;
  }

  public void aiStep() {
    if (this.noJumpDelay > 0) {
      --this.noJumpDelay;
    }

    this.syncPacketPositionCodec(this.x(), this.y(), this.z());

    this.setDeltaMovement(this.getDeltaMovement().scale(0.98));

    var vec3 = this.getDeltaMovement();
    var d = vec3.getX();
    var e = vec3.getY();
    var f = vec3.getZ();
    if (Math.abs(vec3.getX()) < 0.003) {
      d = 0.0;
    }

    if (Math.abs(vec3.getY()) < 0.003) {
      e = 0.0;
    }

    if (Math.abs(vec3.getZ()) < 0.003) {
      f = 0.0;
    }

    this.setDeltaMovement(d, e, f);
    if (this.isImmobile()) {
      this.jumping = false;
      this.xxa = 0.0F;
      this.zza = 0.0F;
    }

    if (this.jumping && this.isAffectedByFluids()) {
      double g;
      if (this.isInLava()) {
        g = this.getFluidHeight(FluidTags.LAVA);
      } else {
        g = this.getFluidHeight(FluidTags.WATER);
      }

      var bl = this.isInWater() && g > 0.0;
      double h = this.getFluidJumpThreshold();
      if (!bl || this.onGround() && !(g > h)) {
        if (!this.isInLava() || this.onGround() && !(g > h)) {
          if ((this.onGround() || bl && g <= h) && this.noJumpDelay == 0) {
            this.jumpFromGround();
            this.noJumpDelay = 10;
          }
        } else {
          this.jumpInLiquid(FluidTags.LAVA);
        }
      } else {
        this.jumpInLiquid(FluidTags.WATER);
      }
    } else {
      this.noJumpDelay = 0;
    }

    this.xxa *= 0.98F;
    this.zza *= 0.98F;
    this.updateFallFlying();
    AABB aABB = this.boundingBox();
    var vec32 = Vector3d.from((double) this.xxa, (double) this.yya, (double) this.zza);
    if (this.hasEffect(Effect.SLOW_FALLING) || this.hasEffect(Effect.LEVITATION)) {
      this.resetFallDistance();
    }

    this.travel(vec32);
  }

  public void travel(Vector3d travelVector) {
    var d = 0.08;
    var bl = this.getDeltaMovement().getY() <= 0.0;
    if (bl && this.hasEffect(Effect.SLOW_FALLING)) {
      d = 0.01;
    }

    FluidState fluidState = this.level().getFluidState(this.blockPosition());
    if (this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState)) {
      var e = this.y();
      var f = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
      var g = 0.02F;
      var h = (float) EnchantmentHelper.getDepthStrider(this);
      if (h > 3.0F) {
        h = 3.0F;
      }

      if (!this.onGround()) {
        h *= 0.5F;
      }

      if (h > 0.0F) {
        f += (0.54600006F - f) * h / 3.0F;
        g += (this.getSpeed() - g) * h / 3.0F;
      }

      if (this.hasEffect(Effect.DOLPHINS_GRACE)) {
        f = 0.96F;
      }

      this.moveRelative(g, travelVector);
      this.move(MoverType.SELF, this.getDeltaMovement());
      var vec3 = this.getDeltaMovement();
      if (this.horizontalCollision && this.onClimbable()) {
        vec3 = Vector3d.from(vec3.getX(), 0.2, vec3.getZ());
      }

      this.setDeltaMovement(vec3.mul((double) f, 0.8F, (double) f));
      Vector3d vec32 = this.getFluidFallingAdjustedMovement(d, bl, this.getDeltaMovement());
      this.setDeltaMovement(vec32);
      if (this.horizontalCollision && this.isFree(vec32.getX(), vec32.getY() + 0.6F - this.y() + e, vec32.getZ())) {
        this.setDeltaMovement(vec32.getX(), 0.3F, vec32.getZ());
      }
    } else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidState)) {
      var e = this.y();
      this.moveRelative(0.02F, travelVector);
      this.move(MoverType.SELF, this.getDeltaMovement());
      if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.8F, 0.5));
        Vector3d vec33 = this.getFluidFallingAdjustedMovement(d, bl, this.getDeltaMovement());
        this.setDeltaMovement(vec33);
      } else {
        this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
      }

        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -d / 4.0, 0.0));

      var vec33 = this.getDeltaMovement();
      if (this.horizontalCollision && this.isFree(vec33.getX(), vec33.getY() + 0.6F - this.y() + e, vec33.getZ())) {
        this.setDeltaMovement(vec33.getX(), 0.3F, vec33.getZ());
      }
    } else if (this.isFallFlying()) {
      this.checkSlowFallDistance();
      var vec34 = this.getDeltaMovement();
      Vector3d vec35 = this.getLookAngle();
      var f = this.getXRot() * (float) (Math.PI / 180.0);
      var i = Math.sqrt(vec35.getX() * vec35.getX() + vec35.getZ() * vec35.getZ());
      double j = vec34.horizontalDistance();
      var k = vec35.length();
      var l = Math.cos((double) f);
      l = l * l * Math.min(1.0, k / 0.4);
      vec34 = this.getDeltaMovement().add(0.0, d * (-1.0 + l * 0.75), 0.0);
      if (vec34.getY() < 0.0 && i > 0.0) {
        var m = vec34.getY() * -0.1 * l;
        vec34 = vec34.add(vec35.getX() * m / i, m, vec35.getZ() * m / i);
      }

      if (f < 0.0F && i > 0.0) {
        var m = j * (double) (-Math.sin(f)) * 0.04;
        vec34 = vec34.add(-vec35.getX() * m / i, m * 3.2, -vec35.getZ() * m / i);
      }

      if (i > 0.0) {
        vec34 = vec34.add((vec35.getX() / i * j - vec34.getX()) * 0.1, 0.0, (vec35.getZ() / i * j - vec34.getZ()) * 0.1);
      }

      this.setDeltaMovement(vec34.multiply(0.99F, 0.98F, 0.99F));
      this.move(MoverType.SELF, this.getDeltaMovement());
    } else {
      BlockPos blockPos = this.getBlockPosBelowThatAffectsMyMovement();
      float p = this.level().getBlockState(blockPos).getBlock().getFriction();
      var f = this.onGround() ? p * 0.91F : 0.91F;
      Vector3d vec36 = this.handleRelativeFrictionAndCalculateMovement(travelVector, p);
      double q = vec36.getY();
      if (this.hasEffect(Effect.LEVITATION)) {
        q += (0.05 * (double) (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - vec36.y) * 0.2;
      } else if (!this.level().hasChunkAt(blockPos)) {
        if (this.y() > (double) this.level().getMinBuildHeight()) {
          q = -0.1;
        } else {
          q = 0.0;
        }
      } else {
        q -= d;
      }

      if (this.shouldDiscardFriction()) {
        this.setDeltaMovement(vec36.getX(), q, vec36.getZ());
      } else {
        this.setDeltaMovement(vec36.getX() * (double) f, q * 0.98F, vec36.getZ() * (double) f);
      }
    }
  }

  private Vector3d handleOnClimbable(Vector3d deltaMovement) {
    if (this.onClimbable()) {
      this.resetFallDistance();
      float f = 0.15F;
      double d = MathHelper.doubleClamp(deltaMovement.x, -0.15F, 0.15F);
      double e = MathHelper.doubleClamp(deltaMovement.z, -0.15F, 0.15F);
      double g = Math.max(deltaMovement.getY(), -0.15F);
      if (g < 0.0 && !this.getFeetBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
        g = 0.0;
      }

      deltaMovement = Vector3d.from(d, g, e);
    }

    return deltaMovement;
  }

  private float getFrictionInfluencedSpeed(float friction) {
    return this.onGround() ? this.getSpeed() * (0.21600002F / (friction * friction * friction)) : this.getFlyingSpeed();
  }

  protected float getFlyingSpeed() {
    return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1F : 0.02F;
  }

  public float getSpeed() {
    return this.speed;
  }

  public void setSpeed(float speed) {
    this.speed = speed;
  }

  public boolean hasEffect(Effect effect) {
    return this.effectState.hasEffect(effect);
  }

  public int getEffectAmplifier(Effect effect) {
    return this.effectState.getEffectAmplifier(effect);
  }

  public boolean isImmobile() {
    return isDeadOrDying();
  }

  public boolean isDeadOrDying() {
    return this.getHealth() <= 0.0F;
  }
}
