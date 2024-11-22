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
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;

import java.util.Optional;

@Getter
@Setter
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
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<Vector3i> lastClimbablePos = Optional.empty();

  public LivingEntity(EntityType entityType, Level level) {
    super(entityType, level);
  }

  @Override
  public void tick() {
    super.tick();

    // this.aiStep();

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
      // this.serverAiStep(); // TODO
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

    // this.travel(moveVector); // TODO

    this.removeFrost();
    this.tryAddFrost();

    // this.pushEntities(); // TODO
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
      for (var lv : EquipmentSlot.values()) {
        if (canGlideUsing(this.getItemBySlot(lv), lv)) {
          return true;
        }
      }
    }

    return false;
  }

  public abstract SFItemStack getItemBySlot(EquipmentSlot slot);
}
