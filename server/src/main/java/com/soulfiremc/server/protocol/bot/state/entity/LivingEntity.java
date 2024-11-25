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

import com.soulfiremc.server.data.*;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.VectorHelper;
import com.soulfiremc.server.util.mcstructs.MoverType;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;

import java.util.Optional;

@Getter
public abstract class LivingEntity extends Entity {
  private static final Key SPEED_MODIFIER_POWDER_SNOW_ID = Key.key("powder_snow");
  private static final Key SPRINTING_MODIFIER_ID = Key.key("sprinting");
  private static final Attribute.Modifier SPEED_MODIFIER_SPRINTING = new Attribute.Modifier(
    SPRINTING_MODIFIER_ID, 0.3F, ModifierOperation.ADD_MULTIPLIED_TOTAL
  );
  protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(0.2F);
  protected int fallFlyTicks;
  protected float appliedScale = 1.0F;
  public float xxa;
  public float yya;
  public float zza;
  protected boolean jumping;
  private int noJumpDelay;
  private float speed;
  private final boolean discardFriction = false;
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<Vector3i> lastClimbablePos = Optional.empty();

  public LivingEntity(EntityType entityType, Level level) {
    super(entityType, level);
  }

  @Override
  public void tick() {
    super.tick();

    this.aiStep();

    if (this.isFallFlying()) {
      this.fallFlyTicks++;
    } else {
      this.fallFlyTicks = 0;
    }

    if (this.isSleeping()) {
      this.setXRot(0.0F);
    }

    var currentScale = this.getScale();
    if (currentScale != this.appliedScale) {
      this.appliedScale = currentScale;
      this.refreshDimensions();
    }
  }

  @Override
  public void baseTick() {
    super.baseTick();

    this.effectState.tick();
  }

  public static boolean canGlideUsing(SFItemStack stack, EquipmentSlot slow) {
    if (!stack.has(DataComponentType.GLIDER)) {
      return false;
    } else {
      var equippable = stack.get(DataComponentType.EQUIPPABLE);
      return equippable != null && slow == EquipmentSlot.fromMCPl(equippable.slot()) && !stack.nextDamageWillBreak();
    }
  }

  @Override
  public void fromAddEntityPacket(ClientboundAddEntityPacket packet) {
    var x = packet.getX();
    var y = packet.getY();
    var z = packet.getZ();
    var yRot = packet.getYaw();
    var xRot = packet.getPitch();
    this.syncPacketPositionCodec(x, y, z);
    this.entityId(packet.getEntityId());
    this.uuid(packet.getUuid());
    this.absMoveTo(x, y, z, yRot, xRot);
    this.setDeltaMovement(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
  }

  public void absMoveTo(double x, double y, double z, float yRot, float xRot) {
    this.absMoveTo(x, y, z);
    this.absRotateTo(yRot, xRot);
  }

  public void absRotateTo(float yRot, float xRot) {
    this.setYRot(yRot % 360.0F);
    this.setXRot(MathHelper.clamp(xRot, -90.0F, 90.0F) % 360.0F);
    this.yRotO = this.yRot();
    this.xRotO = this.xRot();
  }

  public void absMoveTo(double x, double y, double z) {
    var xClamp = MathHelper.clamp(x, -3.0E7, 3.0E7);
    var zClamp = MathHelper.clamp(z, -3.0E7, 3.0E7);
    this.xo = xClamp;
    this.yo = y;
    this.zo = zClamp;
    this.setPos(xClamp, y, zClamp);
  }

  public void aiStep() {
    if (this.noJumpDelay > 0) {
      this.noJumpDelay--;
    }

    if (!this.isEffectiveAi()) {
      this.setDeltaMovement(this.getDeltaMovement().mul(0.98));
    }

    var deltaMovement = this.getDeltaMovement();
    var deltaX = deltaMovement.getX();
    var deltaY = deltaMovement.getY();
    var deltaZ = deltaMovement.getZ();
    if (Math.abs(deltaMovement.getX()) < 0.003) {
      deltaX = 0.0;
    }

    if (Math.abs(deltaMovement.getY()) < 0.003) {
      deltaY = 0.0;
    }

    if (Math.abs(deltaMovement.getZ()) < 0.003) {
      deltaZ = 0.0;
    }

    this.setDeltaMovement(deltaX, deltaY, deltaZ);
    if (this.isImmobile()) {
      this.jumping = false;
      this.xxa = 0.0F;
      this.zza = 0.0F;
    } else if (this.isEffectiveAi()) {
      this.serverAiStep();
    }

    if (this.jumping && this.isAffectedByFluids()) {
      double g;
      if (this.isInLava()) {
        g = this.getFluidHeight(FluidTags.LAVA);
      } else {
        g = this.getFluidHeight(FluidTags.WATER);
      }

      var bl = this.isInWater() && g > 0.0;
      var h = this.getFluidJumpThreshold();
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
    if (this.isFallFlying()) {
      this.updateFallFlying();
    }

    var moveVector = Vector3d.from(this.xxa, this.yya, (double) this.zza);
    if (this.effectState().hasEffect(EffectType.SLOW_FALLING) || this.effectState().hasEffect(EffectType.LEVITATION)) {
      this.resetFallDistance();
    }

    this.travel(moveVector);

    if (this.isControlledByLocalInstance()) {
      this.applyEffectsFromBlocks();
    }

    this.removeFrost();
    this.tryAddFrost();

    this.pushEntities();
  }

  protected void serverAiStep() {
  }

  public void travel(Vector3d travelVector) {
    if (this.isControlledByLocalInstance()) {
      var currentFluidState = this.level().getBlockState(this.blockPosition()).fluidState();
      if ((this.isInWater() || this.isInLava()) && this.isAffectedByFluids() && !this.canStandOnFluid(currentFluidState)) {
        this.travelInFluid(travelVector);
      } else if (this.isFallFlying()) {
        this.travelFallFlying();
      } else {
        this.travelInAir(travelVector);
      }
    }
  }

  private void travelInFluid(Vector3d arg) {
    var bl = this.getDeltaMovement().getY() <= 0.0;
    var d = this.y();
    var e = this.getEffectiveGravity();
    if (this.isInWater()) {
      var f = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
      var g = 0.02F;
      var h = (float) this.attributeValue(AttributeType.WATER_MOVEMENT_EFFICIENCY);
      if (!this.onGround()) {
        h *= 0.5F;
      }

      if (h > 0.0F) {
        f += (0.54600006F - f) * h;
        g += (this.getSpeed() - g) * h;
      }

      if (this.effectState.hasEffect(EffectType.DOLPHINS_GRACE)) {
        f = 0.96F;
      }

      this.moveRelative(g, arg);
      this.move(MoverType.SELF, this.getDeltaMovement());
      var lv = this.getDeltaMovement();
      if (this.horizontalCollision && this.onClimbable()) {
        lv = Vector3d.from(lv.getX(), 0.2, lv.getZ());
      }

      lv = lv.mul(f, 0.8F, (double) f);
      this.setDeltaMovement(this.getFluidFallingAdjustedMovement(e, bl, lv));
    } else {
      this.moveRelative(0.02F, arg);
      this.move(MoverType.SELF, this.getDeltaMovement());
      if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
        this.setDeltaMovement(this.getDeltaMovement().mul(0.5, 0.8F, 0.5));
        var lv2 = this.getFluidFallingAdjustedMovement(e, bl, this.getDeltaMovement());
        this.setDeltaMovement(lv2);
      } else {
        this.setDeltaMovement(this.getDeltaMovement().mul(0.5));
      }

      if (e != 0.0) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -e / 4.0, 0.0));
      }
    }

    var newDelta = this.getDeltaMovement();
    if (this.horizontalCollision && this.isFree(newDelta.getX(), newDelta.getY() + 0.6F - this.y() + d, newDelta.getZ())) {
      this.setDeltaMovement(newDelta.getX(), 0.3F, newDelta.getZ());
    }
  }

  public Vector3d getFluidFallingAdjustedMovement(double gravity, boolean isFalling, Vector3d deltaMovement) {
    if (gravity != 0.0 && !this.isSprinting()) {
      double e;
      if (isFalling && Math.abs(deltaMovement.getY() - 0.005) >= 0.003 && Math.abs(deltaMovement.getY() - gravity / 16.0) < 0.003) {
        e = -0.003;
      } else {
        e = deltaMovement.getY() - gravity / 16.0;
      }

      return Vector3d.from(deltaMovement.getX(), e, deltaMovement.getZ());
    } else {
      return deltaMovement;
    }
  }

  protected float getWaterSlowDown() {
    return 0.8F;
  }

  private void travelInAir(Vector3d arg) {
    var blockPosBelow = this.getBlockPosBelowThatAffectsMyMovement();
    var f = this.onGround() ? this.level().getBlockState(blockPosBelow).blockType().friction() : 1.0F;
    var g = f * 0.91F;
    var lv2 = this.handleRelativeFrictionAndCalculateMovement(arg, f);
    var d = lv2.getY();
    var lv3 = this.effectState.getEffect(EffectType.LEVITATION);
    if (lv3.isPresent()) {
      d += (0.05 * (double) (lv3.get().amplifier() + 1) - lv2.getY()) * 0.2;
    } else if (this.level().isChunkPositionLoaded(blockPosBelow.getX(), blockPosBelow.getZ())) {
      d -= this.getEffectiveGravity();
    } else if (this.y() > (double) this.level().getMinBuildHeight()) {
      d = -0.1;
    } else {
      d = 0.0;
    }

    if (this.shouldDiscardFriction()) {
      this.setDeltaMovement(lv2.getX(), d, lv2.getZ());
    } else {
      this.setDeltaMovement(lv2.getX() * (double) g, d * (double) 0.98F, lv2.getZ() * (double) g);
    }
  }

  private Vector3d handleRelativeFrictionAndCalculateMovement(Vector3d deltaMovement, float friction) {
    this.moveRelative(this.getFrictionInfluencedSpeed(friction), deltaMovement);
    this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
    this.move(MoverType.SELF, this.getDeltaMovement());
    var newDelta = this.getDeltaMovement();
    if ((this.horizontalCollision || this.jumping)
      && (this.onClimbable() || this.getInBlockState().blockType() == BlockType.POWDER_SNOW && canEntityWalkOnPowderSnow(this))) {
      newDelta = Vector3d.from(newDelta.getX(), 0.2, newDelta.getZ());
    }

    return newDelta;
  }

  public boolean canEntityWalkOnPowderSnow(Entity entity) {
    if (this.level().tagsState().is(entity.entityType(), EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) {
      return true;
    } else {
      return entity instanceof LivingEntity le && le.getItemBySlot(EquipmentSlot.FEET)
        .map(item -> item.type() == ItemType.LEATHER_BOOTS)
        .orElse(false);
    }
  }

  private Vector3d handleOnClimbable(Vector3d deltaMovement) {
    if (this.onClimbable()) {
      this.resetFallDistance();
      var f = 0.15F;
      var d = MathHelper.clamp(deltaMovement.getX(), -0.15F, 0.15F);
      var e = MathHelper.clamp(deltaMovement.getZ(), -0.15F, 0.15F);
      var g = Math.max(deltaMovement.getY(), -0.15F);
      if (g < 0.0 && this.getInBlockState().blockType() != BlockType.SCAFFOLDING && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
        g = 0.0;
      }

      deltaMovement = Vector3d.from(d, g, e);
    }

    return deltaMovement;
  }

  private float getFrictionInfluencedSpeed(float friction) {
    return this.onGround() ? this.getSpeed() * (0.21600002F / (friction * friction * friction)) : this.getFlyingSpeed();
  }

  public boolean shouldDiscardFriction() {
    return this.discardFriction;
  }

  public boolean canStandOnFluid(FluidState fluidState) {
    return false;
  }

  private void travelFallFlying() {
    var deltaMovement = this.getDeltaMovement();
    this.setDeltaMovement(this.updateFallFlyingMovement(deltaMovement));
    this.move(MoverType.SELF, this.getDeltaMovement());
  }

  private Vector3d updateFallFlyingMovement(Vector3d inputDelta) {
    var lookAngle = this.getLookAngle();
    var f = this.xRot() * (float) (Math.PI / 180.0);
    var d = Math.sqrt(lookAngle.getX() * lookAngle.getX() + lookAngle.getZ() * lookAngle.getZ());
    var e = VectorHelper.horizontalDistance(inputDelta);
    var g = this.getEffectiveGravity();
    var h = MathHelper.square(Math.cos(f));
    inputDelta = inputDelta.add(0.0, g * (-1.0 + h * 0.75), 0.0);
    if (inputDelta.getY() < 0.0 && d > 0.0) {
      var i = inputDelta.getY() * -0.1 * h;
      inputDelta = inputDelta.add(lookAngle.getX() * i / d, i, lookAngle.getZ() * i / d);
    }

    if (f < 0.0F && d > 0.0) {
      var i = e * (double) (-MathHelper.sin(f)) * 0.04;
      inputDelta = inputDelta.add(-lookAngle.getX() * i / d, i * 3.2, -lookAngle.getZ() * i / d);
    }

    if (d > 0.0) {
      inputDelta = inputDelta.add((lookAngle.getX() / d * e - inputDelta.getX()) * 0.1, 0.0, (lookAngle.getZ() / d * e - inputDelta.getZ()) * 0.1);
    }

    return inputDelta.mul(0.99F, 0.98F, 0.99F);
  }

  protected void removeFrost() {
    var speedAttribute = this.attributeState().getOrCreateAttribute(AttributeType.MOVEMENT_SPEED);
    if (speedAttribute != null) {
      if (speedAttribute.getModifier(SPEED_MODIFIER_POWDER_SNOW_ID) != null) {
        speedAttribute.removeModifier(SPEED_MODIFIER_POWDER_SNOW_ID);
      }
    }
  }

  protected void tryAddFrost() {
    if (!this.getBlockStateOnLegacy().blockType().air()) {
      var ticksFrozen = this.getTicksFrozen();
      if (ticksFrozen > 0) {
        var speedAttribute = this.attributeState().getOrCreateAttribute(AttributeType.MOVEMENT_SPEED);
        if (speedAttribute == null) {
          return;
        }

        var f = -0.05F * this.getPercentFrozen();
        speedAttribute.addModifier(new Attribute.Modifier(SPEED_MODIFIER_POWDER_SNOW_ID, f, ModifierOperation.ADD_VALUE));
      }
    }
  }

  public Optional<Vector3i> getSleepingPos() {
    return this.metadataState.getMetadata(NamedEntityData.LIVING_ENTITY__SLEEPING_POS, MetadataType.OPTIONAL_POSITION);
  }

  public boolean isSpectator() {
    return false;
  }

  public boolean isSleeping() {
    return this.getSleepingPos().isPresent();
  }

  public abstract boolean isUnderWater();

  @Override
  public final EntityDimensions getDimensions(Pose pose) {
    return pose == Pose.SLEEPING ? SLEEPING_DIMENSIONS : this.getDefaultDimensions(pose).scale(this.getScale());
  }

  protected EntityDimensions getDefaultDimensions(Pose pose) {
    return this.entityType().dimensions().scale(this.getAgeScale());
  }

  public boolean isBaby() {
    return false;
  }

  public float getAgeScale() {
    return this.isBaby() ? 0.5F : 1.0F;
  }

  public final float getScale() {
    return attributeState().hasAttribute(AttributeType.SCALE) ? this.sanitizeScale((float) attributeValue(AttributeType.SCALE)) : 1.0F;
  }

  protected float sanitizeScale(float scale) {
    return scale;
  }

  protected void setLivingEntityFlag(int key, boolean value) {
    int currentFlags = this.metadataState.getMetadata(NamedEntityData.LIVING_ENTITY__LIVING_ENTITY_FLAGS, MetadataType.BYTE);
    if (value) {
      currentFlags |= key;
    } else {
      currentFlags &= ~key;
    }

    this.metadataState.setMetadata(NamedEntityData.LIVING_ENTITY__LIVING_ENTITY_FLAGS, MetadataType.BYTE, (byte) currentFlags);
  }

  @Override
  protected double getDefaultGravity() {
    return this.attributeValue(AttributeType.GRAVITY);
  }

  protected double getEffectiveGravity() {
    var bl = this.getDeltaMovement().getY() <= 0.0;
    return bl && this.effectState().hasEffect(EffectType.SLOW_FALLING) ? Math.min(this.getGravity(), 0.01) : this.getGravity();
  }

  @Override
  public void setSprinting(boolean sprinting) {
    super.setSprinting(sprinting);
    var lv = this.attributeState.getOrCreateAttribute(AttributeType.MOVEMENT_SPEED);
    lv.removeModifier(SPEED_MODIFIER_SPRINTING.id());
    if (sprinting) {
      lv.addModifier(SPEED_MODIFIER_SPRINTING);
    }
  }

  public boolean isSuppressingSlidingDownLadder() {
    return this.isShiftKeyDown();
  }

  public boolean isFallFlying() {
    return this.getSharedFlag(FLAG_FALL_FLYING);
  }

  @Override
  public boolean isVisuallySwimming() {
    return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
  }

  public boolean isAutoSpinAttack() {
    return (this.metadataState.getMetadata(NamedEntityData.LIVING_ENTITY__LIVING_ENTITY_FLAGS, MetadataType.BYTE) & 4) != 0;
  }

  @Override
  protected float getBlockSpeedFactor() {
    return MathHelper.lerp((float) this.attributeValue(AttributeType.MOVEMENT_EFFICIENCY), super.getBlockSpeedFactor(), 1.0F);
  }

  protected void updateFallFlying() {
    this.checkSlowFallDistance();
  }

  protected boolean isAffectedByFluids() {
    return true;
  }

  public final float getMaxHealth() {
    return (float) this.attributeValue(AttributeType.MAX_HEALTH);
  }

  public float getHealth() {
    return this.metadataState.getMetadata(NamedEntityData.LIVING_ENTITY__HEALTH, MetadataType.FLOAT);
  }

  public void setHealth(float health) {
    this.metadataState.setMetadata(NamedEntityData.LIVING_ENTITY__HEALTH, MetadataType.FLOAT, MathHelper.clamp(health, 0.0F, this.getMaxHealth()));
  }

  public boolean isDeadOrDying() {
    return this.getHealth() <= 0.0F;
  }

  protected boolean isImmobile() {
    return this.isDeadOrDying();
  }

  protected float getJumpPower() {
    return this.getJumpPower(1.0F);
  }

  @SuppressWarnings("SameParameterValue")
  protected float getJumpPower(float multiplier) {
    return (float) this.attributeValue(AttributeType.JUMP_STRENGTH) * multiplier * this.getBlockJumpFactor() + this.getJumpBoostPower();
  }

  public float getJumpBoostPower() {
    return this.effectState().hasEffect(EffectType.JUMP) ? 0.1F * ((float) this.effectState.getEffect(EffectType.JUMP).orElseThrow().amplifier() + 1.0F) : 0.0F;
  }

  public void jumpFromGround() {
    var f = this.getJumpPower();
    if (!(f <= 1.0E-5F)) {
      var deltaMovement = this.getDeltaMovement();
      this.setDeltaMovement(deltaMovement.getX(), Math.max(f, deltaMovement.getY()), deltaMovement.getZ());
      if (this.isSprinting()) {
        var rot = this.yRot() * (float) (Math.PI / 180.0);
        this.addDeltaMovement(Vector3d.from((double) (-MathHelper.sin(rot)) * 0.2, 0.0, (double) MathHelper.cos(rot) * 0.2));
      }

      this.hasImpulse = true;
    }
  }

  protected void pushEntities() {
    this.level().getEntities(this.getBoundingBox())
      .stream()
      .filter(e -> e instanceof Player)
      .forEach(this::doPush);
  }

  protected void doPush(Entity entity) {
    entity.push(this);
  }

  @Override
  public void push(Entity entity) {
    if (!this.isSleeping()) {
      super.push(entity);
    }
  }

  @Override
  public boolean isPushable() {
    return this.isAlive() && !this.isSpectator() && !this.onClimbable();
  }

  protected void goDownInWater() {
    this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04F, 0.0));
  }

  protected void jumpInLiquid(TagKey<FluidType> fluidTag) {
    this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04F, 0.0));
  }

  public float getSpeed() {
    return this.speed;
  }

  public void setSpeed(float speed) {
    this.speed = speed;
  }

  public boolean onClimbable() {
    if (this.isSpectator()) {
      return false;
    } else {
      var blockPos = this.blockPosition();
      var inState = this.getInBlockState();
      if (this.level().tagsState().is(inState.blockType(), BlockTags.CLIMBABLE)) {
        this.lastClimbablePos = Optional.of(blockPos);
        return true;
      } else if (inState.blockType().trapDoorBlock() && this.trapdoorUsableAsLadder(blockPos, inState)) {
        this.lastClimbablePos = Optional.of(blockPos);
        return true;
      } else {
        return false;
      }
    }
  }

  private boolean trapdoorUsableAsLadder(Vector3i pos, BlockState state) {
    if (!state.properties().getBoolean("open")) {
      return false;
    } else {
      var downState = this.level().getBlockState(pos.down());
      return downState.blockType() == BlockType.LADDER && downState.properties().getString("facing").equals(state.properties().getString("facing"));
    }
  }

  public boolean isUsingItem() {
    return (this.metadataState.getMetadata(NamedEntityData.LIVING_ENTITY__LIVING_ENTITY_FLAGS, MetadataType.BYTE) & 1) > 0;
  }

  protected boolean canGlide() {
    if (!this.onGround() && !this.effectState().hasEffect(EffectType.LEVITATION)) {
      for (var slot : EquipmentSlot.values()) {
        var item = this.getItemBySlot(slot);
        if (item.isPresent() && canGlideUsing(item.get(), slot)) {
          return true;
        }
      }
    }

    return false;
  }

  public abstract Optional<SFItemStack> getItemBySlot(EquipmentSlot slot);

  protected float getFlyingSpeed() {
    return 0.02F;
  }

  @Override
  public float maxUpStep() {
    return (float) this.attributeValue(AttributeType.STEP_HEIGHT);
  }
}
