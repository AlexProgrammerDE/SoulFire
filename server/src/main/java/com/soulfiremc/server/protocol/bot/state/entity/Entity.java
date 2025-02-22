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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.soulfiremc.server.data.*;
import com.soulfiremc.server.protocol.bot.model.ChunkKey;
import com.soulfiremc.server.protocol.bot.state.EntityAttributeState;
import com.soulfiremc.server.protocol.bot.state.EntityEffectState;
import com.soulfiremc.server.protocol.bot.state.EntityMetadataState;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.SectionUtils;
import com.soulfiremc.server.util.VectorHelper;
import com.soulfiremc.server.util.mcstructs.AABB;
import com.soulfiremc.server.util.mcstructs.Direction;
import com.soulfiremc.server.util.mcstructs.MoverType;
import com.soulfiremc.server.util.mcstructs.VecDeltaCodec;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.RotationOrigin;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.ObjectData;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
@Getter
public class Entity {
  protected static final int FLAG_ONFIRE = 0;
  protected static final int FLAG_GLOWING = 6;
  protected static final int FLAG_FALL_FLYING = 7;
  private static final int FLAG_SHIFT_KEY_DOWN = 1;
  private static final int FLAG_SPRINTING = 3;
  private static final int FLAG_SWIMMING = 4;
  private static final int FLAG_INVISIBLE = 5;
  private static final AABB INITIAL_AABB = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
  protected final EntityAttributeState attributeState = new EntityAttributeState();
  protected final EntityEffectState effectState = new EntityEffectState();
  protected final Set<TagKey<FluidType>> fluidOnEyes = new HashSet<>();
  protected final EntityType entityType;
  protected final EntityMetadataState metadataState;
  private final VecDeltaCodec packetPositionCodec = new VecDeltaCodec();
  private final List<Entity.Movement> movementThisTick = new ArrayList<>();
  private final Set<BlockState> blocksInside = new ReferenceArraySet<>();
  private final LongSet visitedBlocks = new LongOpenHashSet();
  private final int remainingFireTicks = -this.getFireImmuneTicks();
  private final ImmutableList<Entity> passengers = ImmutableList.of();
  public int invulnerableTime;
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public Optional<Vector3i> mainSupportingBlockPos = Optional.empty();
  public double xo;
  public double yo;
  public double zo;
  public double xOld;
  public double yOld;
  public double zOld;
  public float yRotO;
  public float xRotO;
  public boolean noPhysics;
  public boolean hasImpulse;
  protected Object2DoubleMap<TagKey<FluidType>> fluidHeight = new Object2DoubleArrayMap<>(2);
  protected float fallDistance;
  @Setter
  protected UUID uuid = UUID.randomUUID();
  @Setter
  protected ObjectData data;
  @Setter
  protected int entityId;
  @Setter
  protected Level level;
  protected BlockState inBlockState = null;
  protected boolean firstTick = true;
  protected Vector3d deltaMovement = Vector3d.ZERO;
  protected float yRot;
  protected float xRot;
  protected float headYRot;
  protected boolean onGround;
  protected boolean horizontalCollision;
  protected boolean verticalCollision;
  protected boolean verticalCollisionBelow;
  protected boolean minorHorizontalCollision;
  protected boolean wasInPowderSnow;
  protected boolean wasTouchingWater;
  protected boolean wasEyeInWater;
  @Setter
  protected boolean isInPowderSnow;
  protected Vector3d stuckSpeedMultiplier = Vector3d.ZERO;
  private boolean onGroundNoBlocks = false;
  private Vector3d pos;
  private Vector3i blockPosition;
  private ChunkKey chunkPosition;
  private EntityDimensions dimensions;
  private float eyeHeight;
  private AABB bb = INITIAL_AABB;
  private int portalCooldown;
  @Nullable
  private Entity vehicle;

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
    MinecraftTypes.readVarInt(buf);
    for (var metadata : MinecraftTypes.readEntityMetadata(buf)) {
      metadataState.setMetadata(metadata);
    }

    this.setPos(0.0, 0.0, 0.0);
    this.eyeHeight = entityType.dimensions().eyeHeight();
  }

  private static float[] collectCandidateStepUpHeights(AABB box, List<AABB> colliders, float deltaY, float maxUpStep) {
    FloatSet floatSet = new FloatArraySet(4);

    for (var collider : colliders) {
      var doubleList = collider.getCoords(Direction.Axis.Y);
      for (double coord : doubleList) {
        var h = (float) (coord - box.minY);
        if (!(h < 0.0F) && h != maxUpStep) {
          if (h > deltaY) {
            break;
          }

          floatSet.add(h);
        }
      }
    }

    var fs = floatSet.toFloatArray();
    FloatArrays.unstableSort(fs);
    return fs;
  }

  static Iterable<Vector3i> boxTraverseBlocks(Vector3d from, Vector3d to, AABB bb) {
    var length = to.sub(from);
    var iterable = betweenClosed(bb);
    if (length.lengthSquared() < (double) MathHelper.square(0.99999F)) {
      return iterable;
    } else {
      Set<Vector3i> set = new ObjectLinkedOpenHashSet<>();
      var normalizedLength = length.normalize().mul(1.0E-7);
      var offsetMin = bb.getMinPosition().add(normalizedLength);
      var lv4 = bb.getMinPosition().sub(length).sub(normalizedLength);
      addCollisionsAlongTravel(set, lv4, offsetMin, bb);

      for (var pos : iterable) {
        set.add(pos);
      }

      return set;
    }
  }

  private static void addCollisionsAlongTravel(Set<Vector3i> set, Vector3d from, Vector3d to, AABB bb) {
    var length = to.sub(from);
    var startX = MathHelper.floor(from.getX());
    var startY = MathHelper.floor(from.getY());
    var startZ = MathHelper.floor(from.getZ());
    var l = MathHelper.sign(length.getX());
    var m = MathHelper.sign(length.getY());
    var n = MathHelper.sign(length.getZ());
    var d = l == 0 ? Double.MAX_VALUE : (double) l / length.getX();
    var e = m == 0 ? Double.MAX_VALUE : (double) m / length.getY();
    var f = n == 0 ? Double.MAX_VALUE : (double) n / length.getZ();
    var g = d * (l > 0 ? 1.0 - MathHelper.frac(from.getX()) : MathHelper.frac(from.getX()));
    var h = e * (m > 0 ? 1.0 - MathHelper.frac(from.getY()) : MathHelper.frac(from.getY()));
    var o = f * (n > 0 ? 1.0 - MathHelper.frac(from.getZ()) : MathHelper.frac(from.getZ()));
    var p = 0;

    while (g <= 1.0 || h <= 1.0 || o <= 1.0) {
      if (g < h) {
        if (g < o) {
          startX += l;
          g += d;
        } else {
          startZ += n;
          o += f;
        }
      } else if (h < o) {
        startY += m;
        h += e;
      } else {
        startZ += n;
        o += f;
      }

      if (p++ > 16) {
        break;
      }

      var optional = AABB.clip(startX, startY, startZ, startX + 1, startY + 1, startZ + 1, from, to);
      if (optional.isPresent()) {
        var lv2 = optional.get();
        var xClamp = MathHelper.clamp(lv2.getX(), (double) startX + 1.0E-5F, (double) startX + 1.0 - 1.0E-5F);
        var yClamp = MathHelper.clamp(lv2.getY(), (double) startY + 1.0E-5F, (double) startY + 1.0 - 1.0E-5F);
        var zClamp = MathHelper.clamp(lv2.getZ(), (double) startZ + 1.0E-5F, (double) startZ + 1.0 - 1.0E-5F);
        var endX = MathHelper.floor(xClamp + bb.getXsize());
        var endY = MathHelper.floor(yClamp + bb.getYsize());
        var endZ = MathHelper.floor(zClamp + bb.getZsize());

        for (var x = startX; x <= endX; x++) {
          for (var y = startY; y <= endY; y++) {
            for (var z = startZ; z <= endZ; z++) {
              set.add(Vector3i.from(x, y, z));
            }
          }
        }
      }
    }
  }

  public static Iterable<Vector3i> betweenClosed(AABB arg) {
    var lv = Vector3i.from(arg.minX, arg.minY, arg.minZ);
    var lv2 = Vector3i.from(arg.maxX, arg.maxY, arg.maxZ);
    return betweenClosed(lv, lv2);
  }

  public static Iterable<Vector3i> betweenClosed(Vector3i firstPos, Vector3i secondPos) {
    return betweenClosed(
      Math.min(firstPos.getX(), secondPos.getX()),
      Math.min(firstPos.getY(), secondPos.getY()),
      Math.min(firstPos.getZ(), secondPos.getZ()),
      Math.max(firstPos.getX(), secondPos.getX()),
      Math.max(firstPos.getY(), secondPos.getY()),
      Math.max(firstPos.getZ(), secondPos.getZ())
    );
  }

  public static Iterable<Vector3i> betweenClosed(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    var o = maxX - minX + 1;
    var p = maxY - minY + 1;
    var q = maxZ - minZ + 1;
    var r = o * p * q;
    return () -> new AbstractIterator<>() {
      private int index;

      protected Vector3i computeNext() {
        if (this.index == r) {
          return this.endOfData();
        } else {
          var i = this.index % o;
          var j = this.index / o;
          var k = j % p;
          var l = j / p;
          this.index++;
          return Vector3i.from(minX + i, minY + k, minZ + l);
        }
      }
    };
  }

  public static Vector3d collideBoundingBox(@Nullable Entity entity, Vector3d vec, AABB collisionBox, Level level, List<AABB> potentialHits) {
    var list2 = collectColliders(entity, level, potentialHits, collisionBox.expandTowards(vec));
    return collideWithShapes(vec, collisionBox, list2);
  }

  private static List<AABB> collectColliders(@Nullable Entity entity, Level level, List<AABB> collisions, AABB boundingBox) {
    ImmutableList.Builder<AABB> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 1);
    if (!collisions.isEmpty()) {
      builder.addAll(collisions);
    }

    // TODO: WorldBorder
    // WorldBorder lv = level.getWorldBorder();
    // boolean bl = entity != null && lv.isInsideCloseToBorder(entity, boundingBox);
    // if (bl) {
    //   builder.add(lv.getCollisionShape());
    // }

    builder.addAll(level.getBlockCollisionBoxes(boundingBox));
    return builder.build();
  }

  public static double collide(Direction.Axis movementAxis, AABB collisionBox, Iterable<AABB> possibleHits, double desiredOffset) {
    for (var hit : possibleHits) {
      if (Math.abs(desiredOffset) < 1.0E-7) {
        return 0.0;
      }

      desiredOffset = hit.collide(movementAxis, collisionBox, desiredOffset);
    }

    return desiredOffset;
  }

  private static Vector3d collideWithShapes(Vector3d deltaMovement, AABB entityBB, List<AABB> shapes) {
    if (shapes.isEmpty()) {
      return deltaMovement;
    } else {
      var x = deltaMovement.getX();
      var y = deltaMovement.getY();
      var z = deltaMovement.getZ();
      if (y != 0.0) {
        y = collide(Direction.Axis.Y, entityBB, shapes, y);
        if (y != 0.0) {
          entityBB = entityBB.move(0.0, y, 0.0);
        }
      }

      var bl = Math.abs(x) < Math.abs(z);
      if (bl && z != 0.0) {
        z = collide(Direction.Axis.Z, entityBB, shapes, z);
        if (z != 0.0) {
          entityBB = entityBB.move(0.0, 0.0, z);
        }
      }

      if (x != 0.0) {
        x = collide(Direction.Axis.X, entityBB, shapes, x);
        if (!bl && x != 0.0) {
          entityBB = entityBB.move(x, 0.0, 0.0);
        }
      }

      if (!bl && z != 0.0) {
        z = collide(Direction.Axis.Z, entityBB, shapes, z);
      }

      return Vector3d.from(x, y, z);
    }
  }

  protected static Vector3d getInputVector(Vector3d relative, float motionScale, float facing) {
    var d = relative.lengthSquared();
    if (d < 1.0E-7) {
      return Vector3d.ZERO;
    } else {
      var scaledMotion = (d > 1.0 ? relative.normalize() : relative).mul((double) motionScale);
      var h = MathHelper.sin(facing * (float) (Math.PI / 180.0));
      var i = MathHelper.cos(facing * (float) (Math.PI / 180.0));
      return Vector3d.from(scaledMotion.getX() * (double) i - scaledMotion.getZ() * (double) h, scaledMotion.getY(), scaledMotion.getZ() * (double) i + scaledMotion.getX() * (double) h);
    }
  }

  public void fromAddEntityPacket(ClientboundAddEntityPacket packet) {
    var x = packet.getX();
    var y = packet.getY();
    var z = packet.getZ();
    this.syncPacketPositionCodec(x, y, z);
    this.moveTo(x, y, z, packet.getYaw(), packet.getPitch());
    entityId(packet.getEntityId());
    uuid(packet.getUuid());
    data(packet.getData());
  }

  public void syncPacketPositionCodec(double x, double y, double z) {
    this.packetPositionCodec.base(Vector3d.from(x, y, z));
  }

  public VecDeltaCodec getPositionCodec() {
    return this.packetPositionCodec;
  }

  public void moveTo(double x, double y, double z, float yRot, float xRot) {
    this.setPosRaw(x, y, z);
    this.setYRot(yRot);
    this.setXRot(xRot);
    this.setOldPosAndRot();
    this.reapplyPosition();
  }

  public Vector3d getKnownMovement() {
    return this.getDeltaMovement();
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

  public double lerpTargetX() {
    return this.x();
  }

  public double lerpTargetY() {
    return this.y();
  }

  public double lerpTargetZ() {
    return this.z();
  }

  public float lerpTargetXRot() {
    return this.xRot();
  }

  public float lerpTargetYRot() {
    return this.yRot();
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

  protected void lerpPositionAndRotationStep(int steps, double targetX, double targetY, double targetZ, double targetYRot, double targetXRot) {
    var partialStep = 1.0 / (double) steps;
    var xLerp = MathHelper.lerp(partialStep, this.x(), targetX);
    var yLerp = MathHelper.lerp(partialStep, this.y(), targetY);
    var zLerp = MathHelper.lerp(partialStep, this.z(), targetZ);
    var yRotLerp = (float) MathHelper.rotLerp(partialStep, this.yRot(), targetYRot);
    var xRotLerp = (float) MathHelper.lerp(partialStep, this.xRot(), targetXRot);
    this.setPos(xLerp, yLerp, zLerp);
    this.setRot(yRotLerp, xRotLerp);
  }

  public void lerpMotion(double x, double y, double z) {
    this.setDeltaMovement(x, y, z);
  }

  public void cancelLerp() {
  }

  public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    this.setPos(x, y, z);
    this.setRot(yRot, xRot);
  }

  public void setOnGround(boolean onGround) {
    this.onGround = onGround;
    this.checkSupportingBlock(onGround, null);
  }

  public void setOnGroundWithMovement(boolean onGround, boolean horizontalCollision, Vector3d arg) {
    this.onGround = onGround;
    this.horizontalCollision = horizontalCollision;
    this.checkSupportingBlock(onGround, arg);
  }

  public boolean isSupportedBy(Vector3i pos) {
    return this.mainSupportingBlockPos.isPresent() && this.mainSupportingBlockPos.get().equals(pos);
  }

  protected void checkSupportingBlock(boolean onGround, @Nullable Vector3d movement) {
    if (onGround) {
      var boundingBox = this.getBoundingBox();
      var checkBoundingBox = new AABB(boundingBox.minX, boundingBox.minY - 1.0E-6, boundingBox.minZ, boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
      var optional = this.level.findSupportingBlock(this, checkBoundingBox);
      if (optional.isPresent() || this.onGroundNoBlocks) {
        this.mainSupportingBlockPos = optional;
      } else if (movement != null) {
        var movedBoundingBox = checkBoundingBox.move(-movement.getX(), 0.0, -movement.getZ());
        optional = this.level.findSupportingBlock(this, movedBoundingBox);
        this.mainSupportingBlockPos = optional;
      }

      this.onGroundNoBlocks = optional.isEmpty();
    } else {
      this.onGroundNoBlocks = false;
      if (this.mainSupportingBlockPos.isPresent()) {
        this.mainSupportingBlockPos = Optional.empty();
      }
    }
  }

  public boolean fireImmune() {
    return this.entityType.fireImmune();
  }

  public boolean isCurrentlyGlowing() {
    return this.getSharedFlag(FLAG_GLOWING);
  }

  public boolean isInvisible() {
    return this.getSharedFlag(FLAG_INVISIBLE);
  }

  public boolean onGround() {
    return this.onGround;
  }

  public final AABB getBoundingBox() {
    return this.bb;
  }

  public final void setBoundingBox(AABB bb) {
    this.bb = bb;
  }

  public void setRot(float yRot, float xRot) {
    this.setYRot(yRot % 360.0F);
    this.setXRot(xRot % 360.0F);
  }

  public void setYRot(float yRot) {
    if (!Float.isFinite(yRot)) {
      log.warn("Invalid entity y rotation: {}, discarding.", yRot);
    } else {
      this.yRot = yRot;
    }
  }

  public void setXRot(float xRot) {
    if (!Float.isFinite(xRot)) {
      log.warn("Invalid entity x rotation: {}, discarding.", xRot);
    } else {
      this.xRot = Math.clamp(xRot % 360.0F, -90.0F, 90.0F);
    }
  }

  public void setHeadRotation(float headYRot) {
    this.headYRot = headYRot;
  }

  public void setDeltaMovement(double deltaMovementX, double deltaMovementY, double deltaMovementZ) {
    this.setDeltaMovement(Vector3d.from(deltaMovementX, deltaMovementY, deltaMovementZ));
  }

  public void addDeltaMovement(Vector3d addend) {
    this.setDeltaMovement(this.getDeltaMovement().add(addend));
  }

  protected Vector3d getDeltaMovement() {
    return deltaMovement;
  }

  public void setDeltaMovement(Vector3d deltaMovement) {
    this.deltaMovement = deltaMovement;
  }

  protected boolean getSharedFlag(int flag) {
    return (this.metadataState.getMetadata(NamedEntityData.ENTITY__SHARED_FLAGS, MetadataTypes.BYTE) & 1 << flag) != 0;
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
    log.trace("Unhandled entity event for entity {}: {}", entityId, event.name());
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

  public double attributeValue(AttributeType type) {
    return attributeState.getOrCreateAttribute(type).calculateValue();
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

  public final void setOldPosAndRot() {
    this.setOldPos();
    this.setOldRot();
  }

  public final void setOldPosAndRot(Vector3d vec, float f, float g) {
    this.setOldPos(vec);
    this.setOldRot(f, g);
  }

  protected void setOldPos() {
    this.setOldPos(this.pos);
  }

  public void setOldRot() {
    this.setOldRot(this.yRot(), this.xRot());
  }

  private void setOldPos(Vector3d vec) {
    this.xo = this.xOld = vec.getX();
    this.yo = this.yOld = vec.getY();
    this.zo = this.zOld = vec.getZ();
  }

  private void setOldRot(float f, float g) {
    this.yRotO = f;
    this.xRotO = g;
  }

  public final Vector3d oldPosition() {
    return Vector3d.from(this.xOld, this.yOld, this.zOld);
  }

  public boolean isPushable() {
    return false;
  }

  public void push(Entity entity) {
    if (!entity.noPhysics && !this.noPhysics) {
      var d = entity.x() - this.x();
      var e = entity.z() - this.z();
      var f = MathHelper.absMax(d, e);
      if (f >= 0.01F) {
        f = Math.sqrt(f);
        d /= f;
        e /= f;
        var g = 1.0 / f;
        if (g > 1.0) {
          g = 1.0;
        }

        d *= g;
        e *= g;
        d *= 0.05F;
        e *= 0.05F;
        if (this.isPushable()) {
          this.push(-d, 0.0, -e);
        }

        if (entity.isPushable()) {
          entity.push(d, 0.0, e);
        }
      }
    }
  }

  public void push(double x, double y, double z) {
    this.setDeltaMovement(this.getDeltaMovement().add(x, y, z));
    this.hasImpulse = true;
  }

  public void move(MoverType type, Vector3d pos) {
    if (this.noPhysics) {
      this.setPos(this.x() + pos.getX(), this.y() + pos.getY(), this.z() + pos.getZ());
    } else {
      if (type == MoverType.PISTON) {
        throw new IllegalArgumentException("Invalid move type " + type);
      }

      if (this.stuckSpeedMultiplier.lengthSquared() > 1.0E-7) {
        pos = pos.mul(this.stuckSpeedMultiplier);
        this.stuckSpeedMultiplier = Vector3d.ZERO;
        this.setDeltaMovement(Vector3d.ZERO);
      }

      pos = this.maybeBackOffFromEdge(pos, type);
      var collideOffset = this.collide(pos);
      var d = collideOffset.lengthSquared();
      if (d > 1.0E-7 || pos.lengthSquared() - d < 1.0E-7) {
        if (this.fallDistance != 0.0F && d >= 1.0) {
          var touchedPositions = this.level().getTouchedPositions(new AABB(this.pos(), this.pos().add(collideOffset)));
          for (var touchedPos : touchedPositions) {
            var blockState = this.level().getBlockState(touchedPos);
            if (!blockState.fluidState().empty() || this.level().tagsState().is(blockState.blockType(), BlockTags.FALL_DAMAGE_RESETTING)) {
              this.resetFallDistance();
              break;
            }
          }
        }

        this.setPos(this.x() + collideOffset.getX(), this.y() + collideOffset.getY(), this.z() + collideOffset.getZ());
      }

      var xCollision = !MathHelper.equal(pos.getX(), collideOffset.getX());
      var zCollision = !MathHelper.equal(pos.getZ(), collideOffset.getZ());
      this.horizontalCollision = xCollision || zCollision;
      this.verticalCollision = pos.getY() != collideOffset.getY();
      this.verticalCollisionBelow = this.verticalCollision && pos.getY() < 0.0;
      if (this.horizontalCollision) {
        this.minorHorizontalCollision = this.isHorizontalCollisionMinor(collideOffset);
      } else {
        this.minorHorizontalCollision = false;
      }

      this.setOnGroundWithMovement(this.verticalCollisionBelow, this.horizontalCollision, collideOffset);
      var onPos = this.getOnPosLegacy();
      var onBlockState = this.level().getBlockState(onPos);
      if (this.isControlledByLocalInstance() && !this.isControlledByClient()) {
        this.checkFallDamage(collideOffset.getY(), this.onGround(), onBlockState, onPos);
      }

      if (this.horizontalCollision) {
        var currentDeltaMovement = this.getDeltaMovement();
        this.setDeltaMovement(xCollision ? 0.0 : currentDeltaMovement.getX(), currentDeltaMovement.getY(), zCollision ? 0.0 : currentDeltaMovement.getZ());
      }

      if (this.isControlledByLocalInstance()) {
        if (pos.getY() != collideOffset.getY()) {
          BlockBehaviour.updateEntityMovementAfterFallOn(onBlockState.blockType(), this);
        }
      }

      var factor = this.getBlockSpeedFactor();
      this.setDeltaMovement(this.getDeltaMovement().mul(factor, 1.0, factor));
    }
  }

  private Vector3d collide(Vector3d vec) {
    var bb = this.getBoundingBox();
    var list = this.level().getEntityCollisions(this, bb.expandTowards(vec));
    var collidedBB = vec.lengthSquared() == 0.0 ? vec : collideBoundingBox(this, vec, bb, this.level(), list);
    var xDiff = vec.getX() != collidedBB.getX();
    var yDiff = vec.getY() != collidedBB.getY();
    var zDiff = vec.getZ() != collidedBB.getZ();
    var bl4 = yDiff && vec.getY() < 0.0;
    if (this.maxUpStep() > 0.0F && (bl4 || this.onGround()) && (xDiff || zDiff)) {
      var lv3 = bl4 ? bb.move(0.0, collidedBB.getY(), 0.0) : bb;
      var lv4 = lv3.expandTowards(vec.getX(), this.maxUpStep(), vec.getZ());
      if (!bl4) {
        lv4 = lv4.expandTowards(0.0, -1.0E-5F, 0.0);
      }

      var list2 = collectColliders(this, this.level, list, lv4);
      var f = (float) collidedBB.getY();
      var stepUpHeights = collectCandidateStepUpHeights(lv3, list2, this.maxUpStep(), f);

      for (var stepUpHeight : stepUpHeights) {
        var collidedShapes = collideWithShapes(Vector3d.from(vec.getX(), stepUpHeight, vec.getZ()), lv3, list2);
        if (VectorHelper.horizontalDistanceSqr(collidedShapes) > VectorHelper.horizontalDistanceSqr(collidedBB)) {
          var d = bb.minY - lv3.minY;
          return collidedShapes.add(0.0, -d, 0.0);
        }
      }
    }

    return collidedBB;
  }

  public void applyEffectsFromBlocks() {
    this.applyEffectsFromBlocks(this.oldPosition(), this.pos);
  }

  public void applyEffectsFromBlocks(Vector3d from, Vector3d to) {
    if (this.isAffectedByBlocks()) {
      if (this.onGround()) {
        var onPos = this.getOnPosLegacy();
        var onBlockState = this.level().getBlockState(onPos);
        BlockBehaviour.stepOn(onBlockState.blockType(), this);
      }

      this.movementThisTick.add(new Entity.Movement(from, to));
      this.checkInsideBlocks(this.movementThisTick, this.blocksInside);
      this.movementThisTick.clear();
      this.blocksInside.clear();
    }
  }

  private void checkInsideBlocks(List<Entity.Movement> list, Set<BlockState> set) {
    if (this.isAffectedByBlocks()) {
      var bb = this.getBoundingBox().deflate(1.0E-5F);
      var longSet = this.visitedBlocks;

      for (var movement : list) {
        var from = movement.from();
        var to = movement.to();

        for (var pos : boxTraverseBlocks(from, to, bb)) {
          if (!this.isAlive()) {
            return;
          }

          var blockState = this.level().getBlockState(pos);
          if (!blockState.blockType().air() && longSet.add(VectorHelper.asLong(pos))) {
            BlockBehaviour.onInsideBlock(pos, blockState, this);

            set.add(blockState);
          }
        }
      }

      longSet.clear();
    }
  }

  public void onAboveBubbleCol(boolean downwards) {
    var deltaMovement = this.getDeltaMovement();
    double d;
    if (downwards) {
      d = Math.max(-0.9, deltaMovement.getY() - 0.03);
    } else {
      d = Math.min(1.8, deltaMovement.getY() + 0.1);
    }

    this.setDeltaMovement(deltaMovement.getX(), d, deltaMovement.getZ());
  }

  public void onInsideBubbleColumn(boolean downwards) {
    var deltaMovement = this.getDeltaMovement();
    double d;
    if (downwards) {
      d = Math.max(-0.3, deltaMovement.getY() - 0.03);
    } else {
      d = Math.min(0.7, deltaMovement.getY() + 0.06);
    }

    this.setDeltaMovement(deltaMovement.getX(), d, deltaMovement.getZ());
    this.resetFallDistance();
  }

  public boolean isAlive() {
    return true;
  }

  protected boolean isAffectedByBlocks() {
    return !this.noPhysics;
  }

  public boolean isFree(double x, double y, double z) {
    return this.isFree(this.getBoundingBox().move(x, y, z));
  }

  private boolean isFree(AABB box) {
    return this.level().noCollision(box) && !this.level().containsAnyLiquid(box);
  }

  protected Vector3d maybeBackOffFromEdge(Vector3d vec, MoverType mover) {
    return vec;
  }

  protected void setSharedFlag(int flag, boolean set) {
    byte b = this.metadataState.getMetadata(NamedEntityData.ENTITY__SHARED_FLAGS, MetadataTypes.BYTE);
    if (set) {
      this.metadataState.setMetadata(NamedEntityData.ENTITY__SHARED_FLAGS, MetadataTypes.BYTE, ByteEntityMetadata::new, (byte) (b | 1 << flag));
    } else {
      this.metadataState.setMetadata(NamedEntityData.ENTITY__SHARED_FLAGS, MetadataTypes.BYTE, ByteEntityMetadata::new, (byte) (b & ~(1 << flag)));
    }
  }

  public boolean isNoGravity() {
    return this.metadataState.getMetadata(NamedEntityData.ENTITY__NO_GRAVITY, MetadataTypes.BOOLEAN);
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
    return this.metadataState.getMetadata(NamedEntityData.ENTITY__POSE, MetadataTypes.POSE);
  }

  public void setPose(Pose pose) {
    this.metadataState.setMetadata(NamedEntityData.ENTITY__POSE, MetadataTypes.POSE, ObjectEntityMetadata::new, pose);
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

  public void checkSlowFallDistance() {
    if (this.getDeltaMovement().getY() > -0.5 && this.fallDistance > 1.0F) {
      this.fallDistance = 1.0F;
    }
  }

  public void resetFallDistance() {
    this.fallDistance = 0.0F;
  }

  public boolean isPushedByFluid() {
    return true;
  }

  protected void checkFallDamage(double y, boolean onGround, BlockState state, Vector3i pos) {
    if (onGround) {
      this.resetFallDistance();
    } else if (y < 0.0) {
      this.fallDistance -= (float) y;
    }
  }

  void updateInWaterStateAndDoWaterCurrentPushing() {
    if (this.updateFluidHeightAndDoFluidPushing(FluidTags.WATER, 0.014)) {
      this.resetFallDistance();
      this.wasTouchingWater = true;
    } else {
      this.wasTouchingWater = false;
    }
  }

  protected boolean isHorizontalCollisionMinor(Vector3d deltaMovement) {
    return false;
  }

  public Vector3i getBlockPosBelowThatAffectsMyMovement() {
    return this.getOnPos(0.500001F);
  }

  public Vector3i getOnPosLegacy() {
    return this.getOnPos(0.2F);
  }

  public Vector3i getOnPos() {
    return this.getOnPos(1.0E-5F);
  }

  protected Vector3i getOnPos(float yOffset) {
    if (this.mainSupportingBlockPos.isPresent()) {
      var supportingBlockPos = this.mainSupportingBlockPos.get();
      if (!(yOffset > 1.0E-5F)) {
        return supportingBlockPos;
      } else {
        var blockState = this.level().getBlockState(supportingBlockPos);
        return (!((double) yOffset <= 0.5) || !this.level().tagsState().is(blockState.blockType(), BlockTags.FENCES))
          && !this.level().tagsState().is(blockState.blockType(), BlockTags.WALLS)
          && !(blockState.blockType().fenceGateBlock())
          ? Vector3i.from(supportingBlockPos.getX(), MathHelper.floor(this.pos.getY() - (double) yOffset), supportingBlockPos.getZ())
          : supportingBlockPos;
      }
    } else {
      var x = MathHelper.floor(this.pos.getX());
      var y = MathHelper.floor(this.pos.getY() - (double) yOffset);
      var z = MathHelper.floor(this.pos.getZ());
      return Vector3i.from(x, y, z);
    }
  }

  protected float getBlockJumpFactor() {
    var f = this.level().getBlockState(this.blockPosition()).blockType().jumpFactor();
    var g = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).blockType().jumpFactor();
    return (double) f == 1.0 ? g : f;
  }

  protected float getBlockSpeedFactor() {
    var blockType = this.level().getBlockState(this.blockPosition()).blockType();
    var speedFactor = blockType.speedFactor();
    if (blockType != BlockType.WATER && blockType != BlockType.BUBBLE_COLUMN) {
      return (double) speedFactor == 1.0 ? this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).blockType().speedFactor() : speedFactor;
    } else {
      return speedFactor;
    }
  }

  public boolean touchingUnloadedChunk() {
    var bb = this.getBoundingBox().inflate(1.0);
    var minX = MathHelper.floor(bb.minX);
    var maxX = MathHelper.ceil(bb.maxX);
    var minZ = MathHelper.floor(bb.minZ);
    var maxZ = MathHelper.ceil(bb.maxZ);
    return !this.level.chunks().hasChunksAt(minX, minZ, maxX, maxZ);
  }

  public boolean canCollideWith(Entity entity) {
    return entity.canBeCollidedWith();
  }

  public boolean canBeCollidedWith() {
    return false;
  }

  protected void updateInWaterStateAndDoFluidPushing() {
    this.fluidHeight.clear();
    this.updateInWaterStateAndDoWaterCurrentPushing();
    var d = this.level().dimensionType().ultrawarm() ? 0.007 : 0.0023333333333333335;
    this.updateFluidHeightAndDoFluidPushing(FluidTags.LAVA, d);
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

  public boolean isEffectiveAi() {
    return false;
  }

  public double getFluidHeight(TagKey<FluidType> fluidTag) {
    return this.fluidHeight.getDouble(fluidTag);
  }

  public double getFluidJumpThreshold() {
    return (double) this.eyeHeight() < 0.4 ? 0.0 : 0.4;
  }

  public boolean isControlledByLocalInstance() {
    return this.getControllingPassenger() instanceof Player player ? player.isLocalPlayer() : this.isEffectiveAi();
  }

  public boolean isControlledByClient() {
    var passenger = this.getControllingPassenger();
    return passenger != null && passenger.isControlledByClient();
  }

  public int getMaxAirSupply() {
    return 300;
  }

  public int getTicksFrozen() {
    return this.metadataState.getMetadata(NamedEntityData.ENTITY__TICKS_FROZEN, MetadataTypes.INT);
  }

  public float getPercentFrozen() {
    var i = this.getTicksRequiredToFreeze();
    return (float) Math.min(this.getTicksFrozen(), i) / (float) i;
  }

  public boolean isFullyFrozen() {
    return this.getTicksFrozen() >= this.getTicksRequiredToFreeze();
  }

  public int getTicksRequiredToFreeze() {
    return 140;
  }

  protected BlockState getBlockStateOnLegacy() {
    return this.level().getBlockState(this.getOnPosLegacy());
  }

  public final float getBbWidth() {
    return this.dimensions.width();
  }

  public final float getBbHeight() {
    return this.dimensions.height();
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
                  var flowDirection = lv4.getFlow(this.level(), lv3);
                  if (height < 0.4) {
                    flowDirection = flowDirection.mul(height);
                  }

                  flow = flow.add(flowDirection);
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
          flow = VectorHelper.normalizeSafe(flow);
        }

        var deltaMovement = this.getDeltaMovement();
        flow = flow.mul(motionScale);
        var g = 0.003;
        if (Math.abs(deltaMovement.getX()) < g && Math.abs(deltaMovement.getZ()) < g && flow.length() < 0.0045000000000000005) {
          flow = VectorHelper.normalizeSafe(flow).mul(0.0045000000000000005);
        }

        this.setDeltaMovement(this.getDeltaMovement().add(flow));
      }

      this.fluidHeight.put(fluidTag, height);
      return bl2;
    }
  }

  public Vector3d getLookAngle() {
    return this.calculateViewVector(this.xRot(), this.yRot());
  }

  public boolean canSprint() {
    return false;
  }

  public float maxUpStep() {
    return 0.0F;
  }

  public void moveRelative(float amount, Vector3d relative) {
    var inputVector = getInputVector(relative, amount, this.yRot());
    this.setDeltaMovement(this.getDeltaMovement().add(inputVector));
  }

  public void makeStuckInBlock(Vector3d motionMultiplier) {
    this.resetFallDistance();
    this.stuckSpeedMultiplier = motionMultiplier;
  }

  public final boolean isRemoved() {
    return false;
  }

  public void playerTouch(Player player) {
  }

  public boolean isSpectator() {
    return false;
  }

  protected int getFireImmuneTicks() {
    return 1;
  }

  @Nullable
  public LivingEntity getControllingPassenger() {
    return null;
  }

  @Nullable
  public Entity getVehicle() {
    return this.vehicle;
  }

  @Nullable
  public Entity getControlledVehicle() {
    return this.vehicle != null && this.vehicle.getControllingPassenger() == this ? this.vehicle : null;
  }

  public final boolean hasControllingPassenger() {
    return this.getControllingPassenger() != null;
  }

  public boolean isOnFire() {
    return !this.fireImmune() && (this.remainingFireTicks > 0 || this.getSharedFlag(FLAG_ONFIRE));
  }

  public boolean isPassenger() {
    return this.getVehicle() != null;
  }

  public final List<Entity> getPassengers() {
    return this.passengers;
  }

  @Nullable
  public Entity getFirstPassenger() {
    return this.passengers.isEmpty() ? null : this.passengers.get(0);
  }

  public boolean hasPassenger(Entity entity) {
    return this.passengers.contains(entity);
  }

  public boolean hasPassenger(Predicate<Entity> predicate) {
    for (var passenger : this.passengers) {
      if (predicate.test(passenger)) {
        return true;
      }
    }

    return false;
  }

  private Stream<Entity> getIndirectPassengersStream() {
    return this.passengers.stream().flatMap(Entity::getSelfAndPassengers);
  }

  public Stream<Entity> getSelfAndPassengers() {
    return Stream.concat(Stream.of(this), this.getIndirectPassengersStream());
  }

  public Stream<Entity> getPassengersAndSelf() {
    return Stream.concat(this.passengers.stream().flatMap(Entity::getPassengersAndSelf), Stream.of(this));
  }

  public Iterable<Entity> getIndirectPassengers() {
    return () -> this.getIndirectPassengersStream().iterator();
  }

  public int countPlayerPassengers() {
    return (int) this.getIndirectPassengersStream().filter(entity -> entity instanceof Player).count();
  }

  public boolean hasExactlyOnePlayerPassenger() {
    return this.countPlayerPassengers() == 1;
  }

  public Entity getRootVehicle() {
    var currentEntity = this;

    while (currentEntity.isPassenger()) {
      currentEntity = currentEntity.getVehicle();
    }

    return currentEntity;
  }

  public boolean isPassengerOfSameVehicle(Entity entity) {
    return this.getRootVehicle() == entity.getRootVehicle();
  }

  public boolean hasIndirectPassenger(Entity entity) {
    if (!entity.isPassenger()) {
      return false;
    } else {
      var vehicle = entity.getVehicle();
      return vehicle == this || this.hasIndirectPassenger(vehicle);
    }
  }

  public boolean isAttackable() {
    return true; // TODO: Implement
  }

  public boolean skipAttackInteraction(Entity entity) {
    return false; // TODO: Implement
  }

  public int getPortalCooldown() {
    return this.portalCooldown;
  }

  public void setPortalCooldown(int portalCooldown) {
    this.portalCooldown = portalCooldown;
  }

  public boolean isOnPortalCooldown() {
    return this.portalCooldown > 0;
  }

  protected void processPortalCooldown() {
    if (this.isOnPortalCooldown()) {
      this.portalCooldown--;
    }
  }

  public boolean hurtClient() {
    return false; // TODO: Implement
  }

  record Movement(Vector3d from, Vector3d to) {
  }
}
