package com.soulfiremc.server.protocol.bot.state.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.soulfiremc.server.data.EffectType;
import com.soulfiremc.server.data.FluidTags;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.kyori.adventure.key.Key;
import net.minecraft.BlockUtil;
import net.minecraft.Util;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LivingEntity extends Entity {
  private static final String TAG_ACTIVE_EFFECTS = "active_effects";
  private static final Key SPEED_MODIFIER_POWDER_SNOW_ID = Key.key("powder_snow");
  private static final Key SPRINTING_MODIFIER_ID = Key.key("sprinting");
  private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(
    SPRINTING_MODIFIER_ID, 0.3F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
  );
  public static final int HAND_SLOTS = 2;
  public static final int ARMOR_SLOTS = 4;
  public static final int EQUIPMENT_SLOT_OFFSET = 98;
  public static final int ARMOR_SLOT_OFFSET = 100;
  public static final int BODY_ARMOR_OFFSET = 105;
  public static final int SWING_DURATION = 6;
  public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
  private static final int DAMAGE_SOURCE_TIMEOUT = 40;
  public static final double MIN_MOVEMENT_DISTANCE = 0.003;
  public static final double DEFAULT_BASE_GRAVITY = 0.08;
  public static final int DEATH_DURATION = 20;
  private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
  private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
  public static final int USE_ITEM_INTERVAL = 4;
  public static final float BASE_JUMP_POWER = 0.42F;
  private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0;
  protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
  protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
  protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
  protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
  private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES = SynchedEntityData.defineId(
    LivingEntity.class, EntityDataSerializers.PARTICLES
  );
  private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID = SynchedEntityData.defineId(
    LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS
  );
  private static final int PARTICLE_FREQUENCY_WHEN_INVISIBLE = 15;
  protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(0.2F);
  public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
  public static final float DEFAULT_BABY_SCALE = 0.5F;
  private static final float ITEM_USE_EFFECT_START_FRACTION = 0.21875F;
  public static final String ATTRIBUTES_FIELD = "attributes";
  private final AttributeMap attributes;
  private final CombatTracker combatTracker = new CombatTracker(this);
  private final Map<EffectType, MobEffectInstance> activeEffects = Maps.newHashMap();
  private final NonNullList<ItemStack> lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
  private final NonNullList<ItemStack> lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);
  private ItemStack lastBodyItemStack = ItemStack.EMPTY;
  public boolean swinging;
  private boolean discardFriction = false;
  public InteractionHand swingingArm;
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
  @Nullable
  protected Player lastHurtByPlayer;
  protected int lastHurtByPlayerTime;
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
  @Nullable
  private LivingEntity lastHurtByMob;
  private int lastHurtByMobTimestamp;
  @Nullable
  private LivingEntity lastHurtMob;
  private int lastHurtMobTimestamp;
  private float speed;
  private int noJumpDelay;
  private float absorptionAmount;
  protected ItemStack useItem = ItemStack.EMPTY;
  protected int useItemRemaining;
  protected int fallFlyTicks;
  private BlockPos lastPos;
  private Optional<BlockPos> lastClimbablePos = Optional.empty();
  @Nullable
  private DamageSource lastDamageSource;
  private long lastDamageStamp;
  protected int autoSpinAttackTicks;
  protected float autoSpinAttackDmg;
  @Nullable
  protected ItemStack autoSpinAttackItemStack;
  private float swimAmount;
  private float swimAmountO;
  private boolean skipDropExperience;
  private final Reference2ObjectMap<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEnchantments = new Reference2ObjectArrayMap();
  protected float appliedScale = 1.0F;

  protected LivingEntity(EntityType<? extends LivingEntity> arg, Level arg2) {
    super(arg, arg2);
    this.attributes = new AttributeMap(DefaultAttributes.getSupplier(arg));
    this.setHealth(this.getMaxHealth());
    this.blocksBuilding = true;
    this.rotA = (float)((Math.random() + 1.0) * 0.01F);
    this.reapplyPosition();
    this.timeOffs = (float)Math.random() * 12398.0F;
    this.setYRot((float)(Math.random() * (float) (Math.PI * 2)));
    this.yHeadRot = this.getYRot();
  }

  @Override
  public void kill() {
    this.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
  }

  public boolean canAttackType(EntityType<?> arg) {
    return true;
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder arg) {
    arg.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
    arg.define(DATA_EFFECT_PARTICLES, List.of());
    arg.define(DATA_EFFECT_AMBIENCE_ID, false);
    arg.define(DATA_ARROW_COUNT_ID, 0);
    arg.define(DATA_STINGER_COUNT_ID, 0);
    arg.define(DATA_HEALTH_ID, 1.0F);
    arg.define(SLEEPING_POS_ID, Optional.empty());
  }

  public static AttributeSupplier.Builder createLivingAttributes() {
    return AttributeSupplier.builder()
      .add(Attributes.MAX_HEALTH)
      .add(Attributes.KNOCKBACK_RESISTANCE)
      .add(Attributes.MOVEMENT_SPEED)
      .add(Attributes.ARMOR)
      .add(Attributes.ARMOR_TOUGHNESS)
      .add(Attributes.MAX_ABSORPTION)
      .add(Attributes.STEP_HEIGHT)
      .add(Attributes.SCALE)
      .add(Attributes.GRAVITY)
      .add(Attributes.SAFE_FALL_DISTANCE)
      .add(Attributes.FALL_DAMAGE_MULTIPLIER)
      .add(Attributes.JUMP_STRENGTH)
      .add(Attributes.OXYGEN_BONUS)
      .add(Attributes.BURNING_TIME)
      .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
      .add(Attributes.WATER_MOVEMENT_EFFICIENCY)
      .add(Attributes.MOVEMENT_EFFICIENCY)
      .add(Attributes.ATTACK_KNOCKBACK);
  }

  @Override
  protected void checkFallDamage(double d, boolean bl, BlockState arg, BlockPos arg2) {
    if (!this.isInWater()) {
      this.updateInWaterStateAndDoWaterCurrentPushing();
    }

    super.checkFallDamage(d, bl, arg, arg2);
    if (bl) {
      this.lastClimbablePos = Optional.empty();
    }
  }

  public final boolean canBreatheUnderwater() {
    return this.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
  }

  public float getSwimAmount(float f) {
    return Mth.lerp(f, this.swimAmountO, this.swimAmount);
  }

  public boolean hasLandedInLiquid() {
    return this.getDeltaMovement().y() < 1.0E-5F && this.isInLiquid();
  }

  @Override
  public void baseTick() {
    this.oAttackAnim = this.attackAnim;
    if (this.firstTick) {
      this.getSleepingPos().ifPresent(this::setPosToBed);
    }

    super.baseTick();
    this.clearFire();

    if (this.isAlive()) {
      boolean bl = this instanceof Player;
      if (this.isEyeInFluid(FluidTags.WATER)
        && !this.level().getBlockState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
        boolean bl2 = !this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && (!bl || !((Player)this).getAbilities().invulnerable);
        if (bl2) {
          this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
          if (this.getAirSupply() == -20) {
            this.setAirSupply(0);
            Vec3 lv2 = this.getDeltaMovement();

            for (int i = 0; i < 8; i++) {
              double f = this.random.nextDouble() - this.random.nextDouble();
              double g = this.random.nextDouble() - this.random.nextDouble();
              double h = this.random.nextDouble() - this.random.nextDouble();
              this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + f, this.getY() + g, this.getZ() + h, lv2.x, lv2.y, lv2.z);
            }

            this.hurt(this.damageSources().drown(), 2.0F);
          }
        }
      } else if (this.getAirSupply() < this.getMaxAirSupply()) {
        this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
      }
    }

    if (this.isAlive() && (this.isInWaterRainOrBubble() || this.isInPowderSnow)) {
      this.extinguishFire();
    }

    if (this.hurtTime > 0) {
      this.hurtTime--;
    }

    if (this.invulnerableTime > 0) {
      this.invulnerableTime--;
    }

    if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) {
      this.tickDeath();
    }

    if (this.lastHurtByPlayerTime > 0) {
      this.lastHurtByPlayerTime--;
    } else {
      this.lastHurtByPlayer = null;
    }

    if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
      this.lastHurtMob = null;
    }

    if (this.lastHurtByMob != null) {
      if (!this.lastHurtByMob.isAlive()) {
        this.setLastHurtByMob(null);
      } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
        this.setLastHurtByMob(null);
      }
    }

    this.tickEffects();
    this.animStepO = this.animStep;
    this.yBodyRotO = this.yBodyRot;
    this.yHeadRotO = this.yHeadRot;
    this.yRotO = this.getYRot();
    this.xRotO = this.getXRot();
  }

  @Override
  protected float getBlockSpeedFactor() {
    return Mth.lerp((float)this.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), super.getBlockSpeedFactor(), 1.0F);
  }

  protected void removeFrost() {
    AttributeInstance lv = this.getAttribute(Attributes.MOVEMENT_SPEED);
    if (lv != null) {
      if (lv.getModifier(SPEED_MODIFIER_POWDER_SNOW_ID) != null) {
        lv.removeModifier(SPEED_MODIFIER_POWDER_SNOW_ID);
      }
    }
  }

  protected void tryAddFrost() {
    if (!this.getBlockStateOnLegacy().isAir()) {
      int i = this.getTicksFrozen();
      if (i > 0) {
        AttributeInstance lv = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (lv == null) {
          return;
        }

        float f = -0.05F * this.getPercentFrozen();
        lv.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_ID, (double)f, AttributeModifier.Operation.ADD_VALUE));
      }
    }
  }

  public boolean isBaby() {
    return false;
  }

  public float getAgeScale() {
    return this.isBaby() ? 0.5F : 1.0F;
  }

  public float getScale() {
    AttributeMap lv = this.getAttributes();
    return lv == null ? 1.0F : this.sanitizeScale((float)lv.getValue(Attributes.SCALE));
  }

  protected float sanitizeScale(float f) {
    return f;
  }

  protected boolean isAffectedByFluids() {
    return true;
  }

  protected void tickDeath() {
    this.deathTime++;
  }

  public boolean shouldDropExperience() {
    return !this.isBaby();
  }

  protected boolean shouldDropLoot() {
    return !this.isBaby();
  }

  protected int decreaseAirSupply(int i) {
    AttributeInstance lv = this.getAttribute(Attributes.OXYGEN_BONUS);
    double d;
    if (lv != null) {
      d = lv.getValue();
    } else {
      d = 0.0;
    }

    return d > 0.0 && this.random.nextDouble() >= 1.0 / (d + 1.0) ? i : i - 1;
  }

  protected int increaseAirSupply(int i) {
    return Math.min(i + 4, this.getMaxAirSupply());
  }

  protected int getBaseExperienceReward() {
    return 0;
  }

  protected boolean isAlwaysExperienceDropper() {
    return false;
  }

  @Nullable
  public LivingEntity getLastHurtByMob() {
    return this.lastHurtByMob;
  }

  @Override
  public LivingEntity getLastAttacker() {
    return this.getLastHurtByMob();
  }

  public int getLastHurtByMobTimestamp() {
    return this.lastHurtByMobTimestamp;
  }

  public void setLastHurtByPlayer(@Nullable Player arg) {
    this.lastHurtByPlayer = arg;
    this.lastHurtByPlayerTime = this.tickCount;
  }

  public void setLastHurtByMob(@Nullable LivingEntity arg) {
    this.lastHurtByMob = arg;
    this.lastHurtByMobTimestamp = this.tickCount;
  }

  @Nullable
  public LivingEntity getLastHurtMob() {
    return this.lastHurtMob;
  }

  public int getLastHurtMobTimestamp() {
    return this.lastHurtMobTimestamp;
  }

  public void setLastHurtMob(Entity arg) {
    if (arg instanceof LivingEntity) {
      this.lastHurtMob = (LivingEntity)arg;
    } else {
      this.lastHurtMob = null;
    }

    this.lastHurtMobTimestamp = this.tickCount;
  }

  public int getNoActionTime() {
    return this.noActionTime;
  }

  public void setNoActionTime(int i) {
    this.noActionTime = i;
  }

  public boolean shouldDiscardFriction() {
    return this.discardFriction;
  }

  public void setDiscardFriction(boolean bl) {
    this.discardFriction = bl;
  }

  protected boolean doesEmitEquipEvent(EquipmentSlot arg) {
    return true;
  }

  public void onEquipItem(EquipmentSlot arg, ItemStack arg2, ItemStack arg3) {
    boolean bl = arg3.isEmpty() && arg2.isEmpty();
    if (!bl && !ItemStack.isSameItemSameComponents(arg2, arg3) && !this.firstTick) {
      Equipable lv = Equipable.get(arg3);
    }
  }

  @Override
  public void remove(Entity.RemovalReason arg) {
    if (arg == Entity.RemovalReason.KILLED || arg == Entity.RemovalReason.DISCARDED) {
      this.triggerOnDeathMobEffects(arg);
    }

    super.remove(arg);
    this.brain.clearMemories();
  }

  protected void triggerOnDeathMobEffects(Entity.RemovalReason arg) {
    for (MobEffectInstance lv : this.getActiveEffects()) {
      lv.onMobRemoved(this, arg);
    }

    this.activeEffects.clear();
  }

  @Override
  public void addAdditionalSaveData(CompoundTag arg) {
    arg.putFloat("Health", this.getHealth());
    arg.putShort("HurtTime", (short)this.hurtTime);
    arg.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
    arg.putShort("DeathTime", (short)this.deathTime);
    arg.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
    arg.put("attributes", this.getAttributes().save());
    if (!this.activeEffects.isEmpty()) {
      ListTag lv = new ListTag();

      for (MobEffectInstance lv2 : this.activeEffects.values()) {
        lv.add(lv2.save());
      }

      arg.put("active_effects", lv);
    }

    arg.putBoolean("FallFlying", this.isFallFlying());
    this.getSleepingPos().ifPresent(arg2 -> {
      arg.putInt("SleepingX", arg2.getX());
      arg.putInt("SleepingY", arg2.getY());
      arg.putInt("SleepingZ", arg2.getZ());
    });
    DataResult<Tag> dataResult = this.brain.serializeStart(NbtOps.INSTANCE);
    dataResult.resultOrPartial(LOGGER::error).ifPresent(arg2 -> arg.put("Brain", arg2));
  }

  @Override
  public void readAdditionalSaveData(CompoundTag arg) {
    this.internalSetAbsorptionAmount(arg.getFloat("AbsorptionAmount"));

    if (arg.contains("active_effects", 9)) {
      ListTag lv = arg.getList("active_effects", 10);

      for (int i = 0; i < lv.size(); i++) {
        CompoundTag lv2 = lv.getCompound(i);
        MobEffectInstance lv3 = MobEffectInstance.load(lv2);
        if (lv3 != null) {
          this.activeEffects.put(lv3.getEffect(), lv3);
        }
      }
    }

    if (arg.contains("Health", 99)) {
      this.setHealth(arg.getFloat("Health"));
    }

    this.hurtTime = arg.getShort("HurtTime");
    this.deathTime = arg.getShort("DeathTime");
    this.lastHurtByMobTimestamp = arg.getInt("HurtByTimestamp");
    if (arg.contains("Team", 8)) {
      String string = arg.getString("Team");
      Scoreboard lv4 = this.level().getScoreboard();
      PlayerTeam lv5 = lv4.getPlayerTeam(string);
      boolean bl = lv5 != null && lv4.addPlayerToTeam(this.getStringUUID(), lv5);
      if (!bl) {
        LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", string);
      }
    }

    if (arg.getBoolean("FallFlying")) {
      this.setSharedFlag(7, true);
    }

    if (arg.contains("SleepingX", 99) && arg.contains("SleepingY", 99) && arg.contains("SleepingZ", 99)) {
      BlockPos lv6 = new BlockPos(arg.getInt("SleepingX"), arg.getInt("SleepingY"), arg.getInt("SleepingZ"));
      this.setSleepingPos(lv6);
      this.entityData.set(DATA_POSE, Pose.SLEEPING);
      if (!this.firstTick) {
        this.setPosToBed(lv6);
      }
    }

    if (arg.contains("Brain", 10)) {
      this.brain = this.makeBrain(new Dynamic(NbtOps.INSTANCE, arg.get("Brain")));
    }
  }

  protected void tickEffects() {
    Iterator<Holder<MobEffect>> iterator = this.activeEffects.keySet().iterator();

    try {
      while (iterator.hasNext()) {
        Holder<MobEffect> lv = iterator.next();
        MobEffectInstance lv2 = this.activeEffects.get(lv);
        if (!lv2.tick(this, () -> this.onEffectUpdated(lv2, true, null))) {
        } else if (lv2.getDuration() % 600 == 0) {
          this.onEffectUpdated(lv2, false, null);
        }
      }
    } catch (ConcurrentModificationException var6) {
    }

    if (this.effectsDirty) {
      this.effectsDirty = false;
    }

    List<ParticleOptions> list = this.entityData.get(DATA_EFFECT_PARTICLES);
    if (!list.isEmpty()) {
      boolean bl = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
      int i = this.isInvisible() ? 15 : 4;
      int j = bl ? 5 : 1;
      if (this.random.nextInt(i * j) == 0) {
        this.level().addParticle(Util.getRandom(list, this.random), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 1.0, 1.0, 1.0);
      }
    }
  }

  protected void updateInvisibilityStatus() {
    if (this.activeEffects.isEmpty()) {
      this.removeEffectParticles();
      this.setInvisible(false);
    } else {
      this.setInvisible(this.hasEffect(EffectType.INVISIBILITY));
      this.updateSynchronizedMobEffectParticles();
    }
  }

  private void updateSynchronizedMobEffectParticles() {
    List<ParticleOptions> list = this.activeEffects
      .values()
      .stream()
      .filter(MobEffectInstance::isVisible)
      .map(MobEffectInstance::getParticleOptions)
      .toList();
    this.entityData.set(DATA_EFFECT_PARTICLES, list);
    this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(this.activeEffects.values()));
  }

  private void updateGlowingStatus() {
    boolean bl = this.isCurrentlyGlowing();
    if (this.getSharedFlag(6) != bl) {
      this.setSharedFlag(6, bl);
    }
  }

  public double getVisibilityPercent(@Nullable Entity arg) {
    double d = 1.0;
    if (this.isDiscrete()) {
      d *= 0.8;
    }

    if (this.isInvisible()) {
      float f = this.getArmorCoverPercentage();
      if (f < 0.1F) {
        f = 0.1F;
      }

      d *= 0.7 * (double)f;
    }

    if (arg != null) {
      ItemStack lv = this.getItemBySlot(EquipmentSlot.HEAD);
      EntityType<?> lv2 = arg.getType();
      if (lv2 == EntityType.SKELETON && lv.is(Items.SKELETON_SKULL)
        || lv2 == EntityType.ZOMBIE && lv.is(Items.ZOMBIE_HEAD)
        || lv2 == EntityType.PIGLIN && lv.is(Items.PIGLIN_HEAD)
        || lv2 == EntityType.PIGLIN_BRUTE && lv.is(Items.PIGLIN_HEAD)
        || lv2 == EntityType.CREEPER && lv.is(Items.CREEPER_HEAD)) {
        d *= 0.5;
      }
    }

    return d;
  }

  public boolean canAttack(LivingEntity arg) {
    return arg instanceof Player && this.level().getDifficulty() == Difficulty.PEACEFUL ? false : arg.canBeSeenAsEnemy();
  }

  public boolean canAttack(LivingEntity arg, TargetingConditions arg2) {
    return arg2.test(this, arg);
  }

  public boolean canBeSeenAsEnemy() {
    return !this.isInvulnerable() && this.canBeSeenByAnyone();
  }

  public boolean canBeSeenByAnyone() {
    return !this.isSpectator() && this.isAlive();
  }

  public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> collection) {
    for (MobEffectInstance lv : collection) {
      if (lv.isVisible() && !lv.isAmbient()) {
        return false;
      }
    }

    return true;
  }

  protected void removeEffectParticles() {
    this.entityData.set(DATA_EFFECT_PARTICLES, List.of());
  }

  public boolean removeAllEffects() {
    return false;
  }

  public Collection<MobEffectInstance> getActiveEffects() {
    return this.activeEffects.values();
  }

  public Map<Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap() {
    return this.activeEffects;
  }

  public boolean hasEffect(EffectType arg) {
    return this.activeEffects.containsKey(arg);
  }

  @Nullable
  public MobEffectInstance getEffect(Holder<MobEffect> arg) {
    return this.activeEffects.get(arg);
  }

  public final boolean addEffect(MobEffectInstance arg) {
    return this.addEffect(arg, null);
  }

  public boolean addEffect(MobEffectInstance arg, @Nullable Entity arg2) {
    if (!this.canBeAffected(arg)) {
      return false;
    } else {
      MobEffectInstance lv = this.activeEffects.get(arg.getEffect());
      boolean bl = false;
      if (lv == null) {
        this.activeEffects.put(arg.getEffect(), arg);
        this.onEffectAdded(arg, arg2);
        bl = true;
        arg.onEffectAdded(this);
      } else if (lv.update(arg)) {
        this.onEffectUpdated(lv, true, arg2);
        bl = true;
      }

      arg.onEffectStarted(this);
      return bl;
    }
  }

  public boolean canBeAffected(MobEffectInstance arg) {
    if (this.getType().is(EntityTypeTags.IMMUNE_TO_INFESTED)) {
      return !arg.is(EffectType.INFESTED);
    } else if (this.getType().is(EntityTypeTags.IMMUNE_TO_OOZING)) {
      return !arg.is(EffectType.OOZING);
    } else {
      return !this.getType().is(EntityTypeTags.IGNORES_POISON_AND_REGEN) ? true : !arg.is(EffectType.REGENERATION) && !arg.is(EffectType.POISON);
    }
  }

  public void forceAddEffect(MobEffectInstance arg, @Nullable Entity arg2) {
    if (this.canBeAffected(arg)) {
      MobEffectInstance lv = this.activeEffects.put(arg.getEffect(), arg);
      if (lv == null) {
        this.onEffectAdded(arg, arg2);
      } else {
        arg.copyBlendState(lv);
        this.onEffectUpdated(arg, true, arg2);
      }
    }
  }

  public boolean isInvertedHealAndHarm() {
    return this.getType().is(EntityTypeTags.INVERTED_HEALING_AND_HARM);
  }

  @Nullable
  public MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> arg) {
    return this.activeEffects.remove(arg);
  }

  public boolean removeEffect(Holder<MobEffect> arg) {
    MobEffectInstance lv = this.removeEffectNoUpdate(arg);
    if (lv != null) {
      this.onEffectRemoved(lv);
      return true;
    } else {
      return false;
    }
  }

  protected void onEffectAdded(MobEffectInstance arg, @Nullable Entity arg2) {
    this.effectsDirty = true;
  }

  protected void onEffectUpdated(MobEffectInstance arg, boolean bl, @Nullable Entity arg2) {
    this.effectsDirty = true;
  }

  protected void onEffectRemoved(MobEffectInstance arg) {
    this.effectsDirty = true;
  }

  private void refreshDirtyAttributes() {
    Set<AttributeInstance> set = this.getAttributes().getAttributesToUpdate();

    for (AttributeInstance lv : set) {
      this.onAttributeUpdated(lv.getAttribute());
    }

    set.clear();
  }

  private void onAttributeUpdated(Holder<Attribute> arg) {
    if (arg.is(Attributes.MAX_HEALTH)) {
      float f = this.getMaxHealth();
      if (this.getHealth() > f) {
        this.setHealth(f);
      }
    } else if (arg.is(Attributes.MAX_ABSORPTION)) {
      float f = this.getMaxAbsorption();
      if (this.getAbsorptionAmount() > f) {
        this.setAbsorptionAmount(f);
      }
    }
  }

  public void heal(float f) {
    float g = this.getHealth();
    if (g > 0.0F) {
      this.setHealth(g + f);
    }
  }

  public float getHealth() {
    return this.entityData.get(DATA_HEALTH_ID);
  }

  public void setHealth(float f) {
    this.entityData.set(DATA_HEALTH_ID, Mth.clamp(f, 0.0F, this.getMaxHealth()));
  }

  public boolean isDeadOrDying() {
    return this.getHealth() <= 0.0F;
  }

  @Override
  public boolean hurt(DamageSource arg, float f) {
    if (this.isInvulnerableTo(arg)) {
      return false;
    } else {
      return false;
    }
  }

  protected void blockUsingShield(LivingEntity arg) {
    arg.blockedByShield(this);
  }

  protected void blockedByShield(LivingEntity arg) {
    arg.knockback(0.5, arg.getX() - this.getX(), arg.getZ() - this.getZ());
  }

  private boolean checkTotemDeathProtection(DamageSource arg) {
    if (arg.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
      return false;
    } else {
      ItemStack lv = null;

      for (InteractionHand lv2 : InteractionHand.values()) {
        ItemStack lv3 = this.getItemInHand(lv2);
        if (lv3.is(Items.TOTEM_OF_UNDYING)) {
          lv = lv3.copy();
          lv3.shrink(1);
          break;
        }
      }

      if (lv != null) {
        this.setHealth(1.0F);
        this.removeAllEffects();
        this.addEffect(new MobEffectInstance(EffectType.REGENERATION, 900, 1));
        this.addEffect(new MobEffectInstance(EffectType.ABSORPTION, 100, 1));
        this.addEffect(new MobEffectInstance(EffectType.FIRE_RESISTANCE, 800, 0));
        this.level().broadcastEntityEvent(this, (byte)35);
      }

      return lv != null;
    }
  }

  @Nullable
  public DamageSource getLastDamageSource() {
    if (this.level().getGameTime() - this.lastDamageStamp > 40L) {
      this.lastDamageSource = null;
    }

    return this.lastDamageSource;
  }

  public boolean isDamageSourceBlocked(DamageSource arg) {
    Entity lv = arg.getDirectEntity();
    boolean bl = false;
    if (lv instanceof AbstractArrow lv2 && lv2.getPierceLevel() > 0) {
      bl = true;
    }

    if (!arg.is(DamageTypeTags.BYPASSES_SHIELD) && this.isBlocking() && !bl) {
      Vec3 lv3 = arg.getSourcePosition();
      if (lv3 != null) {
        Vec3 lv4 = this.calculateViewVector(0.0F, this.getYHeadRot());
        Vec3 lv5 = lv3.vectorTo(this.position());
        lv5 = new Vec3(lv5.x, 0.0, lv5.z).normalize();
        return lv5.dot(lv4) < 0.0;
      }
    }

    return false;
  }

  private void breakItem(ItemStack arg) {
    if (!arg.isEmpty()) {
      if (!this.isSilent()) {
        this.level()
          .playLocalSound(
            this.getX(),
            this.getY(),
            this.getZ(),
            arg.getBreakingSound(),
            this.getSoundSource(),
            0.8F,
            0.8F + this.level().random.nextFloat() * 0.4F,
            false
          );
      }

      this.spawnItemParticles(arg, 5);
    }
  }

  public void die(DamageSource arg) {
    if (!this.isRemoved() && !this.dead) {
      Entity lv = arg.getEntity();
      LivingEntity lv2 = this.getKillCredit();
      if (this.deathScore >= 0 && lv2 != null) {
        lv2.awardKillScore(this, this.deathScore, arg);
      }

      if (this.isSleeping()) {
        this.stopSleeping();
      }

      this.dead = true;
      this.getCombatTracker().recheckStatus();

      this.setPose(Pose.DYING);
    }
  }

  protected void createWitherRose(@Nullable LivingEntity arg) {
  }

  protected void dropEquipment() {
  }

  protected void dropExperience(@Nullable Entity arg) {
  }

  public ResourceKey<LootTable> getLootTable() {
    return this.getType().getDefaultLootTable();
  }

  public long getLootTableSeed() {
    return 0L;
  }

  protected float getKnockback(Entity arg, DamageSource arg2) {
    return (float) this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
  }

  public void knockback(double d, double e, double f) {
    d *= 1.0 - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
    if (!(d <= 0.0)) {
      this.hasImpulse = true;
      Vec3 lv = this.getDeltaMovement();

      while (e * e + f * f < 1.0E-5F) {
        e = (Math.random() - Math.random()) * 0.01;
        f = (Math.random() - Math.random()) * 0.01;
      }

      Vec3 lv2 = new Vec3(e, 0.0, f).normalize().scale(d);
      this.setDeltaMovement(lv.x / 2.0 - lv2.x, this.onGround() ? Math.min(0.4, lv.y / 2.0 + d) : lv.y, lv.z / 2.0 - lv2.z);
    }
  }

  public void indicateDamage(double d, double e) {
  }

  public void skipDropExperience() {
    this.skipDropExperience = true;
  }

  public boolean wasExperienceConsumed() {
    return this.skipDropExperience;
  }

  public float getHurtDir() {
    return 0.0F;
  }

  protected AABB getHitbox() {
    AABB lv = this.getBoundingBox();
    Entity lv2 = this.getVehicle();
    if (lv2 != null) {
      Vec3 lv3 = lv2.getPassengerRidingPosition(this);
      return lv.setMinY(Math.max(lv3.y, lv.minY));
    } else {
      return lv;
    }
  }

  public Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEnchantments() {
    return this.activeLocationDependentEnchantments;
  }

  public Optional<BlockPos> getLastClimbablePos() {
    return this.lastClimbablePos;
  }

  public boolean onClimbable() {
    if (this.isSpectator()) {
      return false;
    } else {
      BlockPos lv = this.blockPosition();
      BlockState lv2 = this.getInBlockState();
      if (lv2.is(BlockTags.CLIMBABLE)) {
        this.lastClimbablePos = Optional.of(lv);
        return true;
      } else if (lv2.getBlock() instanceof TrapDoorBlock && this.trapdoorUsableAsLadder(lv, lv2)) {
        this.lastClimbablePos = Optional.of(lv);
        return true;
      } else {
        return false;
      }
    }
  }

  private boolean trapdoorUsableAsLadder(BlockPos arg, BlockState arg2) {
    if (!arg2.getValue(TrapDoorBlock.OPEN)) {
      return false;
    } else {
      BlockState lv = this.level().getBlockState(arg.below());
      return lv.is(Blocks.LADDER) && lv.getValue(LadderBlock.FACING) == arg2.getValue(TrapDoorBlock.FACING);
    }
  }

  @Override
  public boolean isAlive() {
    return !this.isRemoved() && this.getHealth() > 0.0F;
  }

  @Override
  public int getMaxFallDistance() {
    return this.getComfortableFallDistance(0.0F);
  }

  protected final int getComfortableFallDistance(float f) {
    return Mth.floor(f + 3.0F);
  }

  @Override
  public boolean causeFallDamage(float f, float g, DamageSource arg) {
    boolean bl = super.causeFallDamage(f, g, arg);
    int i = this.calculateFallDamage(f, g);
    if (i > 0) {
      this.playSound(this.getFallDamageSound(i), 1.0F, 1.0F);
      this.playBlockFallSound();
      this.hurt(arg, (float)i);
      return true;
    } else {
      return bl;
    }
  }

  protected int calculateFallDamage(float f, float g) {
    if (this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
      return 0;
    } else {
      float h = (float)this.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
      float i = f - h;
      return Mth.ceil((double)(i * g) * this.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
    }
  }

  protected void playBlockFallSound() {
    if (!this.isSilent()) {
      int i = Mth.floor(this.getX());
      int j = Mth.floor(this.getY() - 0.2F);
      int k = Mth.floor(this.getZ());
      BlockState lv = this.level().getBlockState(new BlockPos(i, j, k));
      if (!lv.isAir()) {
        SoundType lv2 = lv.getSoundType();
        this.playSound(lv2.getFallSound(), lv2.getVolume() * 0.5F, lv2.getPitch() * 0.75F);
      }
    }
  }

  @Override
  public void animateHurt(float f) {
    this.hurtDuration = 10;
    this.hurtTime = this.hurtDuration;
  }

  public int getArmorValue() {
    return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
  }

  protected void hurtArmor(DamageSource arg, float f) {
  }

  protected void hurtHelmet(DamageSource arg, float f) {
  }

  protected void hurtCurrentlyUsedShield(float f) {
  }

  protected void doHurtEquipment(DamageSource arg, float f, EquipmentSlot... args) {
    if (!(f <= 0.0F)) {
      int i = (int)Math.max(1.0F, f / 4.0F);

      for (EquipmentSlot lv : args) {
        ItemStack lv2 = this.getItemBySlot(lv);
        if (lv2.getItem() instanceof ArmorItem && lv2.canBeHurtBy(arg)) {
          lv2.hurtAndBreak(i, this, lv);
        }
      }
    }
  }

  protected float getDamageAfterArmorAbsorb(DamageSource arg, float f) {
    if (!arg.is(DamageTypeTags.BYPASSES_ARMOR)) {
      this.hurtArmor(arg, f);
      f = CombatRules.getDamageAfterAbsorb(this, f, arg, (float)this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    return f;
  }

  protected float getDamageAfterMagicAbsorb(DamageSource arg, float f) {
    if (arg.is(DamageTypeTags.BYPASSES_EFFECTS)) {
      return f;
    } else {
      if (this.hasEffect(EffectType.DAMAGE_RESISTANCE) && !arg.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
        int i = (this.getEffect(EffectType.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
        int j = 25 - i;
        float g = f * (float)j;
        f = Math.max(g / 25.0F, 0.0F);
      }

      if (f <= 0.0F) {
        return 0.0F;
      } else if (arg.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
        return f;
      } else {
        return f;
      }
    }
  }

  protected void actuallyHurt(DamageSource arg, float f) {
    if (!this.isInvulnerableTo(arg)) {
      f = this.getDamageAfterArmorAbsorb(arg, f);
      f = this.getDamageAfterMagicAbsorb(arg, f);
      float var9 = Math.max(f - this.getAbsorptionAmount(), 0.0F);
      this.setAbsorptionAmount(this.getAbsorptionAmount() - (f - var9));

      if (var9 != 0.0F) {
        this.getCombatTracker().recordDamage(arg, var9);
        this.setHealth(this.getHealth() - var9);
        this.setAbsorptionAmount(this.getAbsorptionAmount() - var9);
        this.gameEvent(GameEvent.ENTITY_DAMAGE);
      }
    }
  }

  public CombatTracker getCombatTracker() {
    return this.combatTracker;
  }

  @Nullable
  public LivingEntity getKillCredit() {
    if (this.lastHurtByPlayer != null) {
      return this.lastHurtByPlayer;
    } else {
      return this.lastHurtByMob != null ? this.lastHurtByMob : null;
    }
  }

  public final float getMaxHealth() {
    return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
  }

  public final float getMaxAbsorption() {
    return (float)this.getAttributeValue(Attributes.MAX_ABSORPTION);
  }

  public final int getArrowCount() {
    return this.entityData.get(DATA_ARROW_COUNT_ID);
  }

  public final void setArrowCount(int i) {
    this.entityData.set(DATA_ARROW_COUNT_ID, i);
  }

  public final int getStingerCount() {
    return this.entityData.get(DATA_STINGER_COUNT_ID);
  }

  public final void setStingerCount(int i) {
    this.entityData.set(DATA_STINGER_COUNT_ID, i);
  }

  private int getCurrentSwingDuration() {
    if (MobEffectUtil.hasDigSpeed(this)) {
      return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this));
    } else {
      return this.hasEffect(EffectType.DIG_SLOWDOWN) ? 6 + (1 + this.getEffect(EffectType.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
    }
  }

  public void swing(InteractionHand arg) {
    this.swing(arg, false);
  }

  public void swing(InteractionHand arg, boolean bl) {
    if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
      this.swingTime = -1;
      this.swinging = true;
      this.swingingArm = arg;
    }
  }

  @Override
  public void handleDamageEvent(DamageSource arg) {
    this.walkAnimation.setSpeed(1.5F);
    this.invulnerableTime = 20;
    this.hurtDuration = 10;
    this.hurtTime = this.hurtDuration;

    this.hurt(this.damageSources().generic(), 0.0F);
    this.lastDamageSource = arg;
    this.lastDamageStamp = this.level().getGameTime();
  }

  @Override
  public void handleEntityEvent(byte b) {
    switch (b) {
      case 3:
        if (!(this instanceof Player)) {
          this.setHealth(0.0F);
          this.die(this.damageSources().generic());
        }
        break;
      case 29:
        break;
      case 30:
        break;
      case 46:
        int i = 128;

        for (int j = 0; j < 128; j++) {
          double d = (double)j / 127.0;
          float f = (this.random.nextFloat() - 0.5F) * 0.2F;
          float g = (this.random.nextFloat() - 0.5F) * 0.2F;
          float h = (this.random.nextFloat() - 0.5F) * 0.2F;
          double e = Mth.lerp(d, this.xo, this.getX()) + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth() * 2.0;
          double k = Mth.lerp(d, this.yo, this.getY()) + this.random.nextDouble() * (double)this.getBbHeight();
          double l = Mth.lerp(d, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth() * 2.0;
          this.level().addParticle(ParticleTypes.PORTAL, e, k, l, (double)f, (double)g, (double)h);
        }
        break;
      case 47:
        this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
        break;
      case 48:
        this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
        break;
      case 49:
        this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
        break;
      case 50:
        this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
        break;
      case 51:
        this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
        break;
      case 52:
        this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
        break;
      case 54:
        HoneyBlock.showJumpParticles(this);
        break;
      case 55:
        this.swapHandItems();
        break;
      case 60:
        this.makePoofParticles();
        break;
      case 65:
        this.breakItem(this.getItemBySlot(EquipmentSlot.BODY));
        break;
      default:
        super.handleEntityEvent(b);
    }
  }

  private void makePoofParticles() {
    for (int i = 0; i < 20; i++) {
      double d = this.random.nextGaussian() * 0.02;
      double e = this.random.nextGaussian() * 0.02;
      double f = this.random.nextGaussian() * 0.02;
      this.level().addParticle(ParticleTypes.POOF, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), d, e, f);
    }
  }

  private void swapHandItems() {
    ItemStack lv = this.getItemBySlot(EquipmentSlot.OFFHAND);
    this.setItemSlot(EquipmentSlot.OFFHAND, this.getItemBySlot(EquipmentSlot.MAINHAND));
    this.setItemSlot(EquipmentSlot.MAINHAND, lv);
  }

  @Override
  protected void onBelowWorld() {
    this.hurt(this.damageSources().fellOutOfWorld(), 4.0F);
  }

  protected void updateSwingTime() {
    int i = this.getCurrentSwingDuration();
    if (this.swinging) {
      this.swingTime++;
      if (this.swingTime >= i) {
        this.swingTime = 0;
        this.swinging = false;
      }
    } else {
      this.swingTime = 0;
    }

    this.attackAnim = (float)this.swingTime / (float)i;
  }

  @Nullable
  public AttributeInstance getAttribute(Holder<Attribute> arg) {
    return this.getAttributes().getInstance(arg);
  }

  public double getAttributeValue(Holder<Attribute> arg) {
    return this.getAttributes().getValue(arg);
  }

  public double getAttributeBaseValue(Holder<Attribute> arg) {
    return this.getAttributes().getBaseValue(arg);
  }

  public AttributeMap getAttributes() {
    return this.attributes;
  }

  public ItemStack getMainHandItem() {
    return this.getItemBySlot(EquipmentSlot.MAINHAND);
  }

  public ItemStack getOffhandItem() {
    return this.getItemBySlot(EquipmentSlot.OFFHAND);
  }

  @NotNull
  @Override
  public ItemStack getWeaponItem() {
    return this.getMainHandItem();
  }

  public boolean isHolding(Item arg) {
    return this.isHolding(arg2 -> arg2.is(arg));
  }

  public boolean isHolding(Predicate<ItemStack> predicate) {
    return predicate.test(this.getMainHandItem()) || predicate.test(this.getOffhandItem());
  }

  public ItemStack getItemInHand(InteractionHand arg) {
    if (arg == InteractionHand.MAIN_HAND) {
      return this.getItemBySlot(EquipmentSlot.MAINHAND);
    } else if (arg == InteractionHand.OFF_HAND) {
      return this.getItemBySlot(EquipmentSlot.OFFHAND);
    } else {
      throw new IllegalArgumentException("Invalid hand " + arg);
    }
  }

  public void setItemInHand(InteractionHand arg, ItemStack arg2) {
    if (arg == InteractionHand.MAIN_HAND) {
      this.setItemSlot(EquipmentSlot.MAINHAND, arg2);
    } else {
      if (arg != InteractionHand.OFF_HAND) {
        throw new IllegalArgumentException("Invalid hand " + arg);
      }

      this.setItemSlot(EquipmentSlot.OFFHAND, arg2);
    }
  }

  public boolean hasItemInSlot(EquipmentSlot arg) {
    return !this.getItemBySlot(arg).isEmpty();
  }

  public boolean canUseSlot(EquipmentSlot arg) {
    return false;
  }

  public abstract Iterable<ItemStack> getArmorSlots();

  public abstract ItemStack getItemBySlot(EquipmentSlot arg);

  public abstract void setItemSlot(EquipmentSlot arg, ItemStack arg2);

  public Iterable<ItemStack> getHandSlots() {
    return List.of();
  }

  public Iterable<ItemStack> getArmorAndBodyArmorSlots() {
    return this.getArmorSlots();
  }

  public Iterable<ItemStack> getAllSlots() {
    return Iterables.concat(this.getHandSlots(), this.getArmorAndBodyArmorSlots());
  }

  protected void verifyEquippedItem(ItemStack arg) {
    arg.getItem().verifyComponentsAfterLoad(arg);
  }

  public float getArmorCoverPercentage() {
    Iterable<ItemStack> iterable = this.getArmorSlots();
    int i = 0;
    int j = 0;

    for (ItemStack lv : iterable) {
      if (!lv.isEmpty()) {
        j++;
      }

      i++;
    }

    return i > 0 ? (float)j / (float)i : 0.0F;
  }

  @Override
  public void setSprinting(boolean bl) {
    super.setSprinting(bl);
    AttributeInstance lv = this.getAttribute(Attributes.MOVEMENT_SPEED);
    lv.removeModifier(SPEED_MODIFIER_SPRINTING.id());
    if (bl) {
      lv.addTransientModifier(SPEED_MODIFIER_SPRINTING);
    }
  }

  protected float getSoundVolume() {
    return 1.0F;
  }

  public float getVoicePitch() {
    return this.isBaby()
      ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F
      : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
  }

  protected boolean isImmobile() {
    return this.isDeadOrDying();
  }

  @Override
  public void push(Entity arg) {
    if (!this.isSleeping()) {
      super.push(arg);
    }
  }

  private void dismountVehicle(Entity arg) {
    Vec3 lv;
    if (this.isRemoved()) {
      lv = this.position();
    } else if (!arg.isRemoved() && !this.level().getBlockState(arg.blockPosition()).is(BlockTags.PORTALS)) {
      lv = arg.getDismountLocationForPassenger(this);
    } else {
      double d = Math.max(this.getY(), arg.getY());
      lv = new Vec3(this.getX(), d, this.getZ());
    }

    this.dismountTo(lv.x, lv.y, lv.z);
  }

  @Override
  public boolean shouldShowName() {
    return this.isCustomNameVisible();
  }

  protected float getJumpPower() {
    return this.getJumpPower(1.0F);
  }

  protected float getJumpPower(float f) {
    return (float)this.getAttributeValue(Attributes.JUMP_STRENGTH) * f * this.getBlockJumpFactor() + this.getJumpBoostPower();
  }

  public float getJumpBoostPower() {
    return this.hasEffect(EffectType.JUMP) ? 0.1F * ((float)this.getEffect(EffectType.JUMP).getAmplifier() + 1.0F) : 0.0F;
  }

  @VisibleForTesting
  public void jumpFromGround() {
    float f = this.getJumpPower();
    if (!(f <= 1.0E-5F)) {
      Vec3 lv = this.getDeltaMovement();
      this.setDeltaMovement(lv.x, (double)f, lv.z);
      if (this.isSprinting()) {
        float g = this.getYRot() * (float) (Math.PI / 180.0);
        this.addDeltaMovement(new Vec3((double)(-Mth.sin(g)) * 0.2, 0.0, (double)Mth.cos(g) * 0.2));
      }

      this.hasImpulse = true;
    }
  }

  protected void goDownInWater() {
    this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04F, 0.0));
  }

  protected void jumpInLiquid(TagKey<Fluid> arg) {
    this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04F, 0.0));
  }

  protected float getWaterSlowDown() {
    return 0.8F;
  }

  public boolean canStandOnFluid(FluidState arg) {
    return false;
  }

  @Override
  protected double getDefaultGravity() {
    return this.getAttributeValue(Attributes.GRAVITY);
  }

  public void travel(Vec3 arg) {
    if (this.isControlledByLocalInstance()) {
      double d = this.getGravity();
      boolean bl = this.getDeltaMovement().y <= 0.0;
      if (bl && this.hasEffect(EffectType.SLOW_FALLING)) {
        d = Math.min(d, 0.01);
      }

      FluidState lv = this.level().getFluidState(this.blockPosition());
      if (this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(lv)) {
        double e = this.getY();
        float f = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
        float g = 0.02F;
        float h = (float)this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (!this.onGround()) {
          h *= 0.5F;
        }

        if (h > 0.0F) {
          f += (0.54600006F - f) * h;
          g += (this.getSpeed() - g) * h;
        }

        if (this.hasEffect(EffectType.DOLPHINS_GRACE)) {
          f = 0.96F;
        }

        this.moveRelative(g, arg);
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 lv2 = this.getDeltaMovement();
        if (this.horizontalCollision && this.onClimbable()) {
          lv2 = new Vec3(lv2.x, 0.2, lv2.z);
        }

        this.setDeltaMovement(lv2.multiply((double)f, 0.8F, (double)f));
        Vec3 lv3 = this.getFluidFallingAdjustedMovement(d, bl, this.getDeltaMovement());
        this.setDeltaMovement(lv3);
        if (this.horizontalCollision && this.isFree(lv3.x, lv3.y + 0.6F - this.getY() + e, lv3.z)) {
          this.setDeltaMovement(lv3.x, 0.3F, lv3.z);
        }
      } else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(lv)) {
        double ex = this.getY();
        this.moveRelative(0.02F, arg);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
          this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.8F, 0.5));
          Vec3 lv4 = this.getFluidFallingAdjustedMovement(d, bl, this.getDeltaMovement());
          this.setDeltaMovement(lv4);
        } else {
          this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        }

        if (d != 0.0) {
          this.setDeltaMovement(this.getDeltaMovement().add(0.0, -d / 4.0, 0.0));
        }

        Vec3 lv4 = this.getDeltaMovement();
        if (this.horizontalCollision && this.isFree(lv4.x, lv4.y + 0.6F - this.getY() + ex, lv4.z)) {
          this.setDeltaMovement(lv4.x, 0.3F, lv4.z);
        }
      } else if (this.isFallFlying()) {
        this.checkSlowFallDistance();
        Vec3 lv5 = this.getDeltaMovement();
        Vec3 lv6 = this.getLookAngle();
        float fx = this.getXRot() * (float) (Math.PI / 180.0);
        double i = Math.sqrt(lv6.x * lv6.x + lv6.z * lv6.z);
        double j = lv5.horizontalDistance();
        double k = lv6.length();
        double l = Math.cos((double)fx);
        l = l * l * Math.min(1.0, k / 0.4);
        lv5 = this.getDeltaMovement().add(0.0, d * (-1.0 + l * 0.75), 0.0);
        if (lv5.y < 0.0 && i > 0.0) {
          double m = lv5.y * -0.1 * l;
          lv5 = lv5.add(lv6.x * m / i, m, lv6.z * m / i);
        }

        if (fx < 0.0F && i > 0.0) {
          double m = j * (double)(-Mth.sin(fx)) * 0.04;
          lv5 = lv5.add(-lv6.x * m / i, m * 3.2, -lv6.z * m / i);
        }

        if (i > 0.0) {
          lv5 = lv5.add((lv6.x / i * j - lv5.x) * 0.1, 0.0, (lv6.z / i * j - lv5.z) * 0.1);
        }

        this.setDeltaMovement(lv5.multiply(0.99F, 0.98F, 0.99F));
        this.move(MoverType.SELF, this.getDeltaMovement());
      } else {
        BlockPos lv7 = this.getBlockPosBelowThatAffectsMyMovement();
        float p = this.level().getBlockState(lv7).getBlock().getFriction();
        float fxx = this.onGround() ? p * 0.91F : 0.91F;
        Vec3 lv8 = this.handleRelativeFrictionAndCalculateMovement(arg, p);
        double q = lv8.y;
        if (this.hasEffect(EffectType.LEVITATION)) {
          q += (0.05 * (double)(this.getEffect(EffectType.LEVITATION).getAmplifier() + 1) - lv8.y) * 0.2;
        } else if (this.level().hasChunkAt(lv7)) {
          q -= d;
        } else if (this.getY() > (double)this.level().getMinBuildHeight()) {
          q = -0.1;
        } else {
          q = 0.0;
        }

        if (this.shouldDiscardFriction()) {
          this.setDeltaMovement(lv8.x, q, lv8.z);
        } else {
          this.setDeltaMovement(lv8.x * (double)fxx, this instanceof FlyingAnimal ? q * (double)fxx : q * 0.98F, lv8.z * (double)fxx);
        }
      }
    }

    this.calculateEntityAnimation(this instanceof FlyingAnimal);
  }

  private void travelRidden(Player arg, Vec3 arg2) {
    Vec3 lv = this.getRiddenInput(arg, arg2);
    this.tickRidden(arg, lv);
    if (this.isControlledByLocalInstance()) {
      this.setSpeed(this.getRiddenSpeed(arg));
      this.travel(lv);
    } else {
      this.calculateEntityAnimation(false);
      this.setDeltaMovement(Vec3.ZERO);
      this.tryCheckInsideBlocks();
    }
  }

  protected void tickRidden(Player arg, Vec3 arg2) {
  }

  protected Vec3 getRiddenInput(Player arg, Vec3 arg2) {
    return arg2;
  }

  protected float getRiddenSpeed(Player arg) {
    return this.getSpeed();
  }

  public void calculateEntityAnimation(boolean bl) {
    float f = (float)Mth.length(this.getX() - this.xo, bl ? this.getY() - this.yo : 0.0, this.getZ() - this.zo);
    this.updateWalkAnimation(f);
  }

  protected void updateWalkAnimation(float f) {
    float g = Math.min(f * 4.0F, 1.0F);
    this.walkAnimation.update(g, 0.4F);
  }

  public Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 arg, float f) {
    this.moveRelative(this.getFrictionInfluencedSpeed(f), arg);
    this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
    this.move(MoverType.SELF, this.getDeltaMovement());
    Vec3 lv = this.getDeltaMovement();
    if ((this.horizontalCollision || this.jumping)
      && (this.onClimbable() || this.getInBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
      lv = new Vec3(lv.x, 0.2, lv.z);
    }

    return lv;
  }

  public Vec3 getFluidFallingAdjustedMovement(double d, boolean bl, Vec3 arg) {
    if (d != 0.0 && !this.isSprinting()) {
      double e;
      if (bl && Math.abs(arg.y - 0.005) >= 0.003 && Math.abs(arg.y - d / 16.0) < 0.003) {
        e = -0.003;
      } else {
        e = arg.y - d / 16.0;
      }

      return new Vec3(arg.x, e, arg.z);
    } else {
      return arg;
    }
  }

  private Vec3 handleOnClimbable(Vec3 arg) {
    if (this.onClimbable()) {
      this.resetFallDistance();
      float f = 0.15F;
      double d = Mth.clamp(arg.x, -0.15F, 0.15F);
      double e = Mth.clamp(arg.z, -0.15F, 0.15F);
      double g = Math.max(arg.y, -0.15F);
      if (g < 0.0 && !this.getInBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
        g = 0.0;
      }

      arg = new Vec3(d, g, e);
    }

    return arg;
  }

  private float getFrictionInfluencedSpeed(float f) {
    return this.onGround() ? this.getSpeed() * (0.21600002F / (f * f * f)) : this.getFlyingSpeed();
  }

  protected float getFlyingSpeed() {
    return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1F : 0.02F;
  }

  public float getSpeed() {
    return this.speed;
  }

  public void setSpeed(float f) {
    this.speed = f;
  }

  public boolean doHurtTarget(Entity arg) {
    this.setLastHurtMob(arg);
    return false;
  }

  @Override
  public void tick() {
    super.tick();
    this.updatingUsingItem();
    this.updateSwimAmount();

    if (!this.isRemoved()) {
      this.aiStep();
    }

    double d = this.getX() - this.xo;
    double e = this.getZ() - this.zo;
    float f = (float)(d * d + e * e);
    float g = this.yBodyRot;
    float h = 0.0F;
    this.oRun = this.run;
    float k = 0.0F;
    if (f > 0.0025000002F) {
      k = 1.0F;
      h = (float)Math.sqrt((double)f) * 3.0F;
      float l = (float)Mth.atan2(e, d) * (180.0F / (float)Math.PI) - 90.0F;
      float m = Mth.abs(Mth.wrapDegrees(this.getYRot()) - l);
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

    this.run = this.run + (k - this.run) * 0.3F;
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
      this.fallFlyTicks++;
    } else {
      this.fallFlyTicks = 0;
    }

    if (this.isSleeping()) {
      this.setXRot(0.0F);
    }

    this.refreshDirtyAttributes();
    float l = this.getScale();
    if (l != this.appliedScale) {
      this.appliedScale = l;
      this.refreshDimensions();
    }
  }

  private void detectEquipmentUpdates() {
    Map<EquipmentSlot, ItemStack> map = this.collectEquipmentChanges();
    if (map != null) {
      this.handleHandSwap(map);
      if (!map.isEmpty()) {
        this.handleEquipmentChanges(map);
      }
    }
  }

  @Nullable
  private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
    Map<EquipmentSlot, ItemStack> map = null;

    for (EquipmentSlot lv : EquipmentSlot.values()) {
      ItemStack lv2 = switch (lv.getType()) {
        case HAND -> this.getLastHandItem(lv);
        case HUMANOID_ARMOR -> this.getLastArmorItem(lv);
        case ANIMAL_ARMOR -> this.lastBodyItemStack;
      };
      ItemStack lv3 = this.getItemBySlot(lv);
      if (this.equipmentHasChanged(lv2, lv3)) {
        if (map == null) {
          map = Maps.newEnumMap(EquipmentSlot.class);
        }

        map.put(lv, lv3);
        AttributeMap lv4 = this.getAttributes();
        if (!lv2.isEmpty()) {
          lv2.forEachModifier(lv, (arg4, arg5) -> {
            AttributeInstance lvx = lv4.getInstance(arg4);
            if (lvx != null) {
              lvx.removeModifier(arg5);
            }

            EnchantmentHelper.stopLocationBasedEffects(lv2, this, lv);
          });
        }
      }
    }

    if (map != null) {
      for (Entry<EquipmentSlot, ItemStack> entry : map.entrySet()) {
        EquipmentSlot lv5 = entry.getKey();
        ItemStack lv6 = entry.getValue();
        if (!lv6.isEmpty()) {
          lv6.forEachModifier(lv5, (arg3, arg4) -> {
            AttributeInstance lv = this.attributes.getInstance(arg3);
            if (lv != null) {
              lv.removeModifier(arg4.id());
              lv.addTransientModifier(arg4);
            }
          });
        }
      }
    }

    return map;
  }

  public boolean equipmentHasChanged(ItemStack arg, ItemStack arg2) {
    return !ItemStack.matches(arg2, arg);
  }

  private void handleHandSwap(Map<EquipmentSlot, ItemStack> map) {
    ItemStack lv = map.get(EquipmentSlot.MAINHAND);
    ItemStack lv2 = map.get(EquipmentSlot.OFFHAND);
    if (lv != null
      && lv2 != null
      && ItemStack.matches(lv, this.getLastHandItem(EquipmentSlot.OFFHAND))
      && ItemStack.matches(lv2, this.getLastHandItem(EquipmentSlot.MAINHAND))) {
      ((ServerLevel)this.level()).getChunkSource().broadcast(this, new ClientboundEntityEventPacket(this, (byte)55));
      map.remove(EquipmentSlot.MAINHAND);
      map.remove(EquipmentSlot.OFFHAND);
      this.setLastHandItem(EquipmentSlot.MAINHAND, lv.copy());
      this.setLastHandItem(EquipmentSlot.OFFHAND, lv2.copy());
    }
  }

  private ItemStack getLastArmorItem(EquipmentSlot arg) {
    return this.lastArmorItemStacks.get(arg.getIndex());
  }

  private void setLastArmorItem(EquipmentSlot arg, ItemStack arg2) {
    this.lastArmorItemStacks.set(arg.getIndex(), arg2);
  }

  private ItemStack getLastHandItem(EquipmentSlot arg) {
    return this.lastHandItemStacks.get(arg.getIndex());
  }

  private void setLastHandItem(EquipmentSlot arg, ItemStack arg2) {
    this.lastHandItemStacks.set(arg.getIndex(), arg2);
  }

  protected float tickHeadTurn(float f, float g) {
    float h = Mth.wrapDegrees(f - this.yBodyRot);
    this.yBodyRot += h * 0.3F;
    float i = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
    float j = this.getMaxHeadRotationRelativeToBody();
    if (Math.abs(i) > j) {
      this.yBodyRot = this.yBodyRot + (i - (float)Mth.sign((double)i) * j);
    }

    boolean bl = i < -90.0F || i >= 90.0F;
    if (bl) {
      g *= -1.0F;
    }

    return g;
  }

  protected float getMaxHeadRotationRelativeToBody() {
    return 50.0F;
  }

  public void aiStep() {
    if (this.noJumpDelay > 0) {
      this.noJumpDelay--;
    }

    if (this.isControlledByLocalInstance()) {
      this.lerpSteps = 0;
      this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
    }

    if (this.lerpSteps > 0) {
      this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
      this.lerpSteps--;
    } else if (!this.isEffectiveAi()) {
      this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
    }

    if (this.lerpHeadSteps > 0) {
      this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
      this.lerpHeadSteps--;
    }

    Vec3 lv = this.getDeltaMovement();
    double d = lv.x;
    double e = lv.y;
    double f = lv.z;
    if (Math.abs(lv.x) < 0.003) {
      d = 0.0;
    }

    if (Math.abs(lv.y) < 0.003) {
      e = 0.0;
    }

    if (Math.abs(lv.z) < 0.003) {
      f = 0.0;
    }

    this.setDeltaMovement(d, e, f);
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

      boolean bl = this.isInWater() && g > 0.0;
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
    AABB lv2 = this.getBoundingBox();
    Vec3 lv3 = new Vec3((double)this.xxa, (double)this.yya, (double)this.zza);
    if (this.hasEffect(EffectType.SLOW_FALLING) || this.hasEffect(EffectType.LEVITATION)) {
      this.resetFallDistance();
    }

    label104: {
      if (this.getControllingPassenger() instanceof Player lv4 && this.isAlive()) {
        this.travelRidden(lv4, lv3);
        break label104;
      }

      this.travel(lv3);
    }

    this.removeFrost();
    this.tryAddFrost();

    if (this.autoSpinAttackTicks > 0) {
      this.autoSpinAttackTicks--;
      this.checkAutoSpinAttack(lv2, this.getBoundingBox());
    }

    this.pushEntities();
  }

  public boolean isSensitiveToWater() {
    return false;
  }

  private void updateFallFlying() {
    boolean bl = this.getSharedFlag(7);
    if (bl && !this.onGround() && !this.isPassenger() && !this.hasEffect(EffectType.LEVITATION)) {
      ItemStack lv = this.getItemBySlot(EquipmentSlot.CHEST);
      if (lv.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(lv)) {
        bl = true;
        int i = this.fallFlyTicks + 1;
      } else {
        bl = false;
      }
    } else {
      bl = false;
    }
  }

  protected void serverAiStep() {
  }

  protected void pushEntities() {
    this.level().getEntities(EntityTypeTest.forClass(Player.class), this.getBoundingBox(), EntitySelector.pushableBy(this)).forEach(this::doPush);
  }

  protected void checkAutoSpinAttack(AABB arg, AABB arg2) {
    AABB lv = arg.minmax(arg2);
    List<Entity> list = this.level().getEntities(this, lv);
    if (!list.isEmpty()) {
      for (Entity lv2 : list) {
        if (lv2 instanceof LivingEntity) {
          this.doAutoAttackOnTouch((LivingEntity)lv2);
          this.autoSpinAttackTicks = 0;
          this.setDeltaMovement(this.getDeltaMovement().scale(-0.2));
          break;
        }
      }
    } else if (this.horizontalCollision) {
      this.autoSpinAttackTicks = 0;
    }
  }

  protected void doPush(Entity arg) {
    arg.push(this);
  }

  protected void doAutoAttackOnTouch(LivingEntity arg) {
  }

  public boolean isAutoSpinAttack() {
    return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
  }

  @Override
  public void stopRiding() {
    Entity lv = this.getVehicle();
    super.stopRiding();
  }

  @Override
  public void rideTick() {
    super.rideTick();
    this.oRun = this.run;
    this.run = 0.0F;
    this.resetFallDistance();
  }

  @Override
  public void lerpTo(double d, double e, double f, float g, float h, int i) {
    this.lerpX = d;
    this.lerpY = e;
    this.lerpZ = f;
    this.lerpYRot = (double)g;
    this.lerpXRot = (double)h;
    this.lerpSteps = i;
  }

  @Override
  public double lerpTargetX() {
    return this.lerpSteps > 0 ? this.lerpX : this.getX();
  }

  @Override
  public double lerpTargetY() {
    return this.lerpSteps > 0 ? this.lerpY : this.getY();
  }

  @Override
  public double lerpTargetZ() {
    return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
  }

  @Override
  public float lerpTargetXRot() {
    return this.lerpSteps > 0 ? (float)this.lerpXRot : this.getXRot();
  }

  @Override
  public float lerpTargetYRot() {
    return this.lerpSteps > 0 ? (float)this.lerpYRot : this.getYRot();
  }

  @Override
  public void lerpHeadTo(float f, int i) {
    this.lerpYHeadRot = (double)f;
    this.lerpHeadSteps = i;
  }

  public void setJumping(boolean bl) {
    this.jumping = bl;
  }

  public boolean hasLineOfSight(Entity arg) {
    if (arg.level() != this.level()) {
      return false;
    } else {
      Vec3 lv = new Vec3(this.getX(), this.getEyeY(), this.getZ());
      Vec3 lv2 = new Vec3(arg.getX(), arg.getEyeY(), arg.getZ());
      return lv2.distanceTo(lv) > 128.0
        ? false
        : this.level().clip(new ClipContext(lv, lv2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
    }
  }

  @Override
  public float getViewYRot(float f) {
    return f == 1.0F ? this.yHeadRot : Mth.lerp(f, this.yHeadRotO, this.yHeadRot);
  }

  public float getAttackAnim(float f) {
    float g = this.attackAnim - this.oAttackAnim;
    if (g < 0.0F) {
      g++;
    }

    return this.oAttackAnim + g * f;
  }

  @Override
  public boolean isPickable() {
    return !this.isRemoved();
  }

  @Override
  public boolean isPushable() {
    return this.isAlive() && !this.isSpectator() && !this.onClimbable();
  }

  @Override
  public float getYHeadRot() {
    return this.yHeadRot;
  }

  @Override
  public void setYHeadRot(float f) {
    this.yHeadRot = f;
  }

  @Override
  public void setYBodyRot(float f) {
    this.yBodyRot = f;
  }

  @Override
  public Vec3 getRelativePortalPosition(Direction.Axis arg, BlockUtil.FoundRectangle arg2) {
    return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(arg, arg2));
  }

  public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 arg) {
    return new Vec3(arg.x, arg.y, 0.0);
  }

  public float getAbsorptionAmount() {
    return this.absorptionAmount;
  }

  public final void setAbsorptionAmount(float f) {
    this.internalSetAbsorptionAmount(Mth.clamp(f, 0.0F, this.getMaxAbsorption()));
  }

  protected void internalSetAbsorptionAmount(float f) {
    this.absorptionAmount = f;
  }

  public void onEnterCombat() {
  }

  public void onLeaveCombat() {
  }

  protected void updateEffectVisibility() {
    this.effectsDirty = true;
  }

  public abstract HumanoidArm getMainArm();

  public boolean isUsingItem() {
    return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
  }

  public InteractionHand getUsedItemHand() {
    return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
  }

  private void updatingUsingItem() {
    if (this.isUsingItem()) {
      if (ItemStack.isSameItem(this.getItemInHand(this.getUsedItemHand()), this.useItem)) {
        this.useItem = this.getItemInHand(this.getUsedItemHand());
        this.updateUsingItem(this.useItem);
      } else {
        this.stopUsingItem();
      }
    }
  }

  protected void updateUsingItem(ItemStack arg) {
    arg.onUseTick(this.level(), this, this.getUseItemRemainingTicks());
    if (this.shouldTriggerItemUseEffects()) {
      this.triggerItemUseEffects(arg, 5);
    }

    --this.useItemRemaining;
  }

  private boolean shouldTriggerItemUseEffects() {
    int i = this.useItem.getUseDuration(this) - this.getUseItemRemainingTicks();
    int j = (int)((float)this.useItem.getUseDuration(this) * 0.21875F);
    boolean bl = i > j;
    return bl && this.getUseItemRemainingTicks() % 4 == 0;
  }

  private void updateSwimAmount() {
    this.swimAmountO = this.swimAmount;
    if (this.isVisuallySwimming()) {
      this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
    } else {
      this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
    }
  }

  protected void setLivingEntityFlag(int i, boolean bl) {
    int j = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
    if (bl) {
      j |= i;
    } else {
      j &= ~i;
    }

    this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)j);
  }

  public void startUsingItem(InteractionHand arg) {
    ItemStack lv = this.getItemInHand(arg);
    if (!lv.isEmpty() && !this.isUsingItem()) {
      this.useItem = lv;
      this.useItemRemaining = lv.getUseDuration(this);
    }
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> arg) {
    super.onSyncedDataUpdated(arg);
    if (SLEEPING_POS_ID.equals(arg)) {
      this.getSleepingPos().ifPresent(this::setPosToBed);
    } else if (DATA_LIVING_ENTITY_FLAGS.equals(arg)) {
      if (this.isUsingItem() && this.useItem.isEmpty()) {
        this.useItem = this.getItemInHand(this.getUsedItemHand());
        if (!this.useItem.isEmpty()) {
          this.useItemRemaining = this.useItem.getUseDuration(this);
        }
      } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
        this.useItem = ItemStack.EMPTY;
        this.useItemRemaining = 0;
      }
    }
  }

  @Override
  public void lookAt(EntityAnchorArgument.Anchor arg, Vec3 arg2) {
    super.lookAt(arg, arg2);
    this.yHeadRotO = this.yHeadRot;
    this.yBodyRot = this.yHeadRot;
    this.yBodyRotO = this.yBodyRot;
  }

  @Override
  public float getPreciseBodyRotation(float f) {
    return Mth.lerp(f, this.yBodyRotO, this.yBodyRot);
  }

  protected void triggerItemUseEffects(ItemStack arg, int i) {
    if (!arg.isEmpty() && this.isUsingItem()) {
      if (arg.getUseAnimation() == UseAnim.DRINK) {
        this.playSound(this.getDrinkingSound(arg), 0.5F, this.level().random.nextFloat() * 0.1F + 0.9F);
      }

      if (arg.getUseAnimation() == UseAnim.EAT) {
        this.spawnItemParticles(arg, i);
        this.playSound(
          this.getEatingSound(arg), 0.5F + 0.5F * (float)this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F
        );
      }
    }
  }

  protected void completeUsingItem() {
    if (this.isUsingItem()) {
      InteractionHand lv = this.getUsedItemHand();
      if (!this.useItem.equals(this.getItemInHand(lv))) {
        this.releaseUsingItem();
      } else {
        if (!this.useItem.isEmpty() && this.isUsingItem()) {
          this.triggerItemUseEffects(this.useItem, 16);
          ItemStack lv2 = this.useItem.finishUsingItem(this.level(), this);
          if (lv2 != this.useItem) {
            this.setItemInHand(lv, lv2);
          }

          this.stopUsingItem();
        }
      }
    }
  }

  public ItemStack getUseItem() {
    return this.useItem;
  }

  public int getUseItemRemainingTicks() {
    return this.useItemRemaining;
  }

  public int getTicksUsingItem() {
    return this.isUsingItem() ? this.useItem.getUseDuration(this) - this.getUseItemRemainingTicks() : 0;
  }

  public void releaseUsingItem() {
    if (!this.useItem.isEmpty()) {
      this.useItem.releaseUsing(this.level(), this, this.getUseItemRemainingTicks());
      if (this.useItem.useOnRelease()) {
        this.updatingUsingItem();
      }
    }

    this.stopUsingItem();
  }

  public void stopUsingItem() {
    this.useItem = ItemStack.EMPTY;
    this.useItemRemaining = 0;
  }

  public boolean isBlocking() {
    if (this.isUsingItem() && !this.useItem.isEmpty()) {
      Item lv = this.useItem.getItem();
      return lv.getUseAnimation(this.useItem) != UseAnim.BLOCK ? false : lv.getUseDuration(this.useItem, this) - this.useItemRemaining >= 5;
    } else {
      return false;
    }
  }

  public boolean isSuppressingSlidingDownLadder() {
    return this.isShiftKeyDown();
  }

  public boolean isFallFlying() {
    return this.getSharedFlag(7);
  }

  @Override
  public boolean isVisuallySwimming() {
    return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
  }

  public int getFallFlyingTicks() {
    return this.fallFlyTicks;
  }

  public boolean isAffectedByPotions() {
    return !this.isDeadOrDying();
  }

  public boolean attackable() {
    return true;
  }

  public void setRecordPlayingNearby(BlockPos arg, boolean bl) {
  }

  public boolean canTakeItem(ItemStack arg) {
    return false;
  }

  @Override
  public final EntityDimensions getDimensions(Pose arg) {
    return arg == Pose.SLEEPING ? SLEEPING_DIMENSIONS : this.getDefaultDimensions(arg).scale(this.getScale());
  }

  protected EntityDimensions getDefaultDimensions(Pose arg) {
    return this.getType().getDimensions().scale(this.getAgeScale());
  }

  public ImmutableList<Pose> getDismountPoses() {
    return ImmutableList.of(Pose.STANDING);
  }

  public AABB getLocalBoundsForPose(Pose arg) {
    EntityDimensions lv = this.getDimensions(arg);
    return new AABB(
      (double)(-lv.width() / 2.0F), 0.0, (double)(-lv.width() / 2.0F), (double)(lv.width() / 2.0F), (double)lv.height(), (double)(lv.width() / 2.0F)
    );
  }

  protected boolean wouldNotSuffocateAtTargetPose(Pose arg) {
    AABB lv = this.getDimensions(arg).makeBoundingBox(this.position());
    return this.level().noBlockCollision(this, lv);
  }

  @Override
  public boolean canUsePortal(boolean bl) {
    return super.canUsePortal(bl) && !this.isSleeping();
  }

  public Optional<BlockPos> getSleepingPos() {
    return this.entityData.get(SLEEPING_POS_ID);
  }

  public void setSleepingPos(BlockPos arg) {
    this.entityData.set(SLEEPING_POS_ID, Optional.of(arg));
  }

  public void clearSleepingPos() {
    this.entityData.set(SLEEPING_POS_ID, Optional.empty());
  }

  public boolean isSleeping() {
    return this.getSleepingPos().isPresent();
  }

  public void startSleeping(BlockPos arg) {
    if (this.isPassenger()) {
      this.stopRiding();
    }

    BlockState lv = this.level().getBlockState(arg);
    if (lv.getBlock() instanceof BedBlock) {
      this.level().setBlock(arg, lv.setValue(BedBlock.OCCUPIED, Boolean.valueOf(true)), 3);
    }

    this.setPose(Pose.SLEEPING);
    this.setPosToBed(arg);
    this.setSleepingPos(arg);
    this.setDeltaMovement(Vec3.ZERO);
    this.hasImpulse = true;
  }

  private void setPosToBed(BlockPos arg) {
    this.setPos((double)arg.getX() + 0.5, (double)arg.getY() + 0.6875, (double)arg.getZ() + 0.5);
  }

  private boolean checkBedExists() {
    return this.getSleepingPos().map(arg -> this.level().getBlockState(arg).getBlock() instanceof BedBlock).orElse(false);
  }

  public void stopSleeping() {
    this.getSleepingPos().filter(this.level()::hasChunkAt).ifPresent(arg -> {
      BlockState lvx = this.level().getBlockState(arg);
      if (lvx.getBlock() instanceof BedBlock) {
        Direction lv2 = lvx.getValue(BedBlock.FACING);
        this.level().setBlock(arg, lvx.setValue(BedBlock.OCCUPIED, Boolean.valueOf(false)), 3);
        Vec3 lv3 = BedBlock.findStandUpPosition(this.getType(), this.level(), arg, lv2, this.getYRot()).orElseGet(() -> {
          BlockPos lvxx = arg.above();
          return new Vec3((double)lvxx.getX() + 0.5, (double)lvxx.getY() + 0.1, (double)lvxx.getZ() + 0.5);
        });
        Vec3 lv4 = Vec3.atBottomCenterOf(arg).subtract(lv3).normalize();
        float f = (float)Mth.wrapDegrees(Mth.atan2(lv4.z, lv4.x) * 180.0F / (float)Math.PI - 90.0);
        this.setPos(lv3.x, lv3.y, lv3.z);
        this.setYRot(f);
        this.setXRot(0.0F);
      }
    });
    Vec3 lv = this.position();
    this.setPose(Pose.STANDING);
    this.setPos(lv.x, lv.y, lv.z);
    this.clearSleepingPos();
  }

  @Nullable
  public Direction getBedOrientation() {
    BlockPos lv = this.getSleepingPos().orElse(null);
    return lv != null ? BedBlock.getBedOrientation(this.level(), lv) : null;
  }

  @Override
  public boolean isInWall() {
    return !this.isSleeping() && super.isInWall();
  }

  public ItemStack getProjectile(ItemStack arg) {
    return ItemStack.EMPTY;
  }

  public final ItemStack eat(Level arg, ItemStack arg2) {
    FoodProperties lv = arg2.get(DataComponents.FOOD);
    return lv != null ? this.eat(arg, arg2, lv) : arg2;
  }

  public ItemStack eat(Level arg, ItemStack arg2, FoodProperties arg3) {
    arg.playSound(
      null,
      this.getX(),
      this.getY(),
      this.getZ(),
      this.getEatingSound(arg2),
      SoundSource.NEUTRAL,
      1.0F,
      1.0F + (arg.random.nextFloat() - arg.random.nextFloat()) * 0.4F
    );
    this.addEatEffect(arg3);
    arg2.consume(1, this);
    this.gameEvent(GameEvent.EAT);
    return arg2;
  }

  private void addEatEffect(FoodProperties arg) {
  }

  private static byte entityEventForEquipmentBreak(EquipmentSlot arg) {
    return switch (arg) {
      case MAINHAND -> 47;
      case OFFHAND -> 48;
      case HEAD -> 49;
      case CHEST -> 50;
      case FEET -> 52;
      case LEGS -> 51;
      case BODY -> 65;
    };
  }

  public void onEquippedItemBroken(Item arg, EquipmentSlot arg2) {
    this.level().broadcastEntityEvent(this, entityEventForEquipmentBreak(arg2));
  }

  public static EquipmentSlot getSlotForHand(InteractionHand arg) {
    return arg == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
  }

  @Override
  public AABB getBoundingBoxForCulling() {
    if (this.getItemBySlot(EquipmentSlot.HEAD).is(Items.DRAGON_HEAD)) {
      float f = 0.5F;
      return this.getBoundingBox().inflate(0.5, 0.5, 0.5);
    } else {
      return super.getBoundingBoxForCulling();
    }
  }

  public EquipmentSlot getEquipmentSlotForItem(ItemStack arg) {
    Equipable lv = Equipable.get(arg);
    if (lv != null) {
      EquipmentSlot lv2 = lv.getEquipmentSlot();
      if (this.canUseSlot(lv2)) {
        return lv2;
      }
    }

    return EquipmentSlot.MAINHAND;
  }

  private static SlotAccess createEquipmentSlotAccess(LivingEntity arg, EquipmentSlot arg2) {
    return arg2 != EquipmentSlot.HEAD && arg2 != EquipmentSlot.MAINHAND && arg2 != EquipmentSlot.OFFHAND
      ? SlotAccess.forEquipmentSlot(arg, arg2, arg3 -> arg3.isEmpty() || arg.getEquipmentSlotForItem(arg3) == arg2)
      : SlotAccess.forEquipmentSlot(arg, arg2);
  }

  @Nullable
  private static EquipmentSlot getEquipmentSlot(int i) {
    if (i == 100 + EquipmentSlot.HEAD.getIndex()) {
      return EquipmentSlot.HEAD;
    } else if (i == 100 + EquipmentSlot.CHEST.getIndex()) {
      return EquipmentSlot.CHEST;
    } else if (i == 100 + EquipmentSlot.LEGS.getIndex()) {
      return EquipmentSlot.LEGS;
    } else if (i == 100 + EquipmentSlot.FEET.getIndex()) {
      return EquipmentSlot.FEET;
    } else if (i == 98) {
      return EquipmentSlot.MAINHAND;
    } else if (i == 99) {
      return EquipmentSlot.OFFHAND;
    } else {
      return i == 105 ? EquipmentSlot.BODY : null;
    }
  }

  @Override
  public SlotAccess getSlot(int i) {
    EquipmentSlot lv = getEquipmentSlot(i);
    return lv != null ? createEquipmentSlotAccess(this, lv) : super.getSlot(i);
  }

  @Override
  public boolean canFreeze() {
    if (this.isSpectator()) {
      return false;
    } else {
      boolean bl = !this.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
        && !this.getItemBySlot(EquipmentSlot.CHEST).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
        && !this.getItemBySlot(EquipmentSlot.LEGS).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
        && !this.getItemBySlot(EquipmentSlot.FEET).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
        && !this.getItemBySlot(EquipmentSlot.BODY).is(ItemTags.FREEZE_IMMUNE_WEARABLES);
      return bl && super.canFreeze();
    }
  }

  @Override
  public float getVisualRotationYInDegrees() {
    return this.yBodyRot;
  }

  @Override
  public void recreateFromPacket(ClientboundAddEntityPacket arg) {
    double d = arg.getX();
    double e = arg.getY();
    double f = arg.getZ();
    float g = arg.getYRot();
    float h = arg.getXRot();
    this.syncPacketPositionCodec(d, e, f);
    this.yBodyRot = arg.getYHeadRot();
    this.yHeadRot = arg.getYHeadRot();
    this.yBodyRotO = this.yBodyRot;
    this.yHeadRotO = this.yHeadRot;
    this.setId(arg.getId());
    this.setUUID(arg.getUUID());
    this.absMoveTo(d, e, f, g, h);
    this.setDeltaMovement(arg.getXa(), arg.getYa(), arg.getZa());
  }

  public boolean canDisableShield() {
    return this.getWeaponItem().getItem() instanceof AxeItem;
  }

  @Override
  public float maxUpStep() {
    float f = (float)this.getAttributeValue(Attributes.STEP_HEIGHT);
    return this.getControllingPassenger() instanceof Player ? Math.max(f, 1.0F) : f;
  }

  @Override
  public Vec3 getPassengerRidingPosition(Entity arg) {
    return this.position().add(this.getPassengerAttachmentPoint(arg, this.getDimensions(this.getPose()), this.getScale() * this.getAgeScale()));
  }

  protected void lerpHeadRotationStep(int i, double d) {
    this.yHeadRot = (float)Mth.rotLerp(1.0 / (double)i, (double)this.yHeadRot, d);
  }

  @Override
  public void igniteForTicks(int i) {
    super.igniteForTicks(Mth.ceil((double)i * this.getAttributeValue(Attributes.BURNING_TIME)));
  }

  public boolean hasInfiniteMaterials() {
    return false;
  }
}
