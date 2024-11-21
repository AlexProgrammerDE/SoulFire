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
import com.soulfiremc.server.protocol.bot.model.ChunkKey;
import com.soulfiremc.server.protocol.bot.state.EntityAttributeState;
import com.soulfiremc.server.protocol.bot.state.EntityEffectState;
import com.soulfiremc.server.protocol.bot.state.EntityMetadataState;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.EntityMovement;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.SectionUtils;
import com.soulfiremc.server.util.mcstructs.AABB;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
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
  private static final AABB INITIAL_AABB = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
  protected final EntityAttributeState attributeState = new EntityAttributeState();
  protected final EntityEffectState effectState = new EntityEffectState();
  protected final Set<TagKey<FluidType>> fluidOnEyes = new HashSet<>();
  protected Object2DoubleMap<TagKey<FluidType>> fluidHeight = new Object2DoubleArrayMap<>(2);
  protected final EntityType entityType;
  protected final EntityMetadataState metadataState;
  protected float fallDistance;
  protected UUID uuid;
  protected ObjectData data;
  protected int entityId;
  protected Level level;
  protected BlockState inBlockState = null;
  protected boolean firstTick = true;
  private Vector3d pos;
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
  private Vector3i blockPosition;
  private ChunkKey chunkPosition;
  private EntityDimensions dimensions;
  private float eyeHeight;
  private AABB bb = INITIAL_AABB;
  public boolean noPhysics;

  public Entity(EntityType entityType, Level level) {
    this.metadataState = new EntityMetadataState(entityType);
    this.entityType = entityType;
    this.level = level;
    this.dimensions = entityType.dimensions();
    this.pos = Vector3d.ZERO;
    this.blockPosition = Vector3i.ZERO;
    this.chunkPosition = ChunkKey.ZERO;
    var bytes = Base64.getDecoder().decode(entityType.defaultEntityMetadata());
    var buf = Unpooled.wrappedBuffer(bytes);
    var helper = new MinecraftCodecHelper();
    helper.readVarInt(buf);
    for (var metadata : helper.readEntityMetadata(buf)) {
      metadataState.setMetadata(metadata);
    }

    this.eyeHeight = entityType.dimensions().eyeHeight();
  }

  public void fromAddEntityPacket(ClientboundAddEntityPacket packet) {
    entityId(packet.getEntityId());
    uuid(packet.getUuid());
    data(packet.getData());
    setPos(packet.getX(), packet.getY(), packet.getZ());
    setHeadRotation(packet.getHeadYaw());
    setRotation(packet.getYaw(), packet.getPitch());
    setDeltaMovement(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
  }

  public EntityMovement toMovement() {
    return new EntityMovement(pos, Vector3d.from(deltaMovementX, deltaMovementY, deltaMovementZ), yRot, xRot);
  }

  public void setFrom(EntityMovement entityMovement) {
    setPos(entityMovement.pos());
    setDeltaMovement(entityMovement.deltaMovement());
    setRotation(entityMovement.yRot(), entityMovement.xRot());
  }

  public double x() {
    return pos.getX();
  }

  public double y() {
    return pos.getY();
  }

  public double z() {
    return pos.getZ();
  }

  public EntityDimensions getDimensions(Pose pose) {
    return entityType.dimensions();
  }

  public void setPos(Vector3d pos) {
    setPos(pos.getX(), pos.getY(), pos.getZ());
  }

  public void setPos(double x, double y, double z) {
    setPosRaw(x, y, z);
    setBoundingBox(dimensions.makeBoundingBox(pos));
  }

  public void addPos(double deltaX, double deltaY, double deltaZ) {
    setPos(pos.add(deltaX, deltaY, deltaZ));
  }

  public final void setPosRaw(double x, double y, double z) {
    if (this.pos.getX() != x || this.pos.getY() != y || this.pos.getZ() != z) {
      this.pos = Vector3d.from(x, y, z);
      var blockX = MathHelper.floor(x);
      var blockY = MathHelper.floor(y);
      var blockZ = MathHelper.floor(z);
      if (blockX != this.blockPosition.getX() || blockY != this.blockPosition.getY() || blockZ != this.blockPosition.getZ()) {
        this.blockPosition = Vector3i.from(blockX, blockY, blockZ);
        this.inBlockState = null;
        if (SectionUtils.blockToSection(blockX) != this.chunkPosition.chunkX() || SectionUtils.blockToSection(blockZ) != this.chunkPosition.chunkZ()) {
          this.chunkPosition = ChunkKey.fromBlock(this.blockPosition);
        }
      }
    }
  }

  public final AABB getBoundingBox() {
    return this.bb;
  }

  public final void setBoundingBox(AABB bb) {
    this.bb = bb;
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
    this.updateInWaterStateAndDoFluidPushing();
    this.updateFluidOnEyes();
    this.updateSwimming();

    if (this.isInLava()) {
      this.fallDistance *= 0.5F;
    }
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

  public final float eyeHeight(Pose pose) {
    return this.getDimensions(pose).eyeHeight();
  }

  public final float eyeHeight() {
    return this.eyeHeight;
  }

  public Vector3d eyePosition() {
    return pos.add(0, eyeHeight(), 0);
  }

  public Vector3i blockPos() {
    return blockPosition;
  }

  public int blockX() {
    return blockPosition.getX();
  }

  public int blockY() {
    return blockPosition.getY();
  }

  public int blockZ() {
    return blockPosition.getZ();
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

  public void refreshDimensions() {
    var currentPose = this.getPose();
    var poseDimensions = this.getDimensions(currentPose);
    this.dimensions = poseDimensions;
    this.eyeHeight = poseDimensions.eyeHeight();
    this.reapplyPosition();
  }

  protected void reapplyPosition() {
    setPos(pos);
  }

  public double getEyeY() {
    return this.y() + this.eyeHeight();
  }

  public BlockState getInBlockState() {
    if (this.inBlockState == null) {
      this.inBlockState = this.level().getBlockState(this.blockPosition());
    }

    return this.inBlockState;
  }

  public void resetFallDistance() {
    this.fallDistance = 0.0F;
  }

  public boolean isPushedByFluid() {
    return true;
  }

  protected boolean updateInWaterStateAndDoFluidPushing() {
    this.fluidHeight.clear();
    this.updateInWaterStateAndDoWaterCurrentPushing();
    var d = this.level().dimensionType().ultrawarm() ? 0.007 : 0.0023333333333333335;
    var bl = this.updateFluidHeightAndDoFluidPushing(FluidTags.LAVA, d);
    return this.isInWater() || bl;
  }

  void updateInWaterStateAndDoWaterCurrentPushing() {
    if (this.updateFluidHeightAndDoFluidPushing(FluidTags.WATER, 0.014)) {
      this.resetFallDistance();
      this.wasTouchingWater = true;
    } else {
      this.wasTouchingWater = false;
    }
  }

  public boolean touchingUnloadedChunk() {
    var lv = this.getBoundingBox().inflate(1.0);
    var i = MathHelper.floor(lv.minX);
    var j = MathHelper.ceil(lv.maxX);
    var k = MathHelper.floor(lv.minZ);
    var l = MathHelper.ceil(lv.maxZ);
    // return !this.level().hasChunksAt(i, k, j, l); // TODO
    return false;
  }

  public boolean updateFluidHeightAndDoFluidPushing(TagKey<FluidType> fluidTag, double motionScale) {
    if (this.touchingUnloadedChunk()) {
      return false;
    } else {
      var lv = this.getBoundingBox().deflate(0.001);
      var minX = MathHelper.floor(lv.minX);
      var maxX = MathHelper.ceil(lv.maxX);
      var minY = MathHelper.floor(lv.minY);
      var maxY = MathHelper.ceil(lv.maxY);
      var minZ = MathHelper.floor(lv.minZ);
      var maxZ = MathHelper.ceil(lv.maxZ);
      var height = 0.0;
      var pushedByFluid = this.isPushedByFluid();
      var bl2 = false;
      var flow = Vector3d.ZERO;
      var o = 0;
      var lv3 = Vector3i.ZERO;

      for (var x = minX; x < maxX; x++) {
        for (var y = minY; y < maxY; y++) {
          for (var z = minZ; z < maxZ; z++) {
            lv3 = Vector3i.from(x, y, z);
            var lv4 = this.level().getBlockState(lv3).fluidState();
            if (this.level().tagsState().is(lv4.type(), fluidTag)) {
              var f = (double) ((float) y + lv4.getHeight(this.level(), lv3));
              if (f >= lv.minY) {
                bl2 = true;
                height = Math.max(f - lv.minY, height);
                if (pushedByFluid) {
                  var lv5 = lv4.getFlow(this.level(), lv3);
                  if (height < 0.4) {
                    lv5 = lv5.mul(height);
                  }

                  flow = flow.add(lv5);
                  o++;
                }
              }
            }
          }
        }
      }

      if (flow.length() > 0.0) {
        if (o > 0) {
          flow = flow.mul(1.0 / (double) o);
        }

        if (!(this instanceof Player)) {
          flow = flow.normalize();
        }

        var deltaMovement = this.getDeltaMovement();
        flow = flow.mul(motionScale);
        var g = 0.003;
        if (Math.abs(deltaMovement.getX()) < 0.003 && Math.abs(deltaMovement.getZ()) < 0.003 && flow.length() < 0.0045000000000000005) {
          flow = flow.normalize().mul(0.0045000000000000005);
        }

        this.setDeltaMovement(this.getDeltaMovement().add(flow));
      }

      this.fluidHeight.put(fluidTag, height);
      return bl2;
    }
  }

  private void updateFluidOnEyes() {
    this.wasEyeInWater = this.isEyeInFluid(FluidTags.WATER);
    this.fluidOnEyes.clear();
    var eyeY = this.getEyeY();

    var blockPos = Vector3i.from(this.x(), eyeY, this.z());
    var fluidState = this.level().getBlockState(blockPos).fluidState();
    var fluidHeight = (double) ((float) blockPos.getY() + fluidState.getHeight(this.level(), blockPos));
    if (fluidHeight > eyeY) {
      this.fluidOnEyes.addAll(this.level.tagsState().getTags(fluidState.type()));
    }
  }

  public boolean isEyeInFluid(TagKey<FluidType> fluidTag) {
    return this.fluidOnEyes.contains(fluidTag);
  }

  public boolean isInLava() {
    return !this.firstTick && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
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
