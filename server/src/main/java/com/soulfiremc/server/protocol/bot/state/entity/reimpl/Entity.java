package com.soulfiremc.server.protocol.bot.state.entity.reimpl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Entity implements SyncedDataHolder, Nameable, EntityAccess, CommandSource, ScoreHolder {
  private static final Logger LOGGER = LoggerFactory.getLogger(Entity.class);
  public static final String ID_TAG = "id";
  public static final String PASSENGERS_TAG = "Passengers";
  private static final AtomicInteger ENTITY_COUNTER = new AtomicInteger();
  public static final int CONTENTS_SLOT_INDEX = 0;
  public static final int BOARDING_COOLDOWN = 60;
  public static final int TOTAL_AIR_SUPPLY = 300;
  public static final int MAX_ENTITY_TAG_COUNT = 1024;
  public static final float DELTA_AFFECTED_BY_BLOCKS_BELOW_0_2 = 0.2F;
  public static final double DELTA_AFFECTED_BY_BLOCKS_BELOW_0_5 = 0.500001;
  public static final double DELTA_AFFECTED_BY_BLOCKS_BELOW_1_0 = 0.999999;
  public static final int BASE_TICKS_REQUIRED_TO_FREEZE = 140;
  public static final int FREEZE_HURT_FREQUENCY = 40;
  public static final int BASE_SAFE_FALL_DISTANCE = 3;
  private static final AABB INITIAL_AABB = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
  private static final double WATER_FLOW_SCALE = 0.014;
  private static final double LAVA_FAST_FLOW_SCALE = 0.007;
  private static final double LAVA_SLOW_FLOW_SCALE = 0.0023333333333333335;
  public static final String UUID_TAG = "UUID";
  private static double viewScale = 1.0;
  private final EntityType<?> type;
  private int id = ENTITY_COUNTER.incrementAndGet();
  public boolean blocksBuilding;
  private ImmutableList<Entity> passengers = ImmutableList.of();
  protected int boardingCooldown;
  @Nullable
  private Entity vehicle;
  private Level level;
  public double xo;
  public double yo;
  public double zo;
  private Vec3 position;
  private BlockPos blockPosition;
  private ChunkPos chunkPosition;
  private Vec3 deltaMovement = Vec3.ZERO;
  private float yRot;
  private float xRot;
  public float yRotO;
  public float xRotO;
  private AABB bb = INITIAL_AABB;
  private boolean onGround;
  public boolean horizontalCollision;
  public boolean verticalCollision;
  public boolean verticalCollisionBelow;
  public boolean minorHorizontalCollision;
  public boolean hurtMarked;
  protected Vec3 stuckSpeedMultiplier = Vec3.ZERO;
  @Nullable
  private Entity.RemovalReason removalReason;
  public static final float DEFAULT_BB_WIDTH = 0.6F;
  public static final float DEFAULT_BB_HEIGHT = 1.8F;
  public float walkDistO;
  public float walkDist;
  public float moveDist;
  public float flyDist;
  public float fallDistance;
  private float nextStep = 1.0F;
  public double xOld;
  public double yOld;
  public double zOld;
  public boolean noPhysics;
  protected final RandomSource random = RandomSource.create();
  public int tickCount;
  private int remainingFireTicks = -this.getFireImmuneTicks();
  protected boolean wasTouchingWater;
  protected Object2DoubleMap<TagKey<Fluid>> fluidHeight = new Object2DoubleArrayMap(2);
  protected boolean wasEyeInWater;
  private final Set<TagKey<Fluid>> fluidOnEyes = new HashSet<>();
  public int invulnerableTime;
  protected boolean firstTick = true;
  protected final SynchedEntityData entityData;
  protected static final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BYTE);
  protected static final int FLAG_ONFIRE = 0;
  private static final int FLAG_SHIFT_KEY_DOWN = 1;
  private static final int FLAG_SPRINTING = 3;
  private static final int FLAG_SWIMMING = 4;
  private static final int FLAG_INVISIBLE = 5;
  protected static final int FLAG_GLOWING = 6;
  protected static final int FLAG_FALL_FLYING = 7;
  private static final EntityDataAccessor<Integer> DATA_AIR_SUPPLY_ID = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Optional<Component>> DATA_CUSTOM_NAME = SynchedEntityData.defineId(
    Entity.class, EntityDataSerializers.OPTIONAL_COMPONENT
  );
  private static final EntityDataAccessor<Boolean> DATA_CUSTOM_NAME_VISIBLE = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Boolean> DATA_SILENT = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Boolean> DATA_NO_GRAVITY = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
  protected static final EntityDataAccessor<Pose> DATA_POSE = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.POSE);
  private static final EntityDataAccessor<Integer> DATA_TICKS_FROZEN = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
  private EntityInLevelCallback levelCallback = EntityInLevelCallback.NULL;
  private final VecDeltaCodec packetPositionCodec = new VecDeltaCodec();
  public boolean noCulling;
  public boolean hasImpulse;
  @Nullable
  public PortalProcessor portalProcess;
  private int portalCooldown;
  private boolean invulnerable;
  protected UUID uuid = Mth.createInsecureUUID(this.random);
  protected String stringUUID = this.uuid.toString();
  private boolean hasGlowingTag;
  private final Set<String> tags = Sets.newHashSet();
  private final double[] pistonDeltas = new double[]{0.0, 0.0, 0.0};
  private long pistonDeltasGameTime;
  private EntityDimensions dimensions;
  private float eyeHeight;
  public boolean isInPowderSnow;
  public boolean wasInPowderSnow;
  public boolean wasOnFire;
  public Optional<BlockPos> mainSupportingBlockPos = Optional.empty();
  private boolean onGroundNoBlocks = false;
  private float crystalSoundIntensity;
  private int lastCrystalSoundPlayTick;
  private boolean hasVisualFire;
  @Nullable
  private BlockState inBlockState = null;

  public Entity(EntityType<?> arg, Level arg2) {
    this.type = arg;
    this.level = arg2;
    this.dimensions = arg.getDimensions();
    this.position = Vec3.ZERO;
    this.blockPosition = BlockPos.ZERO;
    this.chunkPosition = ChunkPos.ZERO;
    SynchedEntityData.Builder lv = new SynchedEntityData.Builder(this);
    lv.define(DATA_SHARED_FLAGS_ID, (byte)0);
    lv.define(DATA_AIR_SUPPLY_ID, this.getMaxAirSupply());
    lv.define(DATA_CUSTOM_NAME_VISIBLE, false);
    lv.define(DATA_CUSTOM_NAME, Optional.empty());
    lv.define(DATA_SILENT, false);
    lv.define(DATA_NO_GRAVITY, false);
    lv.define(DATA_POSE, Pose.STANDING);
    lv.define(DATA_TICKS_FROZEN, 0);
    this.defineSynchedData(lv);
    this.entityData = lv.build();
    this.setPos(0.0, 0.0, 0.0);
    this.eyeHeight = this.dimensions.eyeHeight();
  }

  public boolean isColliding(BlockPos pos, BlockState state) {
    VoxelShape lv = state.getCollisionShape(this.level(), pos, CollisionContext.of(this));
    VoxelShape lv2 = lv.move((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
    return Shapes.joinIsNotEmpty(lv2, Shapes.create(this.getBoundingBox()), BooleanOp.AND);
  }

  public int getTeamColor() {
    Team lv = this.getTeam();
    return lv != null && lv.getColor().getColor() != null ? lv.getColor().getColor() : 16777215;
  }

  public boolean isSpectator() {
    return false;
  }

  public final void unRide() {
    if (this.isVehicle()) {
      this.ejectPassengers();
    }

    if (this.isPassenger()) {
      this.stopRiding();
    }
  }

  public void syncPacketPositionCodec(double x, double y, double z) {
    this.packetPositionCodec.setBase(new Vec3(x, y, z));
  }

  public VecDeltaCodec getPositionCodec() {
    return this.packetPositionCodec;
  }

  public EntityType<?> getType() {
    return this.type;
  }

  @Override
  public int getId() {
    return this.id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Set<String> getTags() {
    return this.tags;
  }

  public boolean addTag(String tag) {
    return this.tags.size() >= 1024 ? false : this.tags.add(tag);
  }

  public boolean removeTag(String tag) {
    return this.tags.remove(tag);
  }

  public void kill() {
    this.remove(Entity.RemovalReason.KILLED);
    this.gameEvent(GameEvent.ENTITY_DIE);
  }

  public final void discard() {
    this.remove(Entity.RemovalReason.DISCARDED);
  }

  protected abstract void defineSynchedData(SynchedEntityData.Builder builder);

  public SynchedEntityData getEntityData() {
    return this.entityData;
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof Entity ? ((Entity)object).id == this.id : false;
  }

  @Override
  public int hashCode() {
    return this.id;
  }

  public void remove(Entity.RemovalReason reason) {
    this.setRemoved(reason);
  }

  public void onClientRemoval() {
  }

  public void setPose(Pose pose) {
    this.entityData.set(DATA_POSE, pose);
  }

  public Pose getPose() {
    return this.entityData.get(DATA_POSE);
  }

  public boolean hasPose(Pose pose) {
    return this.getPose() == pose;
  }

  public boolean closerThan(Entity entity, double distance) {
    return this.position().closerThan(entity.position(), distance);
  }

  public boolean closerThan(Entity entity, double horizontalDistance, double verticalDistance) {
    double f = entity.getX() - this.getX();
    double g = entity.getY() - this.getY();
    double h = entity.getZ() - this.getZ();
    return Mth.lengthSquared(f, h) < Mth.square(horizontalDistance) && Mth.square(g) < Mth.square(verticalDistance);
  }

  protected void setRot(float yRot, float xRot) {
    this.setYRot(yRot % 360.0F);
    this.setXRot(xRot % 360.0F);
  }

  public final void setPos(Vec3 pos) {
    this.setPos(pos.x(), pos.y(), pos.z());
  }

  public void setPos(double x, double y, double z) {
    this.setPosRaw(x, y, z);
    this.setBoundingBox(this.makeBoundingBox());
  }

  protected AABB makeBoundingBox() {
    return this.dimensions.makeBoundingBox(this.position);
  }

  protected void reapplyPosition() {
    this.setPos(this.position.x, this.position.y, this.position.z);
  }

  public void turn(double yRot, double xRot) {
    float f = (float)xRot * 0.15F;
    float g = (float)yRot * 0.15F;
    this.setXRot(this.getXRot() + f);
    this.setYRot(this.getYRot() + g);
    this.setXRot(Mth.clamp(this.getXRot(), -90.0F, 90.0F));
    this.xRotO += f;
    this.yRotO += g;
    this.xRotO = Mth.clamp(this.xRotO, -90.0F, 90.0F);
    if (this.vehicle != null) {
      this.vehicle.onPassengerTurned(this);
    }
  }

  public void tick() {
    this.baseTick();
  }

  public void baseTick() {
    this.level().getProfiler().push("entityBaseTick");
    this.inBlockState = null;
    if (this.isPassenger() && this.getVehicle().isRemoved()) {
      this.stopRiding();
    }

    if (this.boardingCooldown > 0) {
      this.boardingCooldown--;
    }

    this.walkDistO = this.walkDist;
    this.xRotO = this.getXRot();
    this.yRotO = this.getYRot();
    if (this.canSpawnSprintParticle()) {
      this.spawnSprintParticle();
    }

    this.wasInPowderSnow = this.isInPowderSnow;
    this.isInPowderSnow = false;
    this.updateInWaterStateAndDoFluidPushing();
    this.updateFluidOnEyes();
    this.updateSwimming();
    if (this.level().isClientSide) {
      this.clearFire();
    } else if (this.remainingFireTicks > 0) {
      if (this.fireImmune()) {
        this.setRemainingFireTicks(this.remainingFireTicks - 4);
        if (this.remainingFireTicks < 0) {
          this.clearFire();
        }
      } else {
        if (this.remainingFireTicks % 20 == 0 && !this.isInLava()) {
          this.hurt(this.damageSources().onFire(), 1.0F);
        }

        this.setRemainingFireTicks(this.remainingFireTicks - 1);
      }

      if (this.getTicksFrozen() > 0) {
        this.setTicksFrozen(0);
        this.level().levelEvent(null, 1009, this.blockPosition, 1);
      }
    }

    if (this.isInLava()) {
      this.lavaHurt();
      this.fallDistance *= 0.5F;
    }

    this.checkBelowWorld();
    if (!this.level().isClientSide) {
      this.setSharedFlagOnFire(this.remainingFireTicks > 0);
    }

    this.firstTick = false;
    if (!this.level().isClientSide && this instanceof Leashable) {
      Leashable.tickLeash((Entity)((Leashable)this));
    }

    this.level().getProfiler().pop();
  }

  public void setSharedFlagOnFire(boolean isOnFire) {
    this.setSharedFlag(0, isOnFire || this.hasVisualFire);
  }

  public void checkBelowWorld() {
    if (this.getY() < (double)(this.level().getMinBuildHeight() - 64)) {
      this.onBelowWorld();
    }
  }

  public void setPortalCooldown() {
    this.portalCooldown = this.getDimensionChangingDelay();
  }

  public void setPortalCooldown(int portalCooldown) {
    this.portalCooldown = portalCooldown;
  }

  public int getPortalCooldown() {
    return this.portalCooldown;
  }

  public boolean isOnPortalCooldown() {
    return this.portalCooldown > 0;
  }

  protected void processPortalCooldown() {
    if (this.isOnPortalCooldown()) {
      this.portalCooldown--;
    }
  }

  public void lavaHurt() {
    if (!this.fireImmune()) {
      this.igniteForSeconds(15.0F);
      this.hurt(this.damageSources().lava(), 4.0F);
    }
  }

  public final void igniteForSeconds(float seconds) {
    this.igniteForTicks(Mth.floor(seconds * 20.0F));
  }

  public void igniteForTicks(int ticks) {
    if (this.remainingFireTicks < ticks) {
      this.setRemainingFireTicks(ticks);
    }
  }

  public void setRemainingFireTicks(int remainingFireTicks) {
    this.remainingFireTicks = remainingFireTicks;
  }

  public int getRemainingFireTicks() {
    return this.remainingFireTicks;
  }

  public void clearFire() {
    this.setRemainingFireTicks(0);
  }

  protected void onBelowWorld() {
    this.discard();
  }

  public boolean isFree(double x, double y, double z) {
    return this.isFree(this.getBoundingBox().move(x, y, z));
  }

  private boolean isFree(AABB box) {
    return this.level().noCollision(this, box) && !this.level().containsAnyLiquid(box);
  }

  public void setOnGround(boolean onGround) {
    this.onGround = onGround;
    this.checkSupportingBlock(onGround, null);
  }

  public void setOnGroundWithMovement(boolean onGround, Vec3 movement) {
    this.onGround = onGround;
    this.checkSupportingBlock(onGround, movement);
  }

  public boolean isSupportedBy(BlockPos pos) {
    return this.mainSupportingBlockPos.isPresent() && this.mainSupportingBlockPos.get().equals(pos);
  }

  protected void checkSupportingBlock(boolean onGround, @Nullable Vec3 movement) {
    if (onGround) {
      AABB lv = this.getBoundingBox();
      AABB lv2 = new AABB(lv.minX, lv.minY - 1.0E-6, lv.minZ, lv.maxX, lv.minY, lv.maxZ);
      Optional<BlockPos> optional = this.level.findSupportingBlock(this, lv2);
      if (optional.isPresent() || this.onGroundNoBlocks) {
        this.mainSupportingBlockPos = optional;
      } else if (movement != null) {
        AABB lv3 = lv2.move(-movement.x, 0.0, -movement.z);
        optional = this.level.findSupportingBlock(this, lv3);
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

  public boolean onGround() {
    return this.onGround;
  }

  public void move(MoverType type, Vec3 pos) {
    if (this.noPhysics) {
      this.setPos(this.getX() + pos.x, this.getY() + pos.y, this.getZ() + pos.z);
    } else {
      this.wasOnFire = this.isOnFire();
      if (type == MoverType.PISTON) {
        pos = this.limitPistonMovement(pos);
        if (pos.equals(Vec3.ZERO)) {
          return;
        }
      }

      this.level().getProfiler().push("move");
      if (this.stuckSpeedMultiplier.lengthSqr() > 1.0E-7) {
        pos = pos.multiply(this.stuckSpeedMultiplier);
        this.stuckSpeedMultiplier = Vec3.ZERO;
        this.setDeltaMovement(Vec3.ZERO);
      }

      pos = this.maybeBackOffFromEdge(pos, type);
      Vec3 lv = this.collide(pos);
      double d = lv.lengthSqr();
      if (d > 1.0E-7) {
        if (this.fallDistance != 0.0F && d >= 1.0) {
          BlockHitResult lv2 = this.level()
            .clip(new ClipContext(this.position(), this.position().add(lv), ClipContext.Block.FALLDAMAGE_RESETTING, ClipContext.Fluid.WATER, this));
          if (lv2.getType() != HitResult.Type.MISS) {
            this.resetFallDistance();
          }
        }

        this.setPos(this.getX() + lv.x, this.getY() + lv.y, this.getZ() + lv.z);
      }

      this.level().getProfiler().pop();
      this.level().getProfiler().push("rest");
      boolean bl = !Mth.equal(pos.x, lv.x);
      boolean bl2 = !Mth.equal(pos.z, lv.z);
      this.horizontalCollision = bl || bl2;
      this.verticalCollision = pos.y != lv.y;
      this.verticalCollisionBelow = this.verticalCollision && pos.y < 0.0;
      if (this.horizontalCollision) {
        this.minorHorizontalCollision = this.isHorizontalCollisionMinor(lv);
      } else {
        this.minorHorizontalCollision = false;
      }

      this.setOnGroundWithMovement(this.verticalCollisionBelow, lv);
      BlockPos lv3 = this.getOnPosLegacy();
      BlockState lv4 = this.level().getBlockState(lv3);
      this.checkFallDamage(lv.y, this.onGround(), lv4, lv3);
      if (this.isRemoved()) {
        this.level().getProfiler().pop();
      } else {
        if (this.horizontalCollision) {
          Vec3 lv5 = this.getDeltaMovement();
          this.setDeltaMovement(bl ? 0.0 : lv5.x, lv5.y, bl2 ? 0.0 : lv5.z);
        }

        Block lv6 = lv4.getBlock();
        if (pos.y != lv.y) {
          lv6.updateEntityAfterFallOn(this.level(), this);
        }

        if (this.onGround()) {
          lv6.stepOn(this.level(), lv3, lv4, this);
        }

        Entity.MovementEmission lv7 = this.getMovementEmission();
        if (lv7.emitsAnything() && !this.isPassenger()) {
          double e = lv.x;
          double f = lv.y;
          double g = lv.z;
          this.flyDist = this.flyDist + (float)(lv.length() * 0.6);
          BlockPos lv8 = this.getOnPos();
          BlockState lv9 = this.level().getBlockState(lv8);
          boolean bl3 = this.isStateClimbable(lv9);
          if (!bl3) {
            f = 0.0;
          }

          this.walkDist = this.walkDist + (float)lv.horizontalDistance() * 0.6F;
          this.moveDist = this.moveDist + (float)Math.sqrt(e * e + f * f + g * g) * 0.6F;
          if (this.moveDist > this.nextStep && !lv9.isAir()) {
            boolean bl4 = lv8.equals(lv3);
            boolean bl5 = this.vibrationAndSoundEffectsFromBlock(lv3, lv4, lv7.emitsSounds(), bl4, pos);
            if (!bl4) {
              bl5 |= this.vibrationAndSoundEffectsFromBlock(lv8, lv9, false, lv7.emitsEvents(), pos);
            }

            if (bl5) {
              this.nextStep = this.nextStep();
            } else if (this.isInWater()) {
              this.nextStep = this.nextStep();
              if (lv7.emitsSounds()) {
                this.waterSwimSound();
              }

              if (lv7.emitsEvents()) {
                this.gameEvent(GameEvent.SWIM);
              }
            }
          } else if (lv9.isAir()) {
            this.processFlappingMovement();
          }
        }

        this.tryCheckInsideBlocks();
        float h = this.getBlockSpeedFactor();
        this.setDeltaMovement(this.getDeltaMovement().multiply((double)h, 1.0, (double)h));
        if (this.level()
          .getBlockStatesIfLoaded(this.getBoundingBox().deflate(1.0E-6))
          .noneMatch(loadedState -> loadedState.is(BlockTags.FIRE) || loadedState.is(Blocks.LAVA))) {
          if (this.remainingFireTicks <= 0) {
            this.setRemainingFireTicks(-this.getFireImmuneTicks());
          }
        }

        if (this.isOnFire() && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
          this.setRemainingFireTicks(-this.getFireImmuneTicks());
        }

        this.level().getProfiler().pop();
      }
    }
  }

  private boolean isStateClimbable(BlockState state) {
    return state.is(BlockTags.CLIMBABLE) || state.is(Blocks.POWDER_SNOW);
  }

  private boolean vibrationAndSoundEffectsFromBlock(BlockPos pos, BlockState state, boolean playStepSound, boolean broadcastGameEvent, Vec3 entityPos) {
    if (state.isAir()) {
      return false;
    } else {
      boolean bl3 = this.isStateClimbable(state);
      if ((this.onGround() || bl3 || this.isCrouching() && entityPos.y == 0.0 || this.isOnRails()) && !this.isSwimming()) {
        if (playStepSound) {
          this.walkingStepSound(pos, state);
        }

        if (broadcastGameEvent) {
          this.level().gameEvent(GameEvent.STEP, this.position(), GameEvent.Context.of(this, state));
        }

        return true;
      } else {
        return false;
      }
    }
  }

  protected boolean isHorizontalCollisionMinor(Vec3 deltaMovement) {
    return false;
  }

  protected void tryCheckInsideBlocks() {
    try {
      this.checkInsideBlocks();
    } catch (Throwable var4) {
      CrashReport lv = CrashReport.forThrowable(var4, "Checking entity block collision");
      CrashReportCategory lv2 = lv.addCategory("Entity being checked for collision");
      this.fillCrashReportCategory(lv2);
      throw new ReportedException(lv);
    }
  }

  public void extinguishFire() {
    this.clearFire();
  }

  protected void processFlappingMovement() {
    if (this.isFlapping()) {
      this.onFlap();
      if (this.getMovementEmission().emitsEvents()) {
        this.gameEvent(GameEvent.FLAP);
      }
    }
  }

  @Deprecated
  public BlockPos getOnPosLegacy() {
    return this.getOnPos(0.2F);
  }

  public BlockPos getBlockPosBelowThatAffectsMyMovement() {
    return this.getOnPos(0.500001F);
  }

  public BlockPos getOnPos() {
    return this.getOnPos(1.0E-5F);
  }

  protected BlockPos getOnPos(float yOffset) {
    if (this.mainSupportingBlockPos.isPresent()) {
      BlockPos lv = this.mainSupportingBlockPos.get();
      if (!(yOffset > 1.0E-5F)) {
        return lv;
      } else {
        BlockState lv2 = this.level().getBlockState(lv);
        return (!((double)yOffset <= 0.5) || !lv2.is(BlockTags.FENCES)) && !lv2.is(BlockTags.WALLS) && !(lv2.getBlock() instanceof FenceGateBlock)
          ? lv.atY(Mth.floor(this.position.y - (double)yOffset))
          : lv;
      }
    } else {
      int i = Mth.floor(this.position.x);
      int j = Mth.floor(this.position.y - (double)yOffset);
      int k = Mth.floor(this.position.z);
      return new BlockPos(i, j, k);
    }
  }

  protected float getBlockJumpFactor() {
    float f = this.level().getBlockState(this.blockPosition()).getBlock().getJumpFactor();
    float g = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
    return (double)f == 1.0 ? g : f;
  }

  protected float getBlockSpeedFactor() {
    BlockState lv = this.level().getBlockState(this.blockPosition());
    float f = lv.getBlock().getSpeedFactor();
    if (!lv.is(Blocks.WATER) && !lv.is(Blocks.BUBBLE_COLUMN)) {
      return (double)f == 1.0 ? this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getSpeedFactor() : f;
    } else {
      return f;
    }
  }

  protected Vec3 maybeBackOffFromEdge(Vec3 vec, MoverType mover) {
    return vec;
  }

  protected Vec3 limitPistonMovement(Vec3 pos) {
    if (pos.lengthSqr() <= 1.0E-7) {
      return pos;
    } else {
      long l = this.level().getGameTime();
      if (l != this.pistonDeltasGameTime) {
        Arrays.fill(this.pistonDeltas, 0.0);
        this.pistonDeltasGameTime = l;
      }

      if (pos.x != 0.0) {
        double d = this.applyPistonMovementRestriction(Direction.Axis.X, pos.x);
        return Math.abs(d) <= 1.0E-5F ? Vec3.ZERO : new Vec3(d, 0.0, 0.0);
      } else if (pos.y != 0.0) {
        double d = this.applyPistonMovementRestriction(Direction.Axis.Y, pos.y);
        return Math.abs(d) <= 1.0E-5F ? Vec3.ZERO : new Vec3(0.0, d, 0.0);
      } else if (pos.z != 0.0) {
        double d = this.applyPistonMovementRestriction(Direction.Axis.Z, pos.z);
        return Math.abs(d) <= 1.0E-5F ? Vec3.ZERO : new Vec3(0.0, 0.0, d);
      } else {
        return Vec3.ZERO;
      }
    }
  }

  private double applyPistonMovementRestriction(Direction.Axis axis, double distance) {
    int i = axis.ordinal();
    double e = Mth.clamp(distance + this.pistonDeltas[i], -0.51, 0.51);
    distance = e - this.pistonDeltas[i];
    this.pistonDeltas[i] = e;
    return distance;
  }

  private Vec3 collide(Vec3 vec) {
    AABB lv = this.getBoundingBox();
    List<VoxelShape> list = this.level().getEntityCollisions(this, lv.expandTowards(vec));
    Vec3 lv2 = vec.lengthSqr() == 0.0 ? vec : collideBoundingBox(this, vec, lv, this.level(), list);
    boolean bl = vec.x != lv2.x;
    boolean bl2 = vec.y != lv2.y;
    boolean bl3 = vec.z != lv2.z;
    boolean bl4 = bl2 && vec.y < 0.0;
    if (this.maxUpStep() > 0.0F && (bl4 || this.onGround()) && (bl || bl3)) {
      AABB lv3 = bl4 ? lv.move(0.0, lv2.y, 0.0) : lv;
      AABB lv4 = lv3.expandTowards(vec.x, (double)this.maxUpStep(), vec.z);
      if (!bl4) {
        lv4 = lv4.expandTowards(0.0, -1.0E-5F, 0.0);
      }

      List<VoxelShape> list2 = collectColliders(this, this.level, list, lv4);
      float f = (float)lv2.y;
      float[] fs = collectCandidateStepUpHeights(lv3, list2, this.maxUpStep(), f);

      for (float g : fs) {
        Vec3 lv5 = collideWithShapes(new Vec3(vec.x, (double)g, vec.z), lv3, list2);
        if (lv5.horizontalDistanceSqr() > lv2.horizontalDistanceSqr()) {
          double d = lv.minY - lv3.minY;
          return lv5.add(0.0, -d, 0.0);
        }
      }
    }

    return lv2;
  }

  private static float[] collectCandidateStepUpHeights(AABB box, List<VoxelShape> colliders, float deltaY, float maxUpStep) {
    FloatSet floatSet = new FloatArraySet(4);

    for (VoxelShape lv : colliders) {
      DoubleList doubleList = lv.getCoords(Direction.Axis.Y);
      DoubleListIterator var8 = doubleList.iterator();

      while (var8.hasNext()) {
        double d = (Double)var8.next();
        float h = (float)(d - box.minY);
        if (!(h < 0.0F) && h != maxUpStep) {
          if (h > deltaY) {
            break;
          }

          floatSet.add(h);
        }
      }
    }

    float[] fs = floatSet.toFloatArray();
    FloatArrays.unstableSort(fs);
    return fs;
  }

  public static Vec3 collideBoundingBox(@Nullable Entity entity, Vec3 vec, AABB collisionBox, Level level, List<VoxelShape> potentialHits) {
    List<VoxelShape> list2 = collectColliders(entity, level, potentialHits, collisionBox.expandTowards(vec));
    return collideWithShapes(vec, collisionBox, list2);
  }

  private static List<VoxelShape> collectColliders(@Nullable Entity entity, Level level, List<VoxelShape> collisions, AABB boundingBox) {
    Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(collisions.size() + 1);
    if (!collisions.isEmpty()) {
      builder.addAll(collisions);
    }

    WorldBorder lv = level.getWorldBorder();
    boolean bl = entity != null && lv.isInsideCloseToBorder(entity, boundingBox);
    if (bl) {
      builder.add(lv.getCollisionShape());
    }

    builder.addAll(level.getBlockCollisions(entity, boundingBox));
    return builder.build();
  }

  private static Vec3 collideWithShapes(Vec3 deltaMovement, AABB entityBB, List<VoxelShape> shapes) {
    if (shapes.isEmpty()) {
      return deltaMovement;
    } else {
      double d = deltaMovement.x;
      double e = deltaMovement.y;
      double f = deltaMovement.z;
      if (e != 0.0) {
        e = Shapes.collide(Direction.Axis.Y, entityBB, shapes, e);
        if (e != 0.0) {
          entityBB = entityBB.move(0.0, e, 0.0);
        }
      }

      boolean bl = Math.abs(d) < Math.abs(f);
      if (bl && f != 0.0) {
        f = Shapes.collide(Direction.Axis.Z, entityBB, shapes, f);
        if (f != 0.0) {
          entityBB = entityBB.move(0.0, 0.0, f);
        }
      }

      if (d != 0.0) {
        d = Shapes.collide(Direction.Axis.X, entityBB, shapes, d);
        if (!bl && d != 0.0) {
          entityBB = entityBB.move(d, 0.0, 0.0);
        }
      }

      if (!bl && f != 0.0) {
        f = Shapes.collide(Direction.Axis.Z, entityBB, shapes, f);
      }

      return new Vec3(d, e, f);
    }
  }

  protected float nextStep() {
    return (float)((int)this.moveDist + 1);
  }

  protected SoundEvent getSwimSound() {
    return SoundEvents.GENERIC_SWIM;
  }

  protected SoundEvent getSwimSplashSound() {
    return SoundEvents.GENERIC_SPLASH;
  }

  protected SoundEvent getSwimHighSpeedSplashSound() {
    return SoundEvents.GENERIC_SPLASH;
  }

  protected void checkInsideBlocks() {
    AABB lv = this.getBoundingBox();
    BlockPos lv2 = BlockPos.containing(lv.minX + 1.0E-7, lv.minY + 1.0E-7, lv.minZ + 1.0E-7);
    BlockPos lv3 = BlockPos.containing(lv.maxX - 1.0E-7, lv.maxY - 1.0E-7, lv.maxZ - 1.0E-7);
    if (this.level().hasChunksAt(lv2, lv3)) {
      BlockPos.MutableBlockPos lv4 = new BlockPos.MutableBlockPos();

      for (int i = lv2.getX(); i <= lv3.getX(); i++) {
        for (int j = lv2.getY(); j <= lv3.getY(); j++) {
          for (int k = lv2.getZ(); k <= lv3.getZ(); k++) {
            if (!this.isAlive()) {
              return;
            }

            lv4.set(i, j, k);
            BlockState lv5 = this.level().getBlockState(lv4);

            try {
              lv5.entityInside(this.level(), lv4, this);
              this.onInsideBlock(lv5);
            } catch (Throwable var12) {
              CrashReport lv6 = CrashReport.forThrowable(var12, "Colliding entity with block");
              CrashReportCategory lv7 = lv6.addCategory("Block being collided with");
              CrashReportCategory.populateBlockDetails(lv7, this.level(), lv4, lv5);
              throw new ReportedException(lv6);
            }
          }
        }
      }
    }
  }

  protected void onInsideBlock(BlockState state) {
  }

  public void gameEvent(Holder<GameEvent> gameEvent, @Nullable Entity entity) {
    this.level().gameEvent(entity, gameEvent, this.position);
  }

  public void gameEvent(Holder<GameEvent> gameEvent) {
    this.gameEvent(gameEvent, this);
  }

  protected void onFlap() {
  }

  protected boolean isFlapping() {
    return false;
  }

  public boolean isSilent() {
    return this.entityData.get(DATA_SILENT);
  }

  public void setSilent(boolean isSilent) {
    this.entityData.set(DATA_SILENT, isSilent);
  }

  public boolean isNoGravity() {
    return this.entityData.get(DATA_NO_GRAVITY);
  }

  public void setNoGravity(boolean noGravity) {
    this.entityData.set(DATA_NO_GRAVITY, noGravity);
  }

  protected double getDefaultGravity() {
    return 0.0;
  }

  public final double getGravity() {
    return this.isNoGravity() ? 0.0 : this.getDefaultGravity();
  }

  protected void applyGravity() {
    double d = this.getGravity();
    if (d != 0.0) {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0, -d, 0.0));
    }
  }

  protected Entity.MovementEmission getMovementEmission() {
    return Entity.MovementEmission.ALL;
  }

  protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    if (onGround) {
      if (this.fallDistance > 0.0F) {
        state.getBlock().fallOn(this.level(), state, pos, this, this.fallDistance);
        this.level()
          .gameEvent(
            GameEvent.HIT_GROUND,
            this.position,
            GameEvent.Context.of(this, this.mainSupportingBlockPos.<BlockState>map(arg -> this.level().getBlockState(arg)).orElse(state))
          );
      }

      this.resetFallDistance();
    } else if (y < 0.0) {
      this.fallDistance -= (float)y;
    }
  }

  public boolean fireImmune() {
    return this.getType().fireImmune();
  }

  public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
    if (this.type.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
      return false;
    } else {
      if (this.isVehicle()) {
        for (Entity lv : this.getPassengers()) {
          lv.causeFallDamage(fallDistance, multiplier, source);
        }
      }

      return false;
    }
  }

  public boolean isInWater() {
    return this.wasTouchingWater;
  }

  private boolean isInRain() {
    BlockPos lv = this.blockPosition();
    return this.level().isRainingAt(lv) || this.level().isRainingAt(BlockPos.containing((double)lv.getX(), this.getBoundingBox().maxY, (double)lv.getZ()));
  }

  private boolean isInBubbleColumn() {
    return this.getInBlockState().is(Blocks.BUBBLE_COLUMN);
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

  public boolean isInLiquid() {
    return this.isInWaterOrBubble() || this.isInLava();
  }

  public boolean isUnderWater() {
    return this.wasEyeInWater && this.isInWater();
  }

  public void updateSwimming() {
    if (this.isSwimming()) {
      this.setSwimming(this.isSprinting() && this.isInWater() && !this.isPassenger());
    } else {
      this.setSwimming(
        this.isSprinting() && this.isUnderWater() && !this.isPassenger() && this.level().getFluidState(this.blockPosition).is(FluidTags.WATER)
      );
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
    if (this.getVehicle() instanceof Boat lv && !lv.isUnderWater()) {
      this.wasTouchingWater = false;
      return;
    }

    if (this.updateFluidHeightAndDoFluidPushing(FluidTags.WATER, 0.014)) {
      if (!this.wasTouchingWater && !this.firstTick) {
        this.doWaterSplashEffect();
      }

      this.resetFallDistance();
      this.wasTouchingWater = true;
      this.clearFire();
    } else {
      this.wasTouchingWater = false;
    }
  }

  private void updateFluidOnEyes() {
    this.wasEyeInWater = this.isEyeInFluid(FluidTags.WATER);
    this.fluidOnEyes.clear();
    double d = this.getEyeY();
    if (this.getVehicle() instanceof Boat lv2 && !lv2.isUnderWater() && lv2.getBoundingBox().maxY >= d && lv2.getBoundingBox().minY <= d) {
      return;
    }

    BlockPos lv3 = BlockPos.containing(this.getX(), d, this.getZ());
    FluidState lv4 = this.level().getFluidState(lv3);
    double e = (double)((float)lv3.getY() + lv4.getHeight(this.level(), lv3));
    if (e > d) {
      lv4.getTags().forEach(this.fluidOnEyes::add);
    }
  }

  protected void doWaterSplashEffect() {
    Entity lv = Objects.requireNonNullElse(this.getControllingPassenger(), this);
    float f = lv == this ? 0.2F : 0.9F;
    Vec3 lv2 = lv.getDeltaMovement();
    float g = Math.min(1.0F, (float)Math.sqrt(lv2.x * lv2.x * 0.2F + lv2.y * lv2.y + lv2.z * lv2.z * 0.2F) * f);

    float h = (float)Mth.floor(this.getY());

    this.gameEvent(GameEvent.SPLASH);
  }

  @Deprecated
  protected BlockState getBlockStateOnLegacy() {
    return this.level().getBlockState(this.getOnPosLegacy());
  }

  public BlockState getBlockStateOn() {
    return this.level().getBlockState(this.getOnPos());
  }

  public boolean canSpawnSprintParticle() {
    return this.isSprinting() && !this.isInWater() && !this.isSpectator() && !this.isCrouching() && !this.isInLava() && this.isAlive();
  }

  protected void spawnSprintParticle() {
    BlockPos lv = this.getOnPosLegacy();
    BlockState lv2 = this.level().getBlockState(lv);
    if (lv2.getRenderShape() != RenderShape.INVISIBLE) {
      Vec3 lv3 = this.getDeltaMovement();
      BlockPos lv4 = this.blockPosition();
      double d = this.getX() + (this.random.nextDouble() - 0.5) * (double)this.dimensions.width();
      double e = this.getZ() + (this.random.nextDouble() - 0.5) * (double)this.dimensions.width();
      if (lv4.getX() != lv.getX()) {
        d = Mth.clamp(d, (double)lv.getX(), (double)lv.getX() + 1.0);
      }

      if (lv4.getZ() != lv.getZ()) {
        e = Mth.clamp(e, (double)lv.getZ(), (double)lv.getZ() + 1.0);
      }

      this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, lv2), d, this.getY() + 0.1, e, lv3.x * -4.0, 1.5, lv3.z * -4.0);
    }
  }

  public boolean isEyeInFluid(TagKey<Fluid> fluidTag) {
    return this.fluidOnEyes.contains(fluidTag);
  }

  public boolean isInLava() {
    return !this.firstTick && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
  }

  public void moveRelative(float amount, Vec3 relative) {
    Vec3 lv = getInputVector(relative, amount, this.getYRot());
    this.setDeltaMovement(this.getDeltaMovement().add(lv));
  }

  private static Vec3 getInputVector(Vec3 relative, float motionScaler, float facing) {
    double d = relative.lengthSqr();
    if (d < 1.0E-7) {
      return Vec3.ZERO;
    } else {
      Vec3 lv = (d > 1.0 ? relative.normalize() : relative).scale((double)motionScaler);
      float h = Mth.sin(facing * (float) (Math.PI / 180.0));
      float i = Mth.cos(facing * (float) (Math.PI / 180.0));
      return new Vec3(lv.x * (double)i - lv.z * (double)h, lv.y, lv.z * (double)i + lv.x * (double)h);
    }
  }

  @Deprecated
  public float getLightLevelDependentMagicValue() {
    return this.level().hasChunkAt(this.getBlockX(), this.getBlockZ())
      ? this.level().getLightLevelDependentMagicValue(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ()))
      : 0.0F;
  }

  public void absMoveTo(double x, double y, double z, float yRot, float xRot) {
    this.absMoveTo(x, y, z);
    this.absRotateTo(yRot, xRot);
  }

  public void absRotateTo(float yRot, float xRot) {
    this.setYRot(yRot % 360.0F);
    this.setXRot(Mth.clamp(xRot, -90.0F, 90.0F) % 360.0F);
    this.yRotO = this.getYRot();
    this.xRotO = this.getXRot();
  }

  public void absMoveTo(double x, double y, double z) {
    double g = Mth.clamp(x, -3.0E7, 3.0E7);
    double h = Mth.clamp(z, -3.0E7, 3.0E7);
    this.xo = g;
    this.yo = y;
    this.zo = h;
    this.setPos(g, y, h);
  }

  public void moveTo(Vec3 vec) {
    this.moveTo(vec.x, vec.y, vec.z);
  }

  public void moveTo(double x, double y, double z) {
    this.moveTo(x, y, z, this.getYRot(), this.getXRot());
  }

  public void moveTo(BlockPos pos, float yRot, float xRot) {
    this.moveTo(pos.getBottomCenter(), yRot, xRot);
  }

  public void moveTo(Vec3 pos, float yRot, float xRot) {
    this.moveTo(pos.x, pos.y, pos.z, yRot, xRot);
  }

  public void moveTo(double x, double y, double z, float yRot, float xRot) {
    this.setPosRaw(x, y, z);
    this.setYRot(yRot);
    this.setXRot(xRot);
    this.setOldPosAndRot();
    this.reapplyPosition();
  }

  public final void setOldPosAndRot() {
    double d = this.getX();
    double e = this.getY();
    double f = this.getZ();
    this.xo = d;
    this.yo = e;
    this.zo = f;
    this.xOld = d;
    this.yOld = e;
    this.zOld = f;
    this.yRotO = this.getYRot();
    this.xRotO = this.getXRot();
  }

  public float distanceTo(Entity entity) {
    float f = (float)(this.getX() - entity.getX());
    float g = (float)(this.getY() - entity.getY());
    float h = (float)(this.getZ() - entity.getZ());
    return Mth.sqrt(f * f + g * g + h * h);
  }

  public double distanceToSqr(double x, double y, double z) {
    double g = this.getX() - x;
    double h = this.getY() - y;
    double i = this.getZ() - z;
    return g * g + h * h + i * i;
  }

  public double distanceToSqr(Entity entity) {
    return this.distanceToSqr(entity.position());
  }

  public double distanceToSqr(Vec3 vec) {
    double d = this.getX() - vec.x;
    double e = this.getY() - vec.y;
    double f = this.getZ() - vec.z;
    return d * d + e * e + f * f;
  }

  public void playerTouch(Player player) {
  }

  public void push(Entity entity) {
    if (!this.isPassengerOfSameVehicle(entity)) {
      if (!entity.noPhysics && !this.noPhysics) {
        double d = entity.getX() - this.getX();
        double e = entity.getZ() - this.getZ();
        double f = Mth.absMax(d, e);
        if (f >= 0.01F) {
          f = Math.sqrt(f);
          d /= f;
          e /= f;
          double g = 1.0 / f;
          if (g > 1.0) {
            g = 1.0;
          }

          d *= g;
          e *= g;
          d *= 0.05F;
          e *= 0.05F;
          if (!this.isVehicle() && this.isPushable()) {
            this.push(-d, 0.0, -e);
          }

          if (!entity.isVehicle() && entity.isPushable()) {
            entity.push(d, 0.0, e);
          }
        }
      }
    }
  }

  public void push(Vec3 vector) {
    this.push(vector.x, vector.y, vector.z);
  }

  public void push(double x, double y, double z) {
    this.setDeltaMovement(this.getDeltaMovement().add(x, y, z));
    this.hasImpulse = true;
  }

  protected void markHurt() {
    this.hurtMarked = true;
  }

  public boolean hurt(DamageSource source, float amount) {
    if (!this.isInvulnerableTo(source)) {
      this.markHurt();
    }

    return false;
  }

  public final Vec3 getViewVector(float partialTicks) {
    return this.calculateViewVector(this.getViewXRot(partialTicks), this.getViewYRot(partialTicks));
  }

  public Direction getNearestViewDirection() {
    return Direction.getNearest(this.getViewVector(1.0F));
  }

  public float getViewXRot(float partialTicks) {
    return partialTicks == 1.0F ? this.getXRot() : Mth.lerp(partialTicks, this.xRotO, this.getXRot());
  }

  public float getViewYRot(float partialTick) {
    return partialTick == 1.0F ? this.getYRot() : Mth.lerp(partialTick, this.yRotO, this.getYRot());
  }

  public final Vec3 calculateViewVector(float xRot, float yRot) {
    float h = xRot * (float) (Math.PI / 180.0);
    float i = -yRot * (float) (Math.PI / 180.0);
    float j = Mth.cos(i);
    float k = Mth.sin(i);
    float l = Mth.cos(h);
    float m = Mth.sin(h);
    return new Vec3((double)(k * l), (double)(-m), (double)(j * l));
  }

  public final Vec3 getUpVector(float partialTicks) {
    return this.calculateUpVector(this.getViewXRot(partialTicks), this.getViewYRot(partialTicks));
  }

  protected final Vec3 calculateUpVector(float xRot, float yRot) {
    return this.calculateViewVector(xRot - 90.0F, yRot);
  }

  public final Vec3 getEyePosition() {
    return new Vec3(this.getX(), this.getEyeY(), this.getZ());
  }

  public final Vec3 getEyePosition(float partialTicks) {
    double d = Mth.lerp((double)partialTicks, this.xo, this.getX());
    double e = Mth.lerp((double)partialTicks, this.yo, this.getY()) + (double)this.getEyeHeight();
    double g = Mth.lerp((double)partialTicks, this.zo, this.getZ());
    return new Vec3(d, e, g);
  }

  public Vec3 getLightProbePosition(float partialTicks) {
    return this.getEyePosition(partialTicks);
  }

  public final Vec3 getPosition(float partialTicks) {
    double d = Mth.lerp((double)partialTicks, this.xo, this.getX());
    double e = Mth.lerp((double)partialTicks, this.yo, this.getY());
    double g = Mth.lerp((double)partialTicks, this.zo, this.getZ());
    return new Vec3(d, e, g);
  }

  public HitResult pick(double hitDistance, float partialTicks, boolean hitFluids) {
    Vec3 lv = this.getEyePosition(partialTicks);
    Vec3 lv2 = this.getViewVector(partialTicks);
    Vec3 lv3 = lv.add(lv2.x * hitDistance, lv2.y * hitDistance, lv2.z * hitDistance);
    return this.level().clip(new ClipContext(lv, lv3, ClipContext.Block.OUTLINE, hitFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
  }

  public boolean canBeHitByProjectile() {
    return this.isAlive() && this.isPickable();
  }

  public boolean isPickable() {
    return false;
  }

  public boolean isPushable() {
    return false;
  }

  public boolean shouldRender(double x, double y, double z) {
    double g = this.getX() - x;
    double h = this.getY() - y;
    double i = this.getZ() - z;
    double j = g * g + h * h + i * i;
    return this.shouldRenderAtSqrDistance(j);
  }

  public boolean shouldRenderAtSqrDistance(double distance) {
    double e = this.getBoundingBox().getSize();
    if (Double.isNaN(e)) {
      e = 1.0;
    }

    e *= 64.0 * viewScale;
    return distance < e * e;
  }

  public void load(CompoundTag compound) {
    try {
      ListTag lv = compound.getList("Pos", 6);
      ListTag lv2 = compound.getList("Motion", 6);
      ListTag lv3 = compound.getList("Rotation", 5);
      double d = lv2.getDouble(0);
      double e = lv2.getDouble(1);
      double f = lv2.getDouble(2);
      this.setDeltaMovement(Math.abs(d) > 10.0 ? 0.0 : d, Math.abs(e) > 10.0 ? 0.0 : e, Math.abs(f) > 10.0 ? 0.0 : f);
      double g = 3.0000512E7;
      this.setPosRaw(
        Mth.clamp(lv.getDouble(0), -3.0000512E7, 3.0000512E7),
        Mth.clamp(lv.getDouble(1), -2.0E7, 2.0E7),
        Mth.clamp(lv.getDouble(2), -3.0000512E7, 3.0000512E7)
      );
      this.setYRot(lv3.getFloat(0));
      this.setXRot(lv3.getFloat(1));
      this.setOldPosAndRot();
      this.setYHeadRot(this.getYRot());
      this.setYBodyRot(this.getYRot());
      this.fallDistance = compound.getFloat("FallDistance");
      this.remainingFireTicks = compound.getShort("Fire");
      if (compound.contains("Air")) {
        this.setAirSupply(compound.getShort("Air"));
      }

      this.onGround = compound.getBoolean("OnGround");
      this.invulnerable = compound.getBoolean("Invulnerable");
      this.portalCooldown = compound.getInt("PortalCooldown");
      if (compound.hasUUID("UUID")) {
        this.uuid = compound.getUUID("UUID");
        this.stringUUID = this.uuid.toString();
      }

      if (!Double.isFinite(this.getX()) || !Double.isFinite(this.getY()) || !Double.isFinite(this.getZ())) {
        throw new IllegalStateException("Entity has invalid position");
      } else if (Double.isFinite((double)this.getYRot()) && Double.isFinite((double)this.getXRot())) {
        this.reapplyPosition();
        this.setRot(this.getYRot(), this.getXRot());
        if (compound.contains("CustomName", 8)) {
          String string = compound.getString("CustomName");

          try {
            this.setCustomName(Component.Serializer.fromJson(string, this.registryAccess()));
          } catch (Exception var16) {
            LOGGER.warn("Failed to parse entity custom name {}", string, var16);
          }
        }

        this.setCustomNameVisible(compound.getBoolean("CustomNameVisible"));
        this.setSilent(compound.getBoolean("Silent"));
        this.setNoGravity(compound.getBoolean("NoGravity"));
        this.setGlowingTag(compound.getBoolean("Glowing"));
        this.setTicksFrozen(compound.getInt("TicksFrozen"));
        this.hasVisualFire = compound.getBoolean("HasVisualFire");
        if (compound.contains("Tags", 9)) {
          this.tags.clear();
          ListTag lv4 = compound.getList("Tags", 8);
          int i = Math.min(lv4.size(), 1024);

          for (int j = 0; j < i; j++) {
            this.tags.add(lv4.getString(j));
          }
        }

        this.readAdditionalSaveData(compound);
        if (this.repositionEntityAfterLoad()) {
          this.reapplyPosition();
        }
      } else {
        throw new IllegalStateException("Entity has invalid rotation");
      }
    } catch (Throwable var17) {
      CrashReport lv5 = CrashReport.forThrowable(var17, "Loading entity NBT");
      CrashReportCategory lv6 = lv5.addCategory("Entity being loaded");
      this.fillCrashReportCategory(lv6);
      throw new ReportedException(lv5);
    }
  }

  protected boolean repositionEntityAfterLoad() {
    return true;
  }

  public boolean isAlive() {
    return !this.isRemoved();
  }

  public boolean isInWall() {
    if (this.noPhysics) {
      return false;
    } else {
      float f = this.dimensions.width() * 0.8F;
      AABB lv = AABB.ofSize(this.getEyePosition(), (double)f, 1.0E-6, (double)f);
      return BlockPos.betweenClosedStream(lv)
        .anyMatch(
          arg2 -> {
            BlockState lvx = this.level().getBlockState(arg2);
            return !lvx.isAir()
              && lvx.isSuffocating(this.level(), arg2)
              && Shapes.joinIsNotEmpty(
              lvx.getCollisionShape(this.level(), arg2).move((double)arg2.getX(), (double)arg2.getY(), (double)arg2.getZ()),
              Shapes.create(lv),
              BooleanOp.AND
            );
          }
        );
    }
  }

  public InteractionResult interact(Player player, InteractionHand hand) {
    if (this.isAlive() && this instanceof Leashable lv) {
      if (lv.getLeashHolder() == player) {
        if (!this.level().isClientSide()) {
          lv.dropLeash(true, !player.hasInfiniteMaterials());
          this.gameEvent(GameEvent.ENTITY_INTERACT, player);
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide);
      }

      ItemStack lv2 = player.getItemInHand(hand);
      if (lv2.is(Items.LEAD) && lv.canHaveALeashAttachedToIt()) {
        if (!this.level().isClientSide()) {
          lv.setLeashedTo(player, true);
        }

        lv2.shrink(1);
        return InteractionResult.sidedSuccess(this.level().isClientSide);
      }
    }

    return InteractionResult.PASS;
  }

  public boolean canCollideWith(Entity entity) {
    return entity.canBeCollidedWith() && !this.isPassengerOfSameVehicle(entity);
  }

  public boolean canBeCollidedWith() {
    return false;
  }

  public void rideTick() {
    this.setDeltaMovement(Vec3.ZERO);
    this.tick();
    if (this.isPassenger()) {
      this.getVehicle().positionRider(this);
    }
  }

  public final void positionRider(Entity passenger) {
    if (this.hasPassenger(passenger)) {
      this.positionRider(passenger, Entity::setPos);
    }
  }

  protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
    Vec3 lv = this.getPassengerRidingPosition(passenger);
    Vec3 lv2 = passenger.getVehicleAttachmentPoint(this);
    callback.accept(passenger, lv.x - lv2.x, lv.y - lv2.y, lv.z - lv2.z);
  }

  public void onPassengerTurned(Entity entityToUpdate) {
  }

  public Vec3 getVehicleAttachmentPoint(Entity entity) {
    return this.getAttachments().get(EntityAttachment.VEHICLE, 0, this.yRot);
  }

  public Vec3 getPassengerRidingPosition(Entity entity) {
    return this.position().add(this.getPassengerAttachmentPoint(entity, this.dimensions, 1.0F));
  }

  protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
    return getDefaultPassengerAttachmentPoint(this, entity, dimensions.attachments());
  }

  protected static Vec3 getDefaultPassengerAttachmentPoint(Entity vehicle, Entity passenger, EntityAttachments attachments) {
    int i = vehicle.getPassengers().indexOf(passenger);
    return attachments.getClamped(EntityAttachment.PASSENGER, i, vehicle.yRot);
  }

  public boolean startRiding(Entity vehicle) {
    return this.startRiding(vehicle, false);
  }

  public boolean showVehicleHealth() {
    return this instanceof LivingEntity;
  }

  public boolean startRiding(Entity vehicle, boolean force) {
    if (vehicle == this.vehicle) {
      return false;
    } else if (!vehicle.couldAcceptPassenger()) {
      return false;
    } else {
      for (Entity lv = vehicle; lv.vehicle != null; lv = lv.vehicle) {
        if (lv.vehicle == this) {
          return false;
        }
      }

      if (force || this.canRide(vehicle) && vehicle.canAddPassenger(this)) {
        if (this.isPassenger()) {
          this.stopRiding();
        }

        this.setPose(Pose.STANDING);
        this.vehicle = vehicle;
        this.vehicle.addPassenger(this);
        return true;
      } else {
        return false;
      }
    }
  }

  protected boolean canRide(Entity vehicle) {
    return !this.isShiftKeyDown() && this.boardingCooldown <= 0;
  }

  public void ejectPassengers() {
    for (int i = this.passengers.size() - 1; i >= 0; i--) {
      ((Entity)this.passengers.get(i)).stopRiding();
    }
  }

  public void removeVehicle() {
    if (this.vehicle != null) {
      Entity lv = this.vehicle;
      this.vehicle = null;
      lv.removePassenger(this);
    }
  }

  public void stopRiding() {
    this.removeVehicle();
  }

  protected void addPassenger(Entity passenger) {
    if (passenger.getVehicle() != this) {
      throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
    } else {
      if (this.passengers.isEmpty()) {
        this.passengers = ImmutableList.of(passenger);
      } else {
        List<Entity> list = Lists.newArrayList(this.passengers);
        if (!this.level().isClientSide && passenger instanceof Player && !(this.getFirstPassenger() instanceof Player)) {
          list.add(0, passenger);
        } else {
          list.add(passenger);
        }

        this.passengers = ImmutableList.copyOf(list);
      }

      this.gameEvent(GameEvent.ENTITY_MOUNT, passenger);
    }
  }

  protected void removePassenger(Entity passenger) {
    if (passenger.getVehicle() == this) {
      throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
    } else {
      if (this.passengers.size() == 1 && this.passengers.get(0) == passenger) {
        this.passengers = ImmutableList.of();
      } else {
        this.passengers = this.passengers.stream().filter(arg2 -> arg2 != passenger).collect(ImmutableList.toImmutableList());
      }

      passenger.boardingCooldown = 60;
      this.gameEvent(GameEvent.ENTITY_DISMOUNT, passenger);
    }
  }

  protected boolean canAddPassenger(Entity passenger) {
    return this.passengers.isEmpty();
  }

  protected boolean couldAcceptPassenger() {
    return true;
  }

  public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    this.setPos(x, y, z);
    this.setRot(yRot, xRot);
  }

  public double lerpTargetX() {
    return this.getX();
  }

  public double lerpTargetY() {
    return this.getY();
  }

  public double lerpTargetZ() {
    return this.getZ();
  }

  public float lerpTargetXRot() {
    return this.getXRot();
  }

  public float lerpTargetYRot() {
    return this.getYRot();
  }

  public void lerpHeadTo(float yaw, int pitch) {
    this.setYHeadRot(yaw);
  }

  public float getPickRadius() {
    return 0.0F;
  }

  public Vec3 getLookAngle() {
    return this.calculateViewVector(this.getXRot(), this.getYRot());
  }

  public Vec3 getHandHoldingItemAngle(Item item) {
    if (!(this instanceof Player lv)) {
      return Vec3.ZERO;
    } else {
      boolean bl = lv.getOffhandItem().is(item) && !lv.getMainHandItem().is(item);
      HumanoidArm lv2 = bl ? lv.getMainArm().getOpposite() : lv.getMainArm();
      return this.calculateViewVector(0.0F, this.getYRot() + (float)(lv2 == HumanoidArm.RIGHT ? 80 : -80)).scale(0.5);
    }
  }

  public Vec2 getRotationVector() {
    return new Vec2(this.getXRot(), this.getYRot());
  }

  public Vec3 getForward() {
    return Vec3.directionFromRotation(this.getRotationVector());
  }

  public int getDimensionChangingDelay() {
    Entity lv = this.getFirstPassenger();
    return 300;
  }

  public void lerpMotion(double x, double y, double z) {
    this.setDeltaMovement(x, y, z);
  }

  public void handleEntityEvent(byte id) {
    switch (id) {
      case 53:
        break;
    }
  }

  public void animateHurt(float yaw) {
  }

  public boolean isOnFire() {
    boolean bl = this.level() != null && this.level().isClientSide;
    return !this.fireImmune() && (this.remainingFireTicks > 0 || bl && this.getSharedFlag(0));
  }

  public boolean isPassenger() {
    return this.getVehicle() != null;
  }

  public boolean isVehicle() {
    return !this.passengers.isEmpty();
  }

  public boolean dismountsUnderwater() {
    return this.getType().is(EntityTypeTags.DISMOUNTS_UNDERWATER);
  }

  public boolean canControlVehicle() {
    return !this.getType().is(EntityTypeTags.NON_CONTROLLING_RIDER);
  }

  public void setShiftKeyDown(boolean keyDown) {
    this.setSharedFlag(1, keyDown);
  }

  public boolean isShiftKeyDown() {
    return this.getSharedFlag(1);
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
    return this.hasPose(Pose.CROUCHING);
  }

  public boolean isSprinting() {
    return this.getSharedFlag(3);
  }

  public void setSprinting(boolean sprinting) {
    this.setSharedFlag(3, sprinting);
  }

  public boolean isSwimming() {
    return this.getSharedFlag(4);
  }

  public boolean isVisuallySwimming() {
    return this.hasPose(Pose.SWIMMING);
  }

  public boolean isVisuallyCrawling() {
    return this.isVisuallySwimming() && !this.isInWater();
  }

  public void setSwimming(boolean swimming) {
    this.setSharedFlag(4, swimming);
  }

  public boolean isInvisible() {
    return this.getSharedFlag(5);
  }

  public boolean isInvisibleTo(Player player) {
    if (player.isSpectator()) {
      return false;
    } else {
      Team lv = this.getTeam();
      return lv != null && player != null && player.getTeam() == lv && lv.canSeeFriendlyInvisibles() ? false : this.isInvisible();
    }
  }

  public boolean isOnRails() {
    return false;
  }

  @Nullable
  public PlayerTeam getTeam() {
    return this.level().getScoreboard().getPlayersTeam(this.getScoreboardName());
  }

  public boolean isAlliedTo(Entity entity) {
    return this.isAlliedTo(entity.getTeam());
  }

  public boolean isAlliedTo(Team team) {
    return this.getTeam() != null ? this.getTeam().isAlliedTo(team) : false;
  }

  public void setInvisible(boolean invisible) {
    this.setSharedFlag(5, invisible);
  }

  protected boolean getSharedFlag(int flag) {
    return (this.entityData.get(DATA_SHARED_FLAGS_ID) & 1 << flag) != 0;
  }

  protected void setSharedFlag(int flag, boolean set) {
    byte b = this.entityData.get(DATA_SHARED_FLAGS_ID);
    if (set) {
      this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(b | 1 << flag));
    } else {
      this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(b & ~(1 << flag)));
    }
  }

  public int getMaxAirSupply() {
    return 300;
  }

  public int getAirSupply() {
    return this.entityData.get(DATA_AIR_SUPPLY_ID);
  }

  public void setAirSupply(int air) {
    this.entityData.set(DATA_AIR_SUPPLY_ID, air);
  }

  public int getTicksFrozen() {
    return this.entityData.get(DATA_TICKS_FROZEN);
  }

  public void setTicksFrozen(int ticksFrozen) {
    this.entityData.set(DATA_TICKS_FROZEN, ticksFrozen);
  }

  public float getPercentFrozen() {
    int i = this.getTicksRequiredToFreeze();
    return (float)Math.min(this.getTicksFrozen(), i) / (float)i;
  }

  public boolean isFullyFrozen() {
    return this.getTicksFrozen() >= this.getTicksRequiredToFreeze();
  }

  public int getTicksRequiredToFreeze() {
    return 140;
  }

  public void onAboveBubbleCol(boolean downwards) {
    Vec3 lv = this.getDeltaMovement();
    double d;
    if (downwards) {
      d = Math.max(-0.9, lv.y - 0.03);
    } else {
      d = Math.min(1.8, lv.y + 0.1);
    }

    this.setDeltaMovement(lv.x, d, lv.z);
  }

  public void onInsideBubbleColumn(boolean downwards) {
    Vec3 lv = this.getDeltaMovement();
    double d;
    if (downwards) {
      d = Math.max(-0.3, lv.y - 0.03);
    } else {
      d = Math.min(0.7, lv.y + 0.06);
    }

    this.setDeltaMovement(lv.x, d, lv.z);
    this.resetFallDistance();
  }

  public void checkSlowFallDistance() {
    if (this.getDeltaMovement().y() > -0.5 && this.fallDistance > 1.0F) {
      this.fallDistance = 1.0F;
    }
  }

  public void resetFallDistance() {
    this.fallDistance = 0.0F;
  }

  public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
    this.resetFallDistance();
    this.stuckSpeedMultiplier = motionMultiplier;
  }

  private static Component removeAction(Component name) {
    MutableComponent lv = name.plainCopy().setStyle(name.getStyle().withClickEvent(null));

    for (Component lv2 : name.getSiblings()) {
      lv.append(removeAction(lv2));
    }

    return lv;
  }

  @Override
  public Component getName() {
    Component lv = this.getCustomName();
    return lv != null ? removeAction(lv) : this.getTypeName();
  }

  protected Component getTypeName() {
    return this.type.getDescription();
  }

  public boolean is(Entity entity) {
    return this == entity;
  }

  public float getYHeadRot() {
    return 0.0F;
  }

  public void setYHeadRot(float yHeadRot) {
  }

  public void setYBodyRot(float yBodyRot) {
  }

  public boolean isAttackable() {
    return true;
  }

  public boolean skipAttackInteraction(Entity entity) {
    return false;
  }

  public boolean isInvulnerableTo(DamageSource source) {
    return this.isRemoved()
      || this.invulnerable && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer()
      || source.is(DamageTypeTags.IS_FIRE) && this.fireImmune()
      || source.is(DamageTypeTags.IS_FALL) && this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE);
  }

  public boolean isInvulnerable() {
    return this.invulnerable;
  }

  public void setInvulnerable(boolean isInvulnerable) {
    this.invulnerable = isInvulnerable;
  }

  public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle portal) {
    return PortalShape.getRelativePosition(portal, axis, this.position(), this.getDimensions(this.getPose()));
  }

  public boolean canUsePortal(boolean allowPassengers) {
    return (allowPassengers || !this.isPassenger()) && this.isAlive();
  }

  public void setUUID(UUID uniqueId) {
    this.uuid = uniqueId;
    this.stringUUID = this.uuid.toString();
  }

  @Override
  public UUID getUUID() {
    return this.uuid;
  }

  public String getStringUUID() {
    return this.stringUUID;
  }

  @Override
  public String getScoreboardName() {
    return this.stringUUID;
  }

  public boolean isPushedByFluid() {
    return true;
  }

  public static double getViewScale() {
    return viewScale;
  }

  public static void setViewScale(double renderDistWeight) {
    viewScale = renderDistWeight;
  }

  @Override
  public Component getDisplayName() {
    return PlayerTeam.formatNameForTeam(this.getTeam(), this.getName())
      .withStyle(arg -> arg.withHoverEvent(this.createHoverEvent()).withInsertion(this.getStringUUID()));
  }

  public void setCustomName(@Nullable Component name) {
    this.entityData.set(DATA_CUSTOM_NAME, Optional.ofNullable(name));
  }

  @Nullable
  @Override
  public Component getCustomName() {
    return this.entityData.get(DATA_CUSTOM_NAME).orElse(null);
  }

  @Override
  public boolean hasCustomName() {
    return this.entityData.get(DATA_CUSTOM_NAME).isPresent();
  }

  public void setCustomNameVisible(boolean alwaysRenderNameTag) {
    this.entityData.set(DATA_CUSTOM_NAME_VISIBLE, alwaysRenderNameTag);
  }

  public boolean isCustomNameVisible() {
    return this.entityData.get(DATA_CUSTOM_NAME_VISIBLE);
  }

  private void teleportPassengers() {
    this.getSelfAndPassengers().forEach(entity -> {
      UnmodifiableIterator var1 = entity.passengers.iterator();

      while (var1.hasNext()) {
        Entity lv = (Entity)var1.next();
        entity.positionRider(lv, Entity::moveTo);
      }
    });
  }

  public void teleportRelative(double dx, double dy, double dz) {
    this.teleportTo(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
  }

  public boolean shouldShowName() {
    return this.isCustomNameVisible();
  }

  @Override
  public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> newData) {
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> dataAccessor) {
    if (DATA_POSE.equals(dataAccessor)) {
      this.refreshDimensions();
    }
  }

  @Deprecated
  protected void fixupDimensions() {
    Pose lv = this.getPose();
    EntityDimensions lv2 = this.getDimensions(lv);
    this.dimensions = lv2;
    this.eyeHeight = lv2.eyeHeight();
  }

  public void refreshDimensions() {
    EntityDimensions lv = this.dimensions;
    Pose lv2 = this.getPose();
    EntityDimensions lv3 = this.getDimensions(lv2);
    this.dimensions = lv3;
    this.eyeHeight = lv3.eyeHeight();
    this.reapplyPosition();
    boolean bl = (double)lv3.width() <= 4.0 && (double)lv3.height() <= 4.0;
    if (!this.level.isClientSide
      && !this.firstTick
      && !this.noPhysics
      && bl
      && (lv3.width() > lv.width() || lv3.height() > lv.height())
      && !(this instanceof Player)) {
      this.fudgePositionAfterSizeChange(lv);
    }
  }

  public boolean fudgePositionAfterSizeChange(EntityDimensions dimensions) {
    EntityDimensions lv = this.getDimensions(this.getPose());
    Vec3 lv2 = this.position().add(0.0, (double)dimensions.height() / 2.0, 0.0);
    double d = (double)Math.max(0.0F, lv.width() - dimensions.width()) + 1.0E-6;
    double e = (double)Math.max(0.0F, lv.height() - dimensions.height()) + 1.0E-6;
    VoxelShape lv3 = Shapes.create(AABB.ofSize(lv2, d, e, d));
    Optional<Vec3> optional = this.level.findFreePosition(this, lv3, lv2, (double)lv.width(), (double)lv.height(), (double)lv.width());
    if (optional.isPresent()) {
      this.setPos(optional.get().add(0.0, (double)(-lv.height()) / 2.0, 0.0));
      return true;
    } else {
      if (lv.width() > dimensions.width() && lv.height() > dimensions.height()) {
        VoxelShape lv4 = Shapes.create(AABB.ofSize(lv2, d, 1.0E-6, d));
        Optional<Vec3> optional2 = this.level.findFreePosition(this, lv4, lv2, (double)lv.width(), (double)dimensions.height(), (double)lv.width());
        if (optional2.isPresent()) {
          this.setPos(optional2.get().add(0.0, (double)(-dimensions.height()) / 2.0 + 1.0E-6, 0.0));
          return true;
        }
      }

      return false;
    }
  }

  public Direction getDirection() {
    return Direction.fromYRot((double)this.getYRot());
  }

  public Direction getMotionDirection() {
    return this.getDirection();
  }

  protected HoverEvent createHoverEvent() {
    return new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new HoverEvent.EntityTooltipInfo(this.getType(), this.getUUID(), this.getName()));
  }

  @Override
  public final AABB getBoundingBox() {
    return this.bb;
  }

  public AABB getBoundingBoxForCulling() {
    return this.getBoundingBox();
  }

  public final void setBoundingBox(AABB bb) {
    this.bb = bb;
  }

  public final float getEyeHeight(Pose pose) {
    return this.getDimensions(pose).eyeHeight();
  }

  public final float getEyeHeight() {
    return this.eyeHeight;
  }

  public Vec3 getLeashOffset(float partialTick) {
    return this.getLeashOffset();
  }

  protected Vec3 getLeashOffset() {
    return new Vec3(0.0, (double)this.getEyeHeight(), (double)(this.getBbWidth() * 0.4F));
  }

  public SlotAccess getSlot(int slot) {
    return SlotAccess.NULL;
  }

  @Override
  public void sendSystemMessage(Component component) {
  }

  public Level getCommandSenderWorld() {
    return this.level();
  }

  @Nullable
  public MinecraftServer getServer() {
    return this.level().getServer();
  }

  public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
    return InteractionResult.PASS;
  }

  public boolean ignoreExplosion(Explosion explosion) {
    return false;
  }

  public float rotate(Rotation transformRotation) {
    float f = Mth.wrapDegrees(this.getYRot());
    switch (transformRotation) {
      case CLOCKWISE_180:
        return f + 180.0F;
      case COUNTERCLOCKWISE_90:
        return f + 270.0F;
      case CLOCKWISE_90:
        return f + 90.0F;
      default:
        return f;
    }
  }

  public float mirror(Mirror transformMirror) {
    float f = Mth.wrapDegrees(this.getYRot());
    switch (transformMirror) {
      case FRONT_BACK:
        return -f;
      case LEFT_RIGHT:
        return 180.0F - f;
      default:
        return f;
    }
  }

  public boolean onlyOpCanSetNbt() {
    return false;
  }

  public ProjectileDeflection deflection(Projectile projectile) {
    return this.getType().is(EntityTypeTags.DEFLECTS_PROJECTILES) ? ProjectileDeflection.REVERSE : ProjectileDeflection.NONE;
  }

  @Nullable
  public LivingEntity getControllingPassenger() {
    return null;
  }

  public final boolean hasControllingPassenger() {
    return this.getControllingPassenger() != null;
  }

  public final List<Entity> getPassengers() {
    return this.passengers;
  }

  @Nullable
  public Entity getFirstPassenger() {
    return this.passengers.isEmpty() ? null : (Entity)this.passengers.get(0);
  }

  public boolean hasPassenger(Entity entity) {
    return this.passengers.contains(entity);
  }

  public boolean hasPassenger(Predicate<Entity> predicate) {
    UnmodifiableIterator var2 = this.passengers.iterator();

    while (var2.hasNext()) {
      Entity lv = (Entity)var2.next();
      if (predicate.test(lv)) {
        return true;
      }
    }

    return false;
  }

  private Stream<Entity> getIndirectPassengersStream() {
    return this.passengers.stream().flatMap(Entity::getSelfAndPassengers);
  }

  @Override
  public Stream<Entity> getSelfAndPassengers() {
    return Stream.concat(Stream.of(this), this.getIndirectPassengersStream());
  }

  @Override
  public Stream<Entity> getPassengersAndSelf() {
    return Stream.concat(this.passengers.stream().flatMap(Entity::getPassengersAndSelf), Stream.of(this));
  }

  public Iterable<Entity> getIndirectPassengers() {
    return () -> this.getIndirectPassengersStream().iterator();
  }

  public int countPlayerPassengers() {
    return (int)this.getIndirectPassengersStream().filter(entity -> entity instanceof Player).count();
  }

  public boolean hasExactlyOnePlayerPassenger() {
    return this.countPlayerPassengers() == 1;
  }

  public Entity getRootVehicle() {
    Entity lv = this;

    while (lv.isPassenger()) {
      lv = lv.getVehicle();
    }

    return lv;
  }

  public boolean isPassengerOfSameVehicle(Entity entity) {
    return this.getRootVehicle() == entity.getRootVehicle();
  }

  public boolean hasIndirectPassenger(Entity entity) {
    if (!entity.isPassenger()) {
      return false;
    } else {
      Entity lv = entity.getVehicle();
      return lv == this ? true : this.hasIndirectPassenger(lv);
    }
  }

  public boolean isControlledByLocalInstance() {
    return this.getControllingPassenger() instanceof Player lv ? lv.isLocalPlayer() : this.isEffectiveAi();
  }

  public boolean isEffectiveAi() {
    return !this.level().isClientSide;
  }

  protected static Vec3 getCollisionHorizontalEscapeVector(double vehicleWidth, double passengerWidth, float yRot) {
    double g = (vehicleWidth + passengerWidth + 1.0E-5F) / 2.0;
    float h = -Mth.sin(yRot * (float) (Math.PI / 180.0));
    float i = Mth.cos(yRot * (float) (Math.PI / 180.0));
    float j = Math.max(Math.abs(h), Math.abs(i));
    return new Vec3((double)h * g / (double)j, 0.0, (double)i * g / (double)j);
  }

  public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
    return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
  }

  @Nullable
  public Entity getVehicle() {
    return this.vehicle;
  }

  @Nullable
  public Entity getControlledVehicle() {
    return this.vehicle != null && this.vehicle.getControllingPassenger() == this ? this.vehicle : null;
  }

  public PushReaction getPistonPushReaction() {
    return PushReaction.NORMAL;
  }

  public SoundSource getSoundSource() {
    return SoundSource.NEUTRAL;
  }

  protected int getFireImmuneTicks() {
    return 1;
  }

  protected int getPermissionLevel() {
    return 0;
  }

  public void lookAt(EntityAnchorArgument.Anchor anchor, Vec3 target) {
    Vec3 lv = anchor.apply(this);
    double d = target.x - lv.x;
    double e = target.y - lv.y;
    double f = target.z - lv.z;
    double g = Math.sqrt(d * d + f * f);
    this.setXRot(Mth.wrapDegrees((float)(-(Mth.atan2(e, g) * 180.0F / (float)Math.PI))));
    this.setYRot(Mth.wrapDegrees((float)(Mth.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F));
    this.setYHeadRot(this.getYRot());
    this.xRotO = this.getXRot();
    this.yRotO = this.getYRot();
  }

  public float getPreciseBodyRotation(float partialTick) {
    return Mth.lerp(partialTick, this.yRotO, this.yRot);
  }

  public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> fluidTag, double motionScale) {
    if (this.touchingUnloadedChunk()) {
      return false;
    } else {
      AABB lv = this.getBoundingBox().deflate(0.001);
      int i = Mth.floor(lv.minX);
      int j = Mth.ceil(lv.maxX);
      int k = Mth.floor(lv.minY);
      int l = Mth.ceil(lv.maxY);
      int m = Mth.floor(lv.minZ);
      int n = Mth.ceil(lv.maxZ);
      double e = 0.0;
      boolean bl = this.isPushedByFluid();
      boolean bl2 = false;
      Vec3 lv2 = Vec3.ZERO;
      int o = 0;
      BlockPos.MutableBlockPos lv3 = new BlockPos.MutableBlockPos();

      for (int p = i; p < j; p++) {
        for (int q = k; q < l; q++) {
          for (int r = m; r < n; r++) {
            lv3.set(p, q, r);
            FluidState lv4 = this.level().getFluidState(lv3);
            if (lv4.is(fluidTag)) {
              double f = (double)((float)q + lv4.getHeight(this.level(), lv3));
              if (f >= lv.minY) {
                bl2 = true;
                e = Math.max(f - lv.minY, e);
                if (bl) {
                  Vec3 lv5 = lv4.getFlow(this.level(), lv3);
                  if (e < 0.4) {
                    lv5 = lv5.scale(e);
                  }

                  lv2 = lv2.add(lv5);
                  o++;
                }
              }
            }
          }
        }
      }

      if (lv2.length() > 0.0) {
        if (o > 0) {
          lv2 = lv2.scale(1.0 / (double)o);
        }

        if (!(this instanceof Player)) {
          lv2 = lv2.normalize();
        }

        Vec3 lv6 = this.getDeltaMovement();
        lv2 = lv2.scale(motionScale);
        double g = 0.003;
        if (Math.abs(lv6.x) < 0.003 && Math.abs(lv6.z) < 0.003 && lv2.length() < 0.0045000000000000005) {
          lv2 = lv2.normalize().scale(0.0045000000000000005);
        }

        this.setDeltaMovement(this.getDeltaMovement().add(lv2));
      }

      this.fluidHeight.put(fluidTag, e);
      return bl2;
    }
  }

  public boolean touchingUnloadedChunk() {
    AABB lv = this.getBoundingBox().inflate(1.0);
    int i = Mth.floor(lv.minX);
    int j = Mth.ceil(lv.maxX);
    int k = Mth.floor(lv.minZ);
    int l = Mth.ceil(lv.maxZ);
    return !this.level().hasChunksAt(i, k, j, l);
  }

  public double getFluidHeight(TagKey<Fluid> fluidTag) {
    return this.fluidHeight.getDouble(fluidTag);
  }

  public double getFluidJumpThreshold() {
    return (double)this.getEyeHeight() < 0.4 ? 0.0 : 0.4;
  }

  public final float getBbWidth() {
    return this.dimensions.width();
  }

  public final float getBbHeight() {
    return this.dimensions.height();
  }

  public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
    return new ClientboundAddEntityPacket(this, entity);
  }

  public EntityDimensions getDimensions(Pose pose) {
    return this.type.getDimensions();
  }

  public final EntityAttachments getAttachments() {
    return this.dimensions.attachments();
  }

  public Vec3 position() {
    return this.position;
  }

  public Vec3 trackingPosition() {
    return this.position();
  }

  @Override
  public BlockPos blockPosition() {
    return this.blockPosition;
  }

  public BlockState getInBlockState() {
    if (this.inBlockState == null) {
      this.inBlockState = this.level().getBlockState(this.blockPosition());
    }

    return this.inBlockState;
  }

  public ChunkPos chunkPosition() {
    return this.chunkPosition;
  }

  public Vec3 getDeltaMovement() {
    return this.deltaMovement;
  }

  public void setDeltaMovement(Vec3 deltaMovement) {
    this.deltaMovement = deltaMovement;
  }

  public void addDeltaMovement(Vec3 addend) {
    this.setDeltaMovement(this.getDeltaMovement().add(addend));
  }

  public void setDeltaMovement(double x, double y, double z) {
    this.setDeltaMovement(new Vec3(x, y, z));
  }

  public final int getBlockX() {
    return this.blockPosition.getX();
  }

  public final double getX() {
    return this.position.x;
  }

  public double getX(double scale) {
    return this.position.x + (double)this.getBbWidth() * scale;
  }

  public double getRandomX(double scale) {
    return this.getX((2.0 * this.random.nextDouble() - 1.0) * scale);
  }

  public final int getBlockY() {
    return this.blockPosition.getY();
  }

  public final double getY() {
    return this.position.y;
  }

  public double getY(double scale) {
    return this.position.y + (double)this.getBbHeight() * scale;
  }

  public double getRandomY() {
    return this.getY(this.random.nextDouble());
  }

  public double getEyeY() {
    return this.position.y + (double)this.eyeHeight;
  }

  public final int getBlockZ() {
    return this.blockPosition.getZ();
  }

  public final double getZ() {
    return this.position.z;
  }

  public double getZ(double scale) {
    return this.position.z + (double)this.getBbWidth() * scale;
  }

  public double getRandomZ(double scale) {
    return this.getZ((2.0 * this.random.nextDouble() - 1.0) * scale);
  }

  public final void setPosRaw(double x, double y, double z) {
    if (this.position.x != x || this.position.y != y || this.position.z != z) {
      this.position = new Vec3(x, y, z);
      int i = Mth.floor(x);
      int j = Mth.floor(y);
      int k = Mth.floor(z);
      if (i != this.blockPosition.getX() || j != this.blockPosition.getY() || k != this.blockPosition.getZ()) {
        this.blockPosition = new BlockPos(i, j, k);
        this.inBlockState = null;
        if (SectionPos.blockToSectionCoord(i) != this.chunkPosition.x || SectionPos.blockToSectionCoord(k) != this.chunkPosition.z) {
          this.chunkPosition = new ChunkPos(this.blockPosition);
        }
      }

      this.levelCallback.onMove();
    }
  }

  public void checkDespawn() {
  }

  public Vec3 getRopeHoldPosition(float partialTicks) {
    return this.getPosition(partialTicks).add(0.0, (double)this.eyeHeight * 0.7, 0.0);
  }

  public void recreateFromPacket(ClientboundAddEntityPacket packet) {
    int i = packet.getId();
    double d = packet.getX();
    double e = packet.getY();
    double f = packet.getZ();
    this.syncPacketPositionCodec(d, e, f);
    this.moveTo(d, e, f);
    this.setXRot(packet.getXRot());
    this.setYRot(packet.getYRot());
    this.setId(i);
    this.setUUID(packet.getUUID());
  }

  @Nullable
  public ItemStack getPickResult() {
    return null;
  }

  public void setIsInPowderSnow(boolean isInPowderSnow) {
    this.isInPowderSnow = isInPowderSnow;
  }

  public boolean canFreeze() {
    return !this.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);
  }

  public boolean isFreezing() {
    return (this.isInPowderSnow || this.wasInPowderSnow) && this.canFreeze();
  }

  public float getYRot() {
    return this.yRot;
  }

  public float getVisualRotationYInDegrees() {
    return this.getYRot();
  }

  public void setYRot(float yRot) {
    if (!Float.isFinite(yRot)) {
      Util.logAndPauseIfInIde("Invalid entity rotation: " + yRot + ", discarding.");
    } else {
      this.yRot = yRot;
    }
  }

  public float getXRot() {
    return this.xRot;
  }

  public void setXRot(float xRot) {
    if (!Float.isFinite(xRot)) {
      Util.logAndPauseIfInIde("Invalid entity rotation: " + xRot + ", discarding.");
    } else {
      this.xRot = xRot;
    }
  }

  public boolean canSprint() {
    return false;
  }

  public float maxUpStep() {
    return 0.0F;
  }

  public void onExplosionHit(@Nullable Entity entity) {
  }

  public final boolean isRemoved() {
    return this.removalReason != null;
  }

  @Nullable
  public Entity.RemovalReason getRemovalReason() {
    return this.removalReason;
  }

  @Override
  public final void setRemoved(Entity.RemovalReason removalReason) {
    if (this.removalReason == null) {
      this.removalReason = removalReason;
    }

    if (this.removalReason.shouldDestroy()) {
      this.stopRiding();
    }

    this.getPassengers().forEach(Entity::stopRiding);
    this.levelCallback.onRemove(removalReason);
  }

  protected void unsetRemoved() {
    this.removalReason = null;
  }

  @Override
  public void setLevelCallback(EntityInLevelCallback levelCallback) {
    this.levelCallback = levelCallback;
  }

  @Override
  public boolean shouldBeSaved() {
    if (this.removalReason != null && !this.removalReason.shouldSave()) {
      return false;
    } else {
      return this.isPassenger() ? false : !this.isVehicle() || !this.hasExactlyOnePlayerPassenger();
    }
  }

  @Override
  public boolean isAlwaysTicking() {
    return false;
  }

  public boolean mayInteract(Level level, BlockPos pos) {
    return true;
  }

  public Level level() {
    return this.level;
  }

  protected void setLevel(Level level) {
    this.level = level;
  }

  public DamageSources damageSources() {
    return this.level().damageSources();
  }

  public RegistryAccess registryAccess() {
    return this.level().registryAccess();
  }

  protected void lerpPositionAndRotationStep(int steps, double targetX, double targetY, double targetZ, double targetYRot, double targetXRot) {
    double j = 1.0 / (double)steps;
    double k = Mth.lerp(j, this.getX(), targetX);
    double l = Mth.lerp(j, this.getY(), targetY);
    double m = Mth.lerp(j, this.getZ(), targetZ);
    float n = (float)Mth.rotLerp(j, (double)this.getYRot(), targetYRot);
    float o = (float)Mth.lerp(j, (double)this.getXRot(), targetXRot);
    this.setPos(k, l, m);
    this.setRot(n, o);
  }

  public RandomSource getRandom() {
    return this.random;
  }

  public Vec3 getKnownMovement() {
    if (this.getControllingPassenger() instanceof Player lv && this.isAlive()) {
      return lv.getKnownMovement();
    }

    return this.getDeltaMovement();
  }

  @Nullable
  public ItemStack getWeaponItem() {
    return null;
  }

  @FunctionalInterface
  public interface MoveFunction {
    void accept(Entity arg, double d, double e, double f);
  }

  public static enum MovementEmission {
    NONE(false, false),
    SOUNDS(true, false),
    EVENTS(false, true),
    ALL(true, true);

    final boolean sounds;
    final boolean events;

    private MovementEmission(final boolean bl, final boolean bl2) {
      this.sounds = bl;
      this.events = bl2;
    }

    public boolean emitsAnything() {
      return this.events || this.sounds;
    }

    public boolean emitsEvents() {
      return this.events;
    }

    public boolean emitsSounds() {
      return this.sounds;
    }
  }

  public static enum RemovalReason {
    KILLED(true, false),
    DISCARDED(true, false),
    UNLOADED_TO_CHUNK(false, true),
    UNLOADED_WITH_PLAYER(false, false),
    CHANGED_DIMENSION(false, false);

    private final boolean destroy;
    private final boolean save;

    private RemovalReason(final boolean bl, final boolean bl2) {
      this.destroy = bl;
      this.save = bl2;
    }

    public boolean shouldDestroy() {
      return this.destroy;
    }

    public boolean shouldSave() {
      return this.save;
    }
  }
}
