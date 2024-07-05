package com.soulfiremc.server.protocol.bot.state.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.logging.LogUtils;
import com.soulfiremc.server.data.FluidTags;
import com.soulfiremc.server.data.FluidType;
import com.soulfiremc.server.data.TagKey;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.BlockUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public abstract class Entity {
  private static final Logger LOGGER = LogUtils.getLogger();
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

  public boolean isColliding(BlockPos arg, BlockState arg2) {
    VoxelShape lv = arg2.getCollisionShape(this.level(), arg, CollisionContext.of(this));
    VoxelShape lv2 = lv.move((double)arg.getX(), (double)arg.getY(), (double)arg.getZ());
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

  public void syncPacketPositionCodec(double d, double e, double f) {
    this.packetPositionCodec.setBase(new Vec3(d, e, f));
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

  public void setId(int i) {
    this.id = i;
  }

  public Set<String> getTags() {
    return this.tags;
  }

  public boolean addTag(String string) {
    return this.tags.size() >= 1024 ? false : this.tags.add(string);
  }

  public boolean removeTag(String string) {
    return this.tags.remove(string);
  }

  public void kill() {
    this.remove(Entity.RemovalReason.KILLED);
    this.gameEvent(GameEvent.ENTITY_DIE);
  }

  public final void discard() {
    this.remove(Entity.RemovalReason.DISCARDED);
  }

  protected abstract void defineSynchedData(SynchedEntityData.Builder arg);

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

  public void remove(Entity.RemovalReason arg) {
    this.setRemoved(arg);
  }

  public void onClientRemoval() {
  }

  public void setPose(Pose arg) {
    this.entityData.set(DATA_POSE, arg);
  }

  public Pose getPose() {
    return this.entityData.get(DATA_POSE);
  }

  public boolean hasPose(Pose arg) {
    return this.getPose() == arg;
  }

  public boolean closerThan(Entity arg, double d) {
    return this.position().closerThan(arg.position(), d);
  }

  public boolean closerThan(Entity arg, double d, double e) {
    double f = arg.getX() - this.getX();
    double g = arg.getY() - this.getY();
    double h = arg.getZ() - this.getZ();
    return Mth.lengthSquared(f, h) < Mth.square(d) && Mth.square(g) < Mth.square(e);
  }

  protected void setRot(float f, float g) {
    this.setYRot(f % 360.0F);
    this.setXRot(g % 360.0F);
  }

  public final void setPos(Vec3 arg) {
    this.setPos(arg.x(), arg.y(), arg.z());
  }

  public void setPos(double d, double e, double f) {
    this.setPosRaw(d, e, f);
    this.setBoundingBox(this.makeBoundingBox());
  }

  protected AABB makeBoundingBox() {
    return this.dimensions.makeBoundingBox(this.position);
  }

  protected void reapplyPosition() {
    this.setPos(this.position.x, this.position.y, this.position.z);
  }

  public void turn(double d, double e) {
    float f = (float)e * 0.15F;
    float g = (float)d * 0.15F;
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
    this.handlePortal();
    if (this.canSpawnSprintParticle()) {
      this.spawnSprintParticle();
    }

    this.wasInPowderSnow = this.isInPowderSnow;
    this.isInPowderSnow = false;
    this.updateInWaterStateAndDoFluidPushing();
    this.updateFluidOnEyes();
    this.updateSwimming();
    this.clearFire();

    if (this.isInLava()) {
      this.lavaHurt();
      this.fallDistance *= 0.5F;
    }

    this.checkBelowWorld();

    this.firstTick = false;
  }

  public void setSharedFlagOnFire(boolean bl) {
    this.setSharedFlag(0, bl || this.hasVisualFire);
  }

  public void checkBelowWorld() {
    if (this.getY() < (double)(this.level().getMinBuildHeight() - 64)) {
      this.onBelowWorld();
    }
  }

  public void setPortalCooldown() {
    this.portalCooldown = this.getDimensionChangingDelay();
  }

  public void setPortalCooldown(int i) {
    this.portalCooldown = i;
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
      if (this.hurt(this.damageSources().lava(), 4.0F)) {
      }
    }
  }

  public final void igniteForSeconds(float f) {
    this.igniteForTicks(Mth.floor(f * 20.0F));
  }

  public void igniteForTicks(int i) {
    if (this.remainingFireTicks < i) {
      this.setRemainingFireTicks(i);
    }
  }

  public void setRemainingFireTicks(int i) {
    this.remainingFireTicks = i;
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

  public boolean isFree(double d, double e, double f) {
    return this.isFree(this.getBoundingBox().move(d, e, f));
  }

  private boolean isFree(AABB arg) {
    return this.level().noCollision(this, arg) && !this.level().containsAnyLiquid(arg);
  }

  public void setOnGround(boolean bl) {
    this.onGround = bl;
    this.checkSupportingBlock(bl, null);
  }

  public void setOnGroundWithMovement(boolean bl, Vec3 arg) {
    this.onGround = bl;
    this.checkSupportingBlock(bl, arg);
  }

  public boolean isSupportedBy(BlockPos arg) {
    return this.mainSupportingBlockPos.isPresent() && this.mainSupportingBlockPos.get().equals(arg);
  }

  protected void checkSupportingBlock(boolean bl, @Nullable Vec3 arg) {
    if (bl) {
      AABB lv = this.getBoundingBox();
      AABB lv2 = new AABB(lv.minX, lv.minY - 1.0E-6, lv.minZ, lv.maxX, lv.minY, lv.maxZ);
      Optional<BlockPos> optional = this.level.findSupportingBlock(this, lv2);
      if (optional.isPresent() || this.onGroundNoBlocks) {
        this.mainSupportingBlockPos = optional;
      } else if (arg != null) {
        AABB lv3 = lv2.move(-arg.x, 0.0, -arg.z);
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

  public void move(MoverType arg, Vec3 arg2) {
    if (this.noPhysics) {
      this.setPos(this.getX() + arg2.x, this.getY() + arg2.y, this.getZ() + arg2.z);
    } else {
      this.wasOnFire = this.isOnFire();
      if (arg == MoverType.PISTON) {
        arg2 = this.limitPistonMovement(arg2);
        if (arg2.equals(Vec3.ZERO)) {
          return;
        }
      }

      if (this.stuckSpeedMultiplier.lengthSqr() > 1.0E-7) {
        arg2 = arg2.multiply(this.stuckSpeedMultiplier);
        this.stuckSpeedMultiplier = Vec3.ZERO;
        this.setDeltaMovement(Vec3.ZERO);
      }

      arg2 = this.maybeBackOffFromEdge(arg2, arg);
      Vec3 lv = this.collide(arg2);
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

      boolean bl = !Mth.equal(arg2.x, lv.x);
      boolean bl2 = !Mth.equal(arg2.z, lv.z);
      this.horizontalCollision = bl || bl2;
      this.verticalCollision = arg2.y != lv.y;
      this.verticalCollisionBelow = this.verticalCollision && arg2.y < 0.0;
      if (this.horizontalCollision) {
        this.minorHorizontalCollision = this.isHorizontalCollisionMinor(lv);
      } else {
        this.minorHorizontalCollision = false;
      }

      this.setOnGroundWithMovement(this.verticalCollisionBelow, lv);
      BlockPos lv3 = this.getOnPosLegacy();
      BlockState lv4 = this.level().getBlockState(lv3);
      this.checkFallDamage(lv.y, this.onGround(), lv4, lv3);
      if (!this.isRemoved()) {
        if (this.horizontalCollision) {
          Vec3 lv5 = this.getDeltaMovement();
          this.setDeltaMovement(bl ? 0.0 : lv5.x, lv5.y, bl2 ? 0.0 : lv5.z);
        }

        Block lv6 = lv4.getBlock();
        if (arg2.y != lv.y) {
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
            boolean bl5 = this.vibrationAndSoundEffectsFromBlock(lv3, lv4, lv7.emitsSounds(), bl4, arg2);
            if (!bl4) {
              bl5 |= this.vibrationAndSoundEffectsFromBlock(lv8, lv9, false, lv7.emitsEvents(), arg2);
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
        if (this.level().getBlockStatesIfLoaded(this.getBoundingBox().deflate(1.0E-6)).noneMatch(argx -> argx.is(BlockTags.FIRE) || argx.is(Blocks.LAVA))) {
          if (this.remainingFireTicks <= 0) {
            this.setRemainingFireTicks(-this.getFireImmuneTicks());
          }

          if (this.wasOnFire && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
            this.playEntityOnFireExtinguishedSound();
          }
        }

        if (this.isOnFire() && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
          this.setRemainingFireTicks(-this.getFireImmuneTicks());
        }
      }
    }
  }

  private boolean isStateClimbable(BlockState arg) {
    return arg.is(BlockTags.CLIMBABLE) || arg.is(Blocks.POWDER_SNOW);
  }

  private boolean vibrationAndSoundEffectsFromBlock(BlockPos arg, BlockState arg2, boolean bl, boolean bl2, Vec3 arg3) {
    if (arg2.isAir()) {
      return false;
    } else {
      boolean bl3 = this.isStateClimbable(arg2);
      if ((this.onGround() || bl3 || this.isCrouching() && arg3.y == 0.0 || this.isOnRails()) && !this.isSwimming()) {
        if (bl) {
          this.walkingStepSound(arg, arg2);
        }

        if (bl2) {
          this.level().gameEvent(GameEvent.STEP, this.position(), GameEvent.Context.of(this, arg2));
        }

        return true;
      } else {
        return false;
      }
    }
  }

  protected boolean isHorizontalCollisionMinor(Vec3 arg) {
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

  protected BlockPos getOnPos(float f) {
    if (this.mainSupportingBlockPos.isPresent()) {
      BlockPos lv = this.mainSupportingBlockPos.get();
      if (!(f > 1.0E-5F)) {
        return lv;
      } else {
        BlockState lv2 = this.level().getBlockState(lv);
        return (!((double)f <= 0.5) || !lv2.is(BlockTags.FENCES)) && !lv2.is(BlockTags.WALLS) && !(lv2.getBlock() instanceof FenceGateBlock)
          ? lv.atY(Mth.floor(this.position.y - (double)f))
          : lv;
      }
    } else {
      int i = Mth.floor(this.position.x);
      int j = Mth.floor(this.position.y - (double)f);
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

  protected Vec3 maybeBackOffFromEdge(Vec3 arg, MoverType arg2) {
    return arg;
  }

  protected Vec3 limitPistonMovement(Vec3 arg) {
    if (arg.lengthSqr() <= 1.0E-7) {
      return arg;
    } else {
      long l = this.level().getGameTime();
      if (l != this.pistonDeltasGameTime) {
        Arrays.fill(this.pistonDeltas, 0.0);
        this.pistonDeltasGameTime = l;
      }

      if (arg.x != 0.0) {
        double d = this.applyPistonMovementRestriction(Direction.Axis.X, arg.x);
        return Math.abs(d) <= 1.0E-5F ? Vec3.ZERO : new Vec3(d, 0.0, 0.0);
      } else if (arg.y != 0.0) {
        double d = this.applyPistonMovementRestriction(Direction.Axis.Y, arg.y);
        return Math.abs(d) <= 1.0E-5F ? Vec3.ZERO : new Vec3(0.0, d, 0.0);
      } else if (arg.z != 0.0) {
        double d = this.applyPistonMovementRestriction(Direction.Axis.Z, arg.z);
        return Math.abs(d) <= 1.0E-5F ? Vec3.ZERO : new Vec3(0.0, 0.0, d);
      } else {
        return Vec3.ZERO;
      }
    }
  }

  private double applyPistonMovementRestriction(Direction.Axis arg, double d) {
    int i = arg.ordinal();
    double e = Mth.clamp(d + this.pistonDeltas[i], -0.51, 0.51);
    d = e - this.pistonDeltas[i];
    this.pistonDeltas[i] = e;
    return d;
  }

  private Vec3 collide(Vec3 arg) {
    AABB lv = this.getBoundingBox();
    List<VoxelShape> list = this.level().getEntityCollisions(this, lv.expandTowards(arg));
    Vec3 lv2 = arg.lengthSqr() == 0.0 ? arg : collideBoundingBox(this, arg, lv, this.level(), list);
    boolean bl = arg.x != lv2.x;
    boolean bl2 = arg.y != lv2.y;
    boolean bl3 = arg.z != lv2.z;
    boolean bl4 = bl2 && arg.y < 0.0;
    if (this.maxUpStep() > 0.0F && (bl4 || this.onGround()) && (bl || bl3)) {
      AABB lv3 = bl4 ? lv.move(0.0, lv2.y, 0.0) : lv;
      AABB lv4 = lv3.expandTowards(arg.x, (double)this.maxUpStep(), arg.z);
      if (!bl4) {
        lv4 = lv4.expandTowards(0.0, -1.0E-5F, 0.0);
      }

      List<VoxelShape> list2 = collectColliders(this, this.level, list, lv4);
      float f = (float)lv2.y;
      float[] fs = collectCandidateStepUpHeights(lv3, list2, this.maxUpStep(), f);

      for (float g : fs) {
        Vec3 lv5 = collideWithShapes(new Vec3(arg.x, (double)g, arg.z), lv3, list2);
        if (lv5.horizontalDistanceSqr() > lv2.horizontalDistanceSqr()) {
          double d = lv.minY - lv3.minY;
          return lv5.add(0.0, -d, 0.0);
        }
      }
    }

    return lv2;
  }

  private static float[] collectCandidateStepUpHeights(AABB arg, List<VoxelShape> list, float f, float g) {
    FloatSet floatSet = new FloatArraySet(4);

    for (VoxelShape lv : list) {
      DoubleList doubleList = lv.getCoords(Direction.Axis.Y);
      DoubleListIterator var8 = doubleList.iterator();

      while (var8.hasNext()) {
        double d = (Double)var8.next();
        float h = (float)(d - arg.minY);
        if (!(h < 0.0F) && h != g) {
          if (h > f) {
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

  public static Vec3 collideBoundingBox(@Nullable Entity arg, Vec3 arg2, AABB arg3, Level arg4, List<VoxelShape> list) {
    List<VoxelShape> list2 = collectColliders(arg, arg4, list, arg3.expandTowards(arg2));
    return collideWithShapes(arg2, arg3, list2);
  }

  private static List<VoxelShape> collectColliders(@Nullable Entity arg, Level arg2, List<VoxelShape> list, AABB arg3) {
    Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(list.size() + 1);
    if (!list.isEmpty()) {
      builder.addAll(list);
    }

    WorldBorder lv = arg2.getWorldBorder();
    boolean bl = arg != null && lv.isInsideCloseToBorder(arg, arg3);
    if (bl) {
      builder.add(lv.getCollisionShape());
    }

    builder.addAll(arg2.getBlockCollisions(arg, arg3));
    return builder.build();
  }

  private static Vec3 collideWithShapes(Vec3 arg, AABB arg2, List<VoxelShape> list) {
    if (list.isEmpty()) {
      return arg;
    } else {
      double d = arg.x;
      double e = arg.y;
      double f = arg.z;
      if (e != 0.0) {
        e = Shapes.collide(Direction.Axis.Y, arg2, list, e);
        if (e != 0.0) {
          arg2 = arg2.move(0.0, e, 0.0);
        }
      }

      boolean bl = Math.abs(d) < Math.abs(f);
      if (bl && f != 0.0) {
        f = Shapes.collide(Direction.Axis.Z, arg2, list, f);
        if (f != 0.0) {
          arg2 = arg2.move(0.0, 0.0, f);
        }
      }

      if (d != 0.0) {
        d = Shapes.collide(Direction.Axis.X, arg2, list, d);
        if (!bl && d != 0.0) {
          arg2 = arg2.move(d, 0.0, 0.0);
        }
      }

      if (!bl && f != 0.0) {
        f = Shapes.collide(Direction.Axis.Z, arg2, list, f);
      }

      return new Vec3(d, e, f);
    }
  }

  protected float nextStep() {
    return (float)((int)this.moveDist + 1);
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

  protected void onInsideBlock(BlockState arg) {
  }

  public void gameEvent(Holder<GameEvent> arg, @Nullable Entity arg2) {
    this.level().gameEvent(arg2, arg, this.position);
  }

  public void gameEvent(Holder<GameEvent> arg) {
    this.gameEvent(arg, this);
  }

  private void walkingStepSound(BlockPos arg, BlockState arg2) {
    this.playStepSound(arg, arg2);
    if (this.shouldPlayAmethystStepSound(arg2)) {
      this.playAmethystStepSound();
    }
  }

  protected void waterSwimSound() {
    Entity lv = Objects.requireNonNullElse(this.getControllingPassenger(), this);
    float f = lv == this ? 0.35F : 0.4F;
    Vec3 lv2 = lv.getDeltaMovement();
    float g = Math.min(1.0F, (float)Math.sqrt(lv2.x * lv2.x * 0.2F + lv2.y * lv2.y + lv2.z * lv2.z * 0.2F) * f);
    this.playSwimSound(g);
  }

  protected BlockPos getPrimaryStepSoundBlockPos(BlockPos arg) {
    BlockPos lv = arg.above();
    BlockState lv2 = this.level().getBlockState(lv);
    return !lv2.is(BlockTags.INSIDE_STEP_SOUND_BLOCKS) && !lv2.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS) ? arg : lv;
  }

  protected void playCombinationStepSounds(BlockState arg, BlockState arg2) {
    SoundType lv = arg.getSoundType();
    this.playSound(lv.getStepSound(), lv.getVolume() * 0.15F, lv.getPitch());
    this.playMuffledStepSound(arg2);
  }

  protected void playMuffledStepSound(BlockState arg) {
    SoundType lv = arg.getSoundType();
    this.playSound(lv.getStepSound(), lv.getVolume() * 0.05F, lv.getPitch() * 0.8F);
  }

  protected void playStepSound(BlockPos arg, BlockState arg2) {
    SoundType lv = arg2.getSoundType();
    this.playSound(lv.getStepSound(), lv.getVolume() * 0.15F, lv.getPitch());
  }

  private boolean shouldPlayAmethystStepSound(BlockState arg) {
    return arg.is(BlockTags.CRYSTAL_SOUND_BLOCKS) && this.tickCount >= this.lastCrystalSoundPlayTick + 20;
  }

  private void playAmethystStepSound() {
    this.crystalSoundIntensity = this.crystalSoundIntensity * (float)Math.pow(0.997, (double)(this.tickCount - this.lastCrystalSoundPlayTick));
    this.crystalSoundIntensity = Math.min(1.0F, this.crystalSoundIntensity + 0.07F);
    float f = 0.5F + this.crystalSoundIntensity * this.random.nextFloat() * 1.2F;
    float g = 0.1F + this.crystalSoundIntensity * 1.2F;
    this.lastCrystalSoundPlayTick = this.tickCount;
  }

  protected void playSwimSound(float f) {
    this.playSound(this.getSwimSound(), f, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
  }

  protected void onFlap() {
  }

  protected boolean isFlapping() {
    return false;
  }

  public boolean isSilent() {
    return this.entityData.get(DATA_SILENT);
  }

  public void setSilent(boolean bl) {
    this.entityData.set(DATA_SILENT, bl);
  }

  public boolean isNoGravity() {
    return this.entityData.get(DATA_NO_GRAVITY);
  }

  public void setNoGravity(boolean bl) {
    this.entityData.set(DATA_NO_GRAVITY, bl);
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

  public boolean dampensVibrations() {
    return false;
  }

  protected void checkFallDamage(double d, boolean bl, BlockState arg, BlockPos arg2) {
    if (bl) {
      if (this.fallDistance > 0.0F) {
        arg.getBlock().fallOn(this.level(), arg, arg2, this, this.fallDistance);
        this.level()
          .gameEvent(
            GameEvent.HIT_GROUND,
            this.position,
            GameEvent.Context.of(this, this.mainSupportingBlockPos.<BlockState>map(argx -> this.level().getBlockState(argx)).orElse(arg))
          );
      }

      this.resetFallDistance();
    } else if (d < 0.0) {
      this.fallDistance -= (float)d;
    }
  }

  public boolean fireImmune() {
    return this.getType().fireImmune();
  }

  public boolean causeFallDamage(float f, float g, DamageSource arg) {
    if (this.type.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
      return false;
    } else {
      if (this.isVehicle()) {
        for (Entity lv : this.getPassengers()) {
          lv.causeFallDamage(f, g, arg);
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
    if (g < 0.25F) {
      this.playSound(this.getSwimSplashSound(), g, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
    } else {
      this.playSound(this.getSwimHighSpeedSplashSound(), g, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
    }

    float h = (float)Mth.floor(this.getY());

    for (int i = 0; (float)i < 1.0F + this.dimensions.width() * 20.0F; i++) {
      double d = (this.random.nextDouble() * 2.0 - 1.0) * (double)this.dimensions.width();
      double e = (this.random.nextDouble() * 2.0 - 1.0) * (double)this.dimensions.width();
      this.level()
        .addParticle(ParticleTypes.BUBBLE, this.getX() + d, (double)(h + 1.0F), this.getZ() + e, lv2.x, lv2.y - this.random.nextDouble() * 0.2F, lv2.z);
    }

    for (int i = 0; (float)i < 1.0F + this.dimensions.width() * 20.0F; i++) {
      double d = (this.random.nextDouble() * 2.0 - 1.0) * (double)this.dimensions.width();
      double e = (this.random.nextDouble() * 2.0 - 1.0) * (double)this.dimensions.width();
      this.level().addParticle(ParticleTypes.SPLASH, this.getX() + d, (double)(h + 1.0F), this.getZ() + e, lv2.x, lv2.y, lv2.z);
    }

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

  public boolean isEyeInFluid(TagKey<FluidType> arg) {
    return this.fluidOnEyes.contains(arg);
  }

  public boolean isInLava() {
    return !this.firstTick && this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
  }

  public void moveRelative(float f, Vec3 arg) {
    Vec3 lv = getInputVector(arg, f, this.getYRot());
    this.setDeltaMovement(this.getDeltaMovement().add(lv));
  }

  private static Vec3 getInputVector(Vec3 arg, float f, float g) {
    double d = arg.lengthSqr();
    if (d < 1.0E-7) {
      return Vec3.ZERO;
    } else {
      Vec3 lv = (d > 1.0 ? arg.normalize() : arg).scale((double)f);
      float h = Mth.sin(g * (float) (Math.PI / 180.0));
      float i = Mth.cos(g * (float) (Math.PI / 180.0));
      return new Vec3(lv.x * (double)i - lv.z * (double)h, lv.y, lv.z * (double)i + lv.x * (double)h);
    }
  }

  @Deprecated
  public float getLightLevelDependentMagicValue() {
    return this.level().hasChunkAt(this.getBlockX(), this.getBlockZ())
      ? this.level().getLightLevelDependentMagicValue(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ()))
      : 0.0F;
  }

  public void absMoveTo(double d, double e, double f, float g, float h) {
    this.absMoveTo(d, e, f);
    this.absRotateTo(g, h);
  }

  public void absRotateTo(float f, float g) {
    this.setYRot(f % 360.0F);
    this.setXRot(Mth.clamp(g, -90.0F, 90.0F) % 360.0F);
    this.yRotO = this.getYRot();
    this.xRotO = this.getXRot();
  }

  public void absMoveTo(double d, double e, double f) {
    double g = Mth.clamp(d, -3.0E7, 3.0E7);
    double h = Mth.clamp(f, -3.0E7, 3.0E7);
    this.xo = g;
    this.yo = e;
    this.zo = h;
    this.setPos(g, e, h);
  }

  public void moveTo(Vec3 arg) {
    this.moveTo(arg.x, arg.y, arg.z);
  }

  public void moveTo(double d, double e, double f) {
    this.moveTo(d, e, f, this.getYRot(), this.getXRot());
  }

  public void moveTo(BlockPos arg, float f, float g) {
    this.moveTo(arg.getBottomCenter(), f, g);
  }

  public void moveTo(Vec3 arg, float f, float g) {
    this.moveTo(arg.x, arg.y, arg.z, f, g);
  }

  public void moveTo(double d, double e, double f, float g, float h) {
    this.setPosRaw(d, e, f);
    this.setYRot(g);
    this.setXRot(h);
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

  public float distanceTo(Entity arg) {
    float f = (float)(this.getX() - arg.getX());
    float g = (float)(this.getY() - arg.getY());
    float h = (float)(this.getZ() - arg.getZ());
    return Mth.sqrt(f * f + g * g + h * h);
  }

  public double distanceToSqr(double d, double e, double f) {
    double g = this.getX() - d;
    double h = this.getY() - e;
    double i = this.getZ() - f;
    return g * g + h * h + i * i;
  }

  public double distanceToSqr(Entity arg) {
    return this.distanceToSqr(arg.position());
  }

  public double distanceToSqr(Vec3 arg) {
    double d = this.getX() - arg.x;
    double e = this.getY() - arg.y;
    double f = this.getZ() - arg.z;
    return d * d + e * e + f * f;
  }

  public void playerTouch(Player arg) {
  }

  public void push(Entity arg) {
    if (!this.isPassengerOfSameVehicle(arg)) {
      if (!arg.noPhysics && !this.noPhysics) {
        double d = arg.getX() - this.getX();
        double e = arg.getZ() - this.getZ();
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

          if (!arg.isVehicle() && arg.isPushable()) {
            arg.push(d, 0.0, e);
          }
        }
      }
    }
  }

  public void push(Vec3 arg) {
    this.push(arg.x, arg.y, arg.z);
  }

  public void push(double d, double e, double f) {
    this.setDeltaMovement(this.getDeltaMovement().add(d, e, f));
    this.hasImpulse = true;
  }

  protected void markHurt() {
    this.hurtMarked = true;
  }

  public boolean hurt(DamageSource arg, float f) {
    if (this.isInvulnerableTo(arg)) {
      return false;
    } else {
      this.markHurt();
      return false;
    }
  }

  public final Vec3 getViewVector(float f) {
    return this.calculateViewVector(this.getViewXRot(f), this.getViewYRot(f));
  }

  public Direction getNearestViewDirection() {
    return Direction.getNearest(this.getViewVector(1.0F));
  }

  public float getViewXRot(float f) {
    return f == 1.0F ? this.getXRot() : Mth.lerp(f, this.xRotO, this.getXRot());
  }

  public float getViewYRot(float f) {
    return f == 1.0F ? this.getYRot() : Mth.lerp(f, this.yRotO, this.getYRot());
  }

  public final Vec3 calculateViewVector(float f, float g) {
    float h = f * (float) (Math.PI / 180.0);
    float i = -g * (float) (Math.PI / 180.0);
    float j = Mth.cos(i);
    float k = Mth.sin(i);
    float l = Mth.cos(h);
    float m = Mth.sin(h);
    return new Vec3((double)(k * l), (double)(-m), (double)(j * l));
  }

  public final Vec3 getUpVector(float f) {
    return this.calculateUpVector(this.getViewXRot(f), this.getViewYRot(f));
  }

  protected final Vec3 calculateUpVector(float f, float g) {
    return this.calculateViewVector(f - 90.0F, g);
  }

  public final Vec3 getEyePosition() {
    return new Vec3(this.getX(), this.getEyeY(), this.getZ());
  }

  public final Vec3 getEyePosition(float f) {
    double d = Mth.lerp((double)f, this.xo, this.getX());
    double e = Mth.lerp((double)f, this.yo, this.getY()) + (double)this.getEyeHeight();
    double g = Mth.lerp((double)f, this.zo, this.getZ());
    return new Vec3(d, e, g);
  }

  public Vec3 getLightProbePosition(float f) {
    return this.getEyePosition(f);
  }

  public final Vec3 getPosition(float f) {
    double d = Mth.lerp((double)f, this.xo, this.getX());
    double e = Mth.lerp((double)f, this.yo, this.getY());
    double g = Mth.lerp((double)f, this.zo, this.getZ());
    return new Vec3(d, e, g);
  }

  public HitResult pick(double d, float f, boolean bl) {
    Vec3 lv = this.getEyePosition(f);
    Vec3 lv2 = this.getViewVector(f);
    Vec3 lv3 = lv.add(lv2.x * d, lv2.y * d, lv2.z * d);
    return this.level().clip(new ClipContext(lv, lv3, ClipContext.Block.OUTLINE, bl ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
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

  public boolean shouldRender(double d, double e, double f) {
    double g = this.getX() - d;
    double h = this.getY() - e;
    double i = this.getZ() - f;
    double j = g * g + h * h + i * i;
    return this.shouldRenderAtSqrDistance(j);
  }

  public boolean shouldRenderAtSqrDistance(double d) {
    double e = this.getBoundingBox().getSize();
    if (Double.isNaN(e)) {
      e = 1.0;
    }

    e *= 64.0 * viewScale;
    return d < e * e;
  }

  public boolean saveAsPassenger(CompoundTag arg) {
    if (this.removalReason != null && !this.removalReason.shouldSave()) {
      return false;
    } else {
      String string = this.getEncodeId();
      if (string == null) {
        return false;
      } else {
        arg.putString("id", string);
        this.saveWithoutId(arg);
        return true;
      }
    }
  }

  public void load(CompoundTag arg) {
    try {
      ListTag lv = arg.getList("Pos", 6);
      ListTag lv2 = arg.getList("Motion", 6);
      ListTag lv3 = arg.getList("Rotation", 5);
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
      this.fallDistance = arg.getFloat("FallDistance");
      this.remainingFireTicks = arg.getShort("Fire");
      if (arg.contains("Air")) {
        this.setAirSupply(arg.getShort("Air"));
      }

      this.onGround = arg.getBoolean("OnGround");
      this.invulnerable = arg.getBoolean("Invulnerable");
      this.portalCooldown = arg.getInt("PortalCooldown");
      if (arg.hasUUID("UUID")) {
        this.uuid = arg.getUUID("UUID");
        this.stringUUID = this.uuid.toString();
      }

      if (!Double.isFinite(this.getX()) || !Double.isFinite(this.getY()) || !Double.isFinite(this.getZ())) {
        throw new IllegalStateException("Entity has invalid position");
      } else if (Double.isFinite((double)this.getYRot()) && Double.isFinite((double)this.getXRot())) {
        this.reapplyPosition();
        this.setRot(this.getYRot(), this.getXRot());
        if (arg.contains("CustomName", 8)) {
          String string = arg.getString("CustomName");

          try {
            this.setCustomName(Component.Serializer.fromJson(string, this.registryAccess()));
          } catch (Exception var16) {
            LOGGER.warn("Failed to parse entity custom name {}", string, var16);
          }
        }

        this.setCustomNameVisible(arg.getBoolean("CustomNameVisible"));
        this.setSilent(arg.getBoolean("Silent"));
        this.setNoGravity(arg.getBoolean("NoGravity"));
        this.setGlowingTag(arg.getBoolean("Glowing"));
        this.setTicksFrozen(arg.getInt("TicksFrozen"));
        this.hasVisualFire = arg.getBoolean("HasVisualFire");
        if (arg.contains("Tags", 9)) {
          this.tags.clear();
          ListTag lv4 = arg.getList("Tags", 8);
          int i = Math.min(lv4.size(), 1024);

          for (int j = 0; j < i; j++) {
            this.tags.add(lv4.getString(j));
          }
        }

        this.readAdditionalSaveData(arg);
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

  @Nullable
  protected final String getEncodeId() {
    EntityType<?> lv = this.getType();
    ResourceLocation lv2 = EntityType.getKey(lv);
    return lv.canSerialize() && lv2 != null ? lv2.toString() : null;
  }

  protected abstract void readAdditionalSaveData(CompoundTag arg);

  protected abstract void addAdditionalSaveData(CompoundTag arg);

  protected ListTag newDoubleList(double... ds) {
    ListTag lv = new ListTag();

    for (double d : ds) {
      lv.add(DoubleTag.valueOf(d));
    }

    return lv;
  }

  protected ListTag newFloatList(float... fs) {
    ListTag lv = new ListTag();

    for (float f : fs) {
      lv.add(FloatTag.valueOf(f));
    }

    return lv;
  }

  @Nullable
  public ItemEntity spawnAtLocation(ItemLike arg) {
    return this.spawnAtLocation(arg, 0);
  }

  @Nullable
  public ItemEntity spawnAtLocation(ItemLike arg, int i) {
    return this.spawnAtLocation(new ItemStack(arg), (float)i);
  }

  @Nullable
  public ItemEntity spawnAtLocation(ItemStack arg) {
    return this.spawnAtLocation(arg, 0.0F);
  }

  @Nullable
  public ItemEntity spawnAtLocation(ItemStack arg, float f) {
    if (arg.isEmpty()) {
      return null;
    } else {
      return null;
    }
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

  public InteractionResult interact(Player arg, InteractionHand arg2) {
    if (this.isAlive() && this instanceof Leashable lv) {
      if (lv.getLeashHolder() == arg) {
        return InteractionResult.sidedSuccess(true);
      }

      ItemStack lv2 = arg.getItemInHand(arg2);
      if (lv2.is(Items.LEAD) && lv.canHaveALeashAttachedToIt()) {
        lv2.shrink(1);
        return InteractionResult.sidedSuccess(true);
      }
    }

    return InteractionResult.PASS;
  }

  public boolean canCollideWith(Entity arg) {
    return arg.canBeCollidedWith() && !this.isPassengerOfSameVehicle(arg);
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

  public final void positionRider(Entity arg) {
    if (this.hasPassenger(arg)) {
      this.positionRider(arg, Entity::setPos);
    }
  }

  protected void positionRider(Entity arg, Entity.MoveFunction arg2) {
    Vec3 lv = this.getPassengerRidingPosition(arg);
    Vec3 lv2 = arg.getVehicleAttachmentPoint(this);
    arg2.accept(arg, lv.x - lv2.x, lv.y - lv2.y, lv.z - lv2.z);
  }

  public void onPassengerTurned(Entity arg) {
  }

  public Vec3 getVehicleAttachmentPoint(Entity arg) {
    return this.getAttachments().get(EntityAttachment.VEHICLE, 0, this.yRot);
  }

  public Vec3 getPassengerRidingPosition(Entity arg) {
    return this.position().add(this.getPassengerAttachmentPoint(arg, this.dimensions, 1.0F));
  }

  protected Vec3 getPassengerAttachmentPoint(Entity arg, EntityDimensions arg2, float f) {
    return getDefaultPassengerAttachmentPoint(this, arg, arg2.attachments());
  }

  protected static Vec3 getDefaultPassengerAttachmentPoint(Entity arg, Entity arg2, EntityAttachments arg3) {
    int i = arg.getPassengers().indexOf(arg2);
    return arg3.getClamped(EntityAttachment.PASSENGER, i, arg.yRot);
  }

  public boolean startRiding(Entity arg) {
    return this.startRiding(arg, false);
  }

  public boolean showVehicleHealth() {
    return this instanceof LivingEntity;
  }

  public boolean startRiding(Entity arg, boolean bl) {
    if (arg == this.vehicle) {
      return false;
    } else if (!arg.couldAcceptPassenger()) {
      return false;
    } else {
      for (Entity lv = arg; lv.vehicle != null; lv = lv.vehicle) {
        if (lv.vehicle == this) {
          return false;
        }
      }

      if (bl || this.canRide(arg) && arg.canAddPassenger(this)) {
        if (this.isPassenger()) {
          this.stopRiding();
        }

        this.setPose(Pose.STANDING);
        this.vehicle = arg;
        this.vehicle.addPassenger(this);
        return true;
      } else {
        return false;
      }
    }
  }

  protected boolean canRide(Entity arg) {
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

  protected void addPassenger(Entity arg) {
    if (arg.getVehicle() != this) {
      throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
    } else {
      if (this.passengers.isEmpty()) {
        this.passengers = ImmutableList.of(arg);
      } else {
        List<Entity> list = Lists.newArrayList(this.passengers);
        list.add(arg);

        this.passengers = ImmutableList.copyOf(list);
      }

      this.gameEvent(GameEvent.ENTITY_MOUNT, arg);
    }
  }

  protected void removePassenger(Entity arg) {
    if (arg.getVehicle() == this) {
      throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
    } else {
      if (this.passengers.size() == 1 && this.passengers.get(0) == arg) {
        this.passengers = ImmutableList.of();
      } else {
        this.passengers = this.passengers.stream().filter(arg2 -> arg2 != arg).collect(ImmutableList.toImmutableList());
      }

      arg.boardingCooldown = 60;
      this.gameEvent(GameEvent.ENTITY_DISMOUNT, arg);
    }
  }

  protected boolean canAddPassenger(Entity arg) {
    return this.passengers.isEmpty();
  }

  protected boolean couldAcceptPassenger() {
    return true;
  }

  public void lerpTo(double d, double e, double f, float g, float h, int i) {
    this.setPos(d, e, f);
    this.setRot(g, h);
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

  public void lerpHeadTo(float f, int i) {
    this.setYHeadRot(f);
  }

  public float getPickRadius() {
    return 0.0F;
  }

  public Vec3 getLookAngle() {
    return this.calculateViewVector(this.getXRot(), this.getYRot());
  }

  public Vec3 getHandHoldingItemAngle(Item arg) {
    if (!(this instanceof Player lv)) {
      return Vec3.ZERO;
    } else {
      boolean bl = lv.getOffhandItem().is(arg) && !lv.getMainHandItem().is(arg);
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

  public void setAsInsidePortal(Portal arg, BlockPos arg2) {
    if (this.isOnPortalCooldown()) {
      this.setPortalCooldown();
    } else {
      if (this.portalProcess != null && this.portalProcess.isSamePortal(arg)) {
        this.portalProcess.updateEntryPosition(arg2.immutable());
        this.portalProcess.setAsInsidePortalThisTick(true);
      } else {
        this.portalProcess = new PortalProcessor(arg, arg2.immutable());
      }
    }
  }

  protected void handlePortal() {
  }

  public int getDimensionChangingDelay() {
    return 300;
  }

  public void lerpMotion(double d, double e, double f) {
    this.setDeltaMovement(d, e, f);
  }

  public void handleDamageEvent(DamageSource arg) {
  }

  public void handleEntityEvent(byte b) {
    switch (b) {
      case 53:
        HoneyBlock.showSlideParticles(this);
    }
  }

  public void animateHurt(float f) {
  }

  public boolean isOnFire() {
    return !this.fireImmune() && (this.remainingFireTicks > 0 || this.getSharedFlag(0));
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

  public void setShiftKeyDown(boolean bl) {
    this.setSharedFlag(1, bl);
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

  public void setSprinting(boolean bl) {
    this.setSharedFlag(3, bl);
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

  public void setSwimming(boolean bl) {
    this.setSharedFlag(4, bl);
  }

  public final boolean hasGlowingTag() {
    return this.hasGlowingTag;
  }

  public final void setGlowingTag(boolean bl) {
    this.hasGlowingTag = bl;
    this.setSharedFlag(6, this.isCurrentlyGlowing());
  }

  public boolean isCurrentlyGlowing() {
    return this.getSharedFlag(6);
  }

  public boolean isInvisible() {
    return this.getSharedFlag(5);
  }

  public boolean isInvisibleTo(Player arg) {
    if (arg.isSpectator()) {
      return false;
    } else {
      Team lv = this.getTeam();
      return lv != null && arg != null && arg.getTeam() == lv && lv.canSeeFriendlyInvisibles() ? false : this.isInvisible();
    }
  }

  public boolean isOnRails() {
    return false;
  }

  @Nullable
  public PlayerTeam getTeam() {
    return this.level().getScoreboard().getPlayersTeam(this.getScoreboardName());
  }

  public boolean isAlliedTo(Entity arg) {
    return this.isAlliedTo(arg.getTeam());
  }

  public boolean isAlliedTo(Team arg) {
    return this.getTeam() != null ? this.getTeam().isAlliedTo(arg) : false;
  }

  public void setInvisible(boolean bl) {
    this.setSharedFlag(5, bl);
  }

  protected boolean getSharedFlag(int i) {
    return (this.entityData.get(DATA_SHARED_FLAGS_ID) & 1 << i) != 0;
  }

  protected void setSharedFlag(int i, boolean bl) {
    byte b = this.entityData.get(DATA_SHARED_FLAGS_ID);
    if (bl) {
      this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(b | 1 << i));
    } else {
      this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(b & ~(1 << i)));
    }
  }

  public int getMaxAirSupply() {
    return 300;
  }

  public int getAirSupply() {
    return this.entityData.get(DATA_AIR_SUPPLY_ID);
  }

  public void setAirSupply(int i) {
    this.entityData.set(DATA_AIR_SUPPLY_ID, i);
  }

  public int getTicksFrozen() {
    return this.entityData.get(DATA_TICKS_FROZEN);
  }

  public void setTicksFrozen(int i) {
    this.entityData.set(DATA_TICKS_FROZEN, i);
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

  public void onAboveBubbleCol(boolean bl) {
    Vec3 lv = this.getDeltaMovement();
    double d;
    if (bl) {
      d = Math.max(-0.9, lv.y - 0.03);
    } else {
      d = Math.min(1.8, lv.y + 0.1);
    }

    this.setDeltaMovement(lv.x, d, lv.z);
  }

  public void onInsideBubbleColumn(boolean bl) {
    Vec3 lv = this.getDeltaMovement();
    double d;
    if (bl) {
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

  protected void moveTowardsClosestSpace(double d, double e, double f) {
    BlockPos lv = BlockPos.containing(d, e, f);
    Vec3 lv2 = new Vec3(d - (double)lv.getX(), e - (double)lv.getY(), f - (double)lv.getZ());
    BlockPos.MutableBlockPos lv3 = new BlockPos.MutableBlockPos();
    Direction lv4 = Direction.UP;
    double g = Double.MAX_VALUE;

    for (Direction lv5 : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
      lv3.setWithOffset(lv, lv5);
      if (!this.level().getBlockState(lv3).isCollisionShapeFullBlock(this.level(), lv3)) {
        double h = lv2.get(lv5.getAxis());
        double i = lv5.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - h : h;
        if (i < g) {
          g = i;
          lv4 = lv5;
        }
      }
    }

    float j = this.random.nextFloat() * 0.2F + 0.1F;
    float k = (float)lv4.getAxisDirection().getStep();
    Vec3 lv6 = this.getDeltaMovement().scale(0.75);
    if (lv4.getAxis() == Direction.Axis.X) {
      this.setDeltaMovement((double)(k * j), lv6.y, lv6.z);
    } else if (lv4.getAxis() == Direction.Axis.Y) {
      this.setDeltaMovement(lv6.x, (double)(k * j), lv6.z);
    } else if (lv4.getAxis() == Direction.Axis.Z) {
      this.setDeltaMovement(lv6.x, lv6.y, (double)(k * j));
    }
  }

  public void makeStuckInBlock(BlockState arg, Vec3 arg2) {
    this.resetFallDistance();
    this.stuckSpeedMultiplier = arg2;
  }

  private static Component removeAction(Component arg) {
    MutableComponent lv = arg.plainCopy().setStyle(arg.getStyle().withClickEvent(null));

    for (Component lv2 : arg.getSiblings()) {
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

  public boolean is(Entity arg) {
    return this == arg;
  }

  public float getYHeadRot() {
    return 0.0F;
  }

  public void setYHeadRot(float f) {
  }

  public void setYBodyRot(float f) {
  }

  public boolean isAttackable() {
    return true;
  }

  public boolean skipAttackInteraction(Entity arg) {
    return false;
  }

  @Override
  public String toString() {
    String string = this.level() == null ? "~NULL~" : this.level().toString();
    return this.removalReason != null
      ? String.format(
      Locale.ROOT,
      "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f, removed=%s]",
      this.getClass().getSimpleName(),
      this.getName().getString(),
      this.id,
      string,
      this.getX(),
      this.getY(),
      this.getZ(),
      this.removalReason
    )
      : String.format(
      Locale.ROOT,
      "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f]",
      this.getClass().getSimpleName(),
      this.getName().getString(),
      this.id,
      string,
      this.getX(),
      this.getY(),
      this.getZ()
    );
  }

  public boolean isInvulnerableTo(DamageSource arg) {
    return this.isRemoved()
      || this.invulnerable && !arg.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !arg.isCreativePlayer()
      || arg.is(DamageTypeTags.IS_FIRE) && this.fireImmune()
      || arg.is(DamageTypeTags.IS_FALL) && this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE);
  }

  public boolean isInvulnerable() {
    return this.invulnerable;
  }

  public void setInvulnerable(boolean bl) {
    this.invulnerable = bl;
  }

  public void copyPosition(Entity arg) {
    this.moveTo(arg.getX(), arg.getY(), arg.getZ(), arg.getYRot(), arg.getXRot());
  }

  public void restoreFrom(Entity arg) {
    CompoundTag lv = arg.saveWithoutId(new CompoundTag());
    lv.remove("Dimension");
    this.load(lv);
    this.portalCooldown = arg.portalCooldown;
    this.portalProcess = arg.portalProcess;
  }

  @Nullable
  public Entity changeDimension(DimensionTransition arg) {
    return null;
  }

  public void placePortalTicket(BlockPos arg) {
  }

  protected void removeAfterChangingDimensions() {
    this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
    if (this instanceof Leashable lv) {
      lv.dropLeash(true, false);
    }
  }

  public Vec3 getRelativePortalPosition(Direction.Axis arg, BlockUtil.FoundRectangle arg2) {
    return PortalShape.getRelativePosition(arg2, arg, this.position(), this.getDimensions(this.getPose()));
  }

  public boolean canUsePortal(boolean bl) {
    return (bl || !this.isPassenger()) && this.isAlive();
  }

  public boolean canChangeDimensions(Level arg, Level arg2) {
    return true;
  }

  public float getBlockExplosionResistance(Explosion arg, BlockGetter arg2, BlockPos arg3, BlockState arg4, FluidState arg5, float f) {
    return f;
  }

  public boolean shouldBlockExplode(Explosion arg, BlockGetter arg2, BlockPos arg3, BlockState arg4, float f) {
    return true;
  }

  public int getMaxFallDistance() {
    return 3;
  }

  public boolean isIgnoringBlockTriggers() {
    return false;
  }

  public void fillCrashReportCategory(CrashReportCategory arg) {
    arg.setDetail("Entity Type", () -> EntityType.getKey(this.getType()) + " (" + this.getClass().getCanonicalName() + ")");
    arg.setDetail("Entity ID", this.id);
    arg.setDetail("Entity Name", () -> this.getName().getString());
    arg.setDetail("Entity's Exact location", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
    arg.setDetail(
      "Entity's Block location", CrashReportCategory.formatLocation(this.level(), Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()))
    );
    Vec3 lv = this.getDeltaMovement();
    arg.setDetail("Entity's Momentum", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", lv.x, lv.y, lv.z));
    arg.setDetail("Entity's Passengers", () -> this.getPassengers().toString());
    arg.setDetail("Entity's Vehicle", () -> String.valueOf(this.getVehicle()));
  }

  public boolean displayFireAnimation() {
    return this.isOnFire() && !this.isSpectator();
  }

  public void setUUID(UUID uUID) {
    this.uuid = uUID;
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

  public static void setViewScale(double d) {
    viewScale = d;
  }

  @Override
  public Component getDisplayName() {
    return PlayerTeam.formatNameForTeam(this.getTeam(), this.getName())
      .withStyle(arg -> arg.withHoverEvent(this.createHoverEvent()).withInsertion(this.getStringUUID()));
  }

  public void setCustomName(@Nullable Component arg) {
    this.entityData.set(DATA_CUSTOM_NAME, Optional.ofNullable(arg));
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

  public void setCustomNameVisible(boolean bl) {
    this.entityData.set(DATA_CUSTOM_NAME_VISIBLE, bl);
  }

  public boolean isCustomNameVisible() {
    return this.entityData.get(DATA_CUSTOM_NAME_VISIBLE);
  }

  public void dismountTo(double d, double e, double f) {
    this.teleportTo(d, e, f);
  }

  public void teleportTo(double d, double e, double f) {
  }

  private void teleportPassengers() {
    this.getSelfAndPassengers().forEach(arg -> {
      UnmodifiableIterator var1 = arg.passengers.iterator();

      while (var1.hasNext()) {
        Entity lv = (Entity)var1.next();
        arg.positionRider(lv, Entity::moveTo);
      }
    });
  }

  public void teleportRelative(double d, double e, double f) {
    this.teleportTo(this.getX() + d, this.getY() + e, this.getZ() + f);
  }

  public boolean shouldShowName() {
    return this.isCustomNameVisible();
  }

  @Override
  public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> list) {
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> arg) {
    if (DATA_POSE.equals(arg)) {
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
  }

  public boolean fudgePositionAfterSizeChange(EntityDimensions arg) {
    EntityDimensions lv = this.getDimensions(this.getPose());
    Vec3 lv2 = this.position().add(0.0, (double)arg.height() / 2.0, 0.0);
    double d = (double)Math.max(0.0F, lv.width() - arg.width()) + 1.0E-6;
    double e = (double)Math.max(0.0F, lv.height() - arg.height()) + 1.0E-6;
    VoxelShape lv3 = Shapes.create(AABB.ofSize(lv2, d, e, d));
    Optional<Vec3> optional = this.level.findFreePosition(this, lv3, lv2, (double)lv.width(), (double)lv.height(), (double)lv.width());
    if (optional.isPresent()) {
      this.setPos(optional.get().add(0.0, (double)(-lv.height()) / 2.0, 0.0));
      return true;
    } else {
      if (lv.width() > arg.width() && lv.height() > arg.height()) {
        VoxelShape lv4 = Shapes.create(AABB.ofSize(lv2, d, 1.0E-6, d));
        Optional<Vec3> optional2 = this.level.findFreePosition(this, lv4, lv2, (double)lv.width(), (double)arg.height(), (double)lv.width());
        if (optional2.isPresent()) {
          this.setPos(optional2.get().add(0.0, (double)(-arg.height()) / 2.0 + 1.0E-6, 0.0));
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

  public final void setBoundingBox(AABB arg) {
    this.bb = arg;
  }

  public final float getEyeHeight(Pose arg) {
    return this.getDimensions(arg).eyeHeight();
  }

  public final float getEyeHeight() {
    return this.eyeHeight;
  }

  public Vec3 getLeashOffset(float f) {
    return this.getLeashOffset();
  }

  protected Vec3 getLeashOffset() {
    return new Vec3(0.0, (double)this.getEyeHeight(), (double)(this.getBbWidth() * 0.4F));
  }

  public SlotAccess getSlot(int i) {
    return SlotAccess.NULL;
  }

  @Override
  public void sendSystemMessage(Component arg) {
  }

  public Level getCommandSenderWorld() {
    return this.level();
  }

  public InteractionResult interactAt(Player arg, Vec3 arg2, InteractionHand arg3) {
    return InteractionResult.PASS;
  }

  public boolean ignoreExplosion(Explosion arg) {
    return false;
  }

  public float rotate(Rotation arg) {
    float f = Mth.wrapDegrees(this.getYRot());
    switch (arg) {
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

  public float mirror(Mirror arg) {
    float f = Mth.wrapDegrees(this.getYRot());
    switch (arg) {
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

  public ProjectileDeflection deflection(Projectile arg) {
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

  public boolean hasPassenger(Entity arg) {
    return this.passengers.contains(arg);
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
    return (int)this.getIndirectPassengersStream().filter(arg -> arg instanceof Player).count();
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

  public boolean isPassengerOfSameVehicle(Entity arg) {
    return this.getRootVehicle() == arg.getRootVehicle();
  }

  public boolean hasIndirectPassenger(Entity arg) {
    if (!arg.isPassenger()) {
      return false;
    } else {
      Entity lv = arg.getVehicle();
      return lv == this ? true : this.hasIndirectPassenger(lv);
    }
  }

  public boolean isControlledByLocalInstance() {
    return this.getControllingPassenger() instanceof Player lv ? lv.isLocalPlayer() : this.isEffectiveAi();
  }

  public boolean isEffectiveAi() {
    return false;
  }

  protected static Vec3 getCollisionHorizontalEscapeVector(double d, double e, float f) {
    double g = (d + e + 1.0E-5F) / 2.0;
    float h = -Mth.sin(f * (float) (Math.PI / 180.0));
    float i = Mth.cos(f * (float) (Math.PI / 180.0));
    float j = Math.max(Math.abs(h), Math.abs(i));
    return new Vec3((double)h * g / (double)j, 0.0, (double)i * g / (double)j);
  }

  public Vec3 getDismountLocationForPassenger(LivingEntity arg) {
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

  public CommandSourceStack createCommandSourceStack() {
    return new CommandSourceStack(
      this,
      this.position(),
      this.getRotationVector(),
      null,
      this.getPermissionLevel(),
      this.getName().getString(),
      this.getDisplayName(),
      this.level().getServer(),
      this
    );
  }

  protected int getPermissionLevel() {
    return 0;
  }

  public boolean hasPermissions(int i) {
    return this.getPermissionLevel() >= i;
  }

  @Override
  public boolean acceptsSuccess() {
    return this.level().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK);
  }

  @Override
  public boolean acceptsFailure() {
    return true;
  }

  @Override
  public boolean shouldInformAdmins() {
    return true;
  }

  public void lookAt(EntityAnchorArgument.Anchor arg, Vec3 arg2) {
    Vec3 lv = arg.apply(this);
    double d = arg2.x - lv.x;
    double e = arg2.y - lv.y;
    double f = arg2.z - lv.z;
    double g = Math.sqrt(d * d + f * f);
    this.setXRot(Mth.wrapDegrees((float)(-(Mth.atan2(e, g) * 180.0F / (float)Math.PI))));
    this.setYRot(Mth.wrapDegrees((float)(Mth.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F));
    this.setYHeadRot(this.getYRot());
    this.xRotO = this.getXRot();
    this.yRotO = this.getYRot();
  }

  public float getPreciseBodyRotation(float f) {
    return Mth.lerp(f, this.yRotO, this.yRot);
  }

  public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> arg, double d) {
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
            if (lv4.is(arg)) {
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
        lv2 = lv2.scale(d);
        double g = 0.003;
        if (Math.abs(lv6.x) < 0.003 && Math.abs(lv6.z) < 0.003 && lv2.length() < 0.0045000000000000005) {
          lv2 = lv2.normalize().scale(0.0045000000000000005);
        }

        this.setDeltaMovement(this.getDeltaMovement().add(lv2));
      }

      this.fluidHeight.put(arg, e);
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

  public double getFluidHeight(TagKey<Fluid> arg) {
    return this.fluidHeight.getDouble(arg);
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

  public EntityDimensions getDimensions(Pose arg) {
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

  public void setDeltaMovement(Vec3 arg) {
    this.deltaMovement = arg;
  }

  public void addDeltaMovement(Vec3 arg) {
    this.setDeltaMovement(this.getDeltaMovement().add(arg));
  }

  public void setDeltaMovement(double d, double e, double f) {
    this.setDeltaMovement(new Vec3(d, e, f));
  }

  public final int getBlockX() {
    return this.blockPosition.getX();
  }

  public final double getX() {
    return this.position.x;
  }

  public double getX(double d) {
    return this.position.x + (double)this.getBbWidth() * d;
  }

  public double getRandomX(double d) {
    return this.getX((2.0 * this.random.nextDouble() - 1.0) * d);
  }

  public final int getBlockY() {
    return this.blockPosition.getY();
  }

  public final double getY() {
    return this.position.y;
  }

  public double getY(double d) {
    return this.position.y + (double)this.getBbHeight() * d;
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

  public double getZ(double d) {
    return this.position.z + (double)this.getBbWidth() * d;
  }

  public double getRandomZ(double d) {
    return this.getZ((2.0 * this.random.nextDouble() - 1.0) * d);
  }

  public final void setPosRaw(double d, double e, double f) {
    if (this.position.x != d || this.position.y != e || this.position.z != f) {
      this.position = new Vec3(d, e, f);
      int i = Mth.floor(d);
      int j = Mth.floor(e);
      int k = Mth.floor(f);
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

  public Vec3 getRopeHoldPosition(float f) {
    return this.getPosition(f).add(0.0, (double)this.eyeHeight * 0.7, 0.0);
  }

  public void recreateFromPacket(ClientboundAddEntityPacket arg) {
    int i = arg.getEntityId();
    double d = arg.getX();
    double e = arg.getY();
    double f = arg.getZ();
    this.syncPacketPositionCodec(d, e, f);
    this.moveTo(d, e, f);
    this.setXRot(arg.getXRot());
    this.setYRot(arg.getYRot());
    this.setId(i);
    this.setUUID(arg.getUuid());
  }

  @Nullable
  public ItemStack getPickResult() {
    return null;
  }

  public void setIsInPowderSnow(boolean bl) {
    this.isInPowderSnow = bl;
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

  public void setYRot(float f) {
    if (!Float.isFinite(f)) {
      Util.logAndPauseIfInIde("Invalid entity rotation: " + f + ", discarding.");
    } else {
      this.yRot = f;
    }
  }

  public float getXRot() {
    return this.xRot;
  }

  public void setXRot(float f) {
    if (!Float.isFinite(f)) {
      Util.logAndPauseIfInIde("Invalid entity rotation: " + f + ", discarding.");
    } else {
      this.xRot = f;
    }
  }

  public boolean canSprint() {
    return false;
  }

  public float maxUpStep() {
    return 0.0F;
  }

  public void onExplosionHit(@Nullable Entity arg) {
  }

  public final boolean isRemoved() {
    return this.removalReason != null;
  }

  @Nullable
  public Entity.RemovalReason getRemovalReason() {
    return this.removalReason;
  }

  @Override
  public final void setRemoved(Entity.RemovalReason arg) {
    if (this.removalReason == null) {
      this.removalReason = arg;
    }

    if (this.removalReason.shouldDestroy()) {
      this.stopRiding();
    }

    this.getPassengers().forEach(Entity::stopRiding);
    this.levelCallback.onRemove(arg);
  }

  protected void unsetRemoved() {
    this.removalReason = null;
  }

  @Override
  public void setLevelCallback(EntityInLevelCallback arg) {
    this.levelCallback = arg;
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

  public boolean mayInteract(Level arg, BlockPos arg2) {
    return true;
  }

  public Level level() {
    return this.level;
  }

  protected void setLevel(Level arg) {
    this.level = arg;
  }

  public DamageSources damageSources() {
    return this.level().damageSources();
  }

  public RegistryAccess registryAccess() {
    return this.level().registryAccess();
  }

  protected void lerpPositionAndRotationStep(int i, double d, double e, double f, double g, double h) {
    double j = 1.0 / (double)i;
    double k = Mth.lerp(j, this.getX(), d);
    double l = Mth.lerp(j, this.getY(), e);
    double m = Mth.lerp(j, this.getZ(), f);
    float n = (float)Mth.rotLerp(j, (double)this.getYRot(), g);
    float o = (float)Mth.lerp(j, (double)this.getXRot(), h);
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

  public enum MovementEmission {
    NONE(false, false),
    SOUNDS(true, false),
    EVENTS(false, true),
    ALL(true, true);

    final boolean sounds;
    final boolean events;

    MovementEmission(final boolean bl, final boolean bl2) {
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

  public enum RemovalReason {
    KILLED(true, false),
    DISCARDED(true, false),
    UNLOADED_TO_CHUNK(false, true),
    UNLOADED_WITH_PLAYER(false, false),
    CHANGED_DIMENSION(false, false);

    private final boolean destroy;
    private final boolean save;

    RemovalReason(final boolean bl, final boolean bl2) {
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
