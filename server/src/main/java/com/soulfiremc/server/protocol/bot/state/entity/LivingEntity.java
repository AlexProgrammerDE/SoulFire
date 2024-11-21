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
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;

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

  public boolean isSpectator() {
    return false; // TODO
  }

  public boolean isSleeping() {
    return false; // TODO
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
}
