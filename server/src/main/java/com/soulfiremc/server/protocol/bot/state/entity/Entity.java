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
import com.soulfiremc.server.protocol.bot.state.EntityAttributeState;
import com.soulfiremc.server.protocol.bot.state.EntityEffectState;
import com.soulfiremc.server.protocol.bot.state.EntityMetadataState;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.EntityMovement;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.mcstructs.AABB;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.RotationOrigin;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.ObjectData;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Getter
@Setter
public abstract class Entity {
  protected static final int FLAG_ONFIRE = 0;
  protected static final int FLAG_GLOWING = 6;
  protected static final int FLAG_FALL_FLYING = 7;
  private static final int FLAG_SHIFT_KEY_DOWN = 1;
  private static final int FLAG_SPRINTING = 3;
  private static final int FLAG_SWIMMING = 4;
  private static final int FLAG_INVISIBLE = 5;
  public static final float BREATHING_DISTANCE_BELOW_EYES = 0.11111111F;
  protected final EntityAttributeState attributeState = new EntityAttributeState();
  protected final EntityEffectState effectState = new EntityEffectState();
  protected final Set<TagKey<FluidType>> fluidOnEyes = new HashSet<>();
  protected final EntityType entityType;
  protected final EntityMetadataState metadataState;
  protected float fallDistance;
  protected UUID uuid;
  protected ObjectData data;
  protected int entityId;
  protected Level level;
  protected double x;
  protected double y;
  protected double z;
  protected float yRot;
  protected float xRot;
  protected float headYRot;
  protected double deltaMovementX;
  protected double deltaMovementY;
  protected double deltaMovementZ;
  protected boolean onGround;
  protected int jumpTriggerTime;
  protected boolean horizontalCollision;
  protected boolean verticalCollision;
  protected boolean verticalCollisionBelow;
  protected boolean minorHorizontalCollision;
  protected boolean isInPowderSnow;
  protected boolean wasInPowderSnow;
  protected boolean wasTouchingWater;
  protected boolean wasEyeInWater;
  public boolean noPhysics;

  public Entity(EntityType entityType, Level level) {
    this.metadataState = new EntityMetadataState(entityType);
    this.entityType = entityType;
    this.level = level;
    var bytes = Base64.getDecoder().decode(entityType.defaultEntityMetadata());
    var buf = Unpooled.wrappedBuffer(bytes);
    var helper = new MinecraftCodecHelper();
    helper.readVarInt(buf);
    for (var metadata : helper.readEntityMetadata(buf)) {
      metadataState.setMetadata(metadata);
    }
  }

  public void fromAddEntityPacket(ClientboundAddEntityPacket packet) {
    entityId(packet.getEntityId());
    uuid(packet.getUuid());
    data(packet.getData());
    setPosition(packet.getX(), packet.getY(), packet.getZ());
    setHeadRotation(packet.getHeadYaw());
    setRotation(packet.getYaw(), packet.getPitch());
    setDeltaMovement(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
  }

  public EntityMovement toMovement() {
    return new EntityMovement(Vector3d.from(x, y, z), Vector3d.from(deltaMovementX, deltaMovementY, deltaMovementZ), yRot, xRot);
  }

  public void setFrom(EntityMovement entityMovement) {
    setPosition(entityMovement.pos());
    setDeltaMovement(entityMovement.deltaMovement());
    setRotation(entityMovement.yRot(), entityMovement.xRot());
  }

  public void setPosition(Vector3d pos) {
    setPosition(pos.getX(), pos.getY(), pos.getZ());
  }

  public void setPosition(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public void addPosition(double deltaX, double deltaY, double deltaZ) {
    this.x += deltaX;
    this.y += deltaY;
    this.z += deltaZ;
  }

  public void setRotation(float yRot, float xRot) {
    this.yRot = yRot;
    this.xRot = xRot;
  }

  public void setHeadRotation(float headYRot) {
    this.headYRot = headYRot;
  }

  public void setDeltaMovement(double deltaMovementX, double deltaMovementY, double deltaMovementZ) {
    this.deltaMovementX = deltaMovementX;
    this.deltaMovementY = deltaMovementY;
    this.deltaMovementZ = deltaMovementZ;
  }

  protected boolean getSharedFlag(int flag) {
    return (this.metadataState.getMetadata(NamedEntityData.ENTITY__SHARED_FLAGS, MetadataType.BYTE) & 1 << flag) != 0;
  }

  public void tick() {
    this.baseTick();
  }

  public void baseTick() {
    this.wasInPowderSnow = this.isInPowderSnow;
    this.isInPowderSnow = false;
    // this.updateInWaterStateAndDoFluidPushing();
    // this.updateFluidOnEyes();
    // this.updateSwimming();

    // if (this.isInLava()) {
    //   this.fallDistance *= 0.5F;
    // }

    effectState.tick();
  }

  public void handleEntityEvent(EntityEvent event) {
    log.debug("Unhandled entity event for entity {}: {}", entityId, event.name());
  }

  public Vector3d originPosition(RotationOrigin origin) {
    return switch (origin) {
      case EYES -> eyePosition();
      case FEET -> pos();
    };
  }

  public boolean isEyeInFluid(TagKey<FluidType> fluid) {
    var eyePos = eyePosition();
    var breathingPos = eyePos.sub(0, BREATHING_DISTANCE_BELOW_EYES, 0);
    var breathingCoords = breathingPos.toInt();

    return level.tagsState().is(level.getBlockState(breathingCoords).fluidState().type(), fluid);
  }

  /**
   * Updates the rotation to look at a given block or location.
   *
   * @param origin   The rotation origin, either EYES or FEET.
   * @param position The block or location to look at.
   */
  public void lookAt(RotationOrigin origin, Vector3d position) {
    var originPosition = originPosition(origin);

    var dx = position.getX() - originPosition.getX();
    var dy = position.getY() - originPosition.getY();
    var dz = position.getZ() - originPosition.getZ();

    var sqr = Math.sqrt(dx * dx + dz * dz);

    this.xRot =
      MathHelper.wrapDegrees((float) (-(Math.atan2(dy, sqr) * 180.0F / (float) Math.PI)));
    this.yRot =
      MathHelper.wrapDegrees((float) (Math.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F);
  }

  public double eyeHeight() {
    return 1.62F;
  }

  public Vector3d eyePosition() {
    return Vector3d.from(x, y + eyeHeight(), z);
  }

  public Vector3i blockPos() {
    return Vector3i.from(blockX(), blockY(), blockZ());
  }

  public int blockX() {
    return MathHelper.floorDouble(x);
  }

  public int blockY() {
    return MathHelper.floorDouble(y);
  }

  public int blockZ() {
    return MathHelper.floorDouble(z);
  }

  public Vector3d pos() {
    return Vector3d.from(x, y, z);
  }

  public float width() {
    return entityType.width();
  }

  public float height() {
    return entityType.height();
  }

  public AABB boundingBox() {
    return boundingBox(x, y, z);
  }

  public AABB boundingBox(Vector3d pos) {
    return boundingBox(pos.getX(), pos.getY(), pos.getZ());
  }

  public AABB boundingBox(double x, double y, double z) {
    var w = width() / 2F;
    var h = height();
    return new AABB(x - w, y, z - w, x + w, y + h, z + w);
  }

  public double attributeValue(AttributeType type) {
    return attributeState.getOrCreateAttribute(type).calculateValue();
  }

  public final Vector3d getViewVector() {
    return this.calculateViewVector(xRot, yRot);
  }

  public final Vector3d calculateViewVector(float xRot, float yRot) {
    var h = xRot * (float) (Math.PI / 180.0);
    var i = -yRot * (float) (Math.PI / 180.0);
    var j = MathHelper.cos(i);
    var k = MathHelper.sin(i);
    var l = MathHelper.cos(h);
    var m = MathHelper.sin(h);
    return Vector3d.from(k * l, -m, (double) (j * l));
  }

  protected void setSharedFlag(int flag, boolean set) {
    byte b = this.metadataState.getMetadata(NamedEntityData.ENTITY__SHARED_FLAGS, MetadataType.BYTE);
    if (set) {
      this.metadataState.setMetadata(NamedEntityData.ENTITY__SHARED_FLAGS, MetadataType.BYTE, (byte) (b | 1 << flag));
    } else {
      this.metadataState.setMetadata(NamedEntityData.ENTITY__SHARED_FLAGS, MetadataType.BYTE, (byte) (b & ~(1 << flag)));
    }
  }

  protected Vector3d getDeltaMovement() {
    return Vector3d.from(deltaMovementX, deltaMovementY, deltaMovementZ);
  }

  public void setDeltaMovement(Vector3d motion) {
    setDeltaMovement(motion.getX(), motion.getY(), motion.getZ());
  }

  public boolean isNoGravity() {
    return this.metadataState.getMetadata(NamedEntityData.ENTITY__NO_GRAVITY, MetadataType.BOOLEAN);
  }

  protected double getDefaultGravity() {
    return 0.0;
  }

  public final double getGravity() {
    return this.isNoGravity() ? 0.0 : this.getDefaultGravity();
  }

  protected void applyGravity() {
    var d = this.getGravity();
    if (d != 0.0) {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, -d, 0.0));
    }
  }

  public Pose getPose() {
    return this.metadataState.getMetadata(NamedEntityData.ENTITY__POSE, MetadataType.POSE);
  }

  public void setPose(Pose pose) {
    this.metadataState.setMetadata(NamedEntityData.ENTITY__POSE, MetadataType.POSE, pose);
  }

  public boolean hasPose(Pose pose) {
    return this.getPose() == pose;
  }

  public boolean isShiftKeyDown() {
    return this.getSharedFlag(FLAG_SHIFT_KEY_DOWN);
  }

  public void setShiftKeyDown(boolean keyDown) {
    this.setSharedFlag(FLAG_SHIFT_KEY_DOWN, keyDown);
  }

  public boolean isSteppingCarefully() {
    return this.isShiftKeyDown();
  }

  public boolean isSuppressingBounce() {
    return this.isShiftKeyDown();
  }

  public boolean isDiscrete() {
    return this.isShiftKeyDown();
  }

  public boolean isDescending() {
    return this.isShiftKeyDown();
  }

  public boolean isCrouching() {
    return this.hasPose(Pose.SNEAKING);
  }

  public boolean isSprinting() {
    return this.getSharedFlag(FLAG_SPRINTING);
  }

  public void setSprinting(boolean sprinting) {
    this.setSharedFlag(FLAG_SPRINTING, sprinting);
  }

  public boolean isSwimming() {
    return this.getSharedFlag(FLAG_SWIMMING);
  }

  public void setSwimming(boolean swimming) {
    this.setSharedFlag(FLAG_SWIMMING, swimming);
  }

  public boolean isVisuallySwimming() {
    return this.hasPose(Pose.SWIMMING);
  }

  public boolean isVisuallyCrawling() {
    return this.isVisuallySwimming() && !this.isInWater();
  }

  public boolean isInWater() {
    return this.wasTouchingWater;
  }

  private boolean isInRain() {
    return false; // TODO
  }

  private boolean isInBubbleColumn() {
    return this.level().getBlockState(blockPos()).blockType().equals(BlockType.BUBBLE_COLUMN);
  }

  public boolean isInWaterOrRain() {
    return this.isInWater() || this.isInRain();
  }

  public boolean isInWaterRainOrBubble() {
    return this.isInWater() || this.isInRain() || this.isInBubbleColumn();
  }

  public boolean isInWaterOrBubble() {
    return this.isInWater() || this.isInBubbleColumn();
  }

  public void updateSwimming() {
    if (this.isSwimming()) {
      this.setSwimming(this.isSprinting() && this.isInWater());
    } else {
      this.setSwimming(
        this.isSprinting() && this.isUnderWater() && this.level.tagsState().is(this.level().getBlockState(blockPos()).fluidState().type(), FluidTags.WATER)
      );
    }
  }

  public boolean isUnderWater() {
    return this.wasEyeInWater && this.isInWater();
  }
}
