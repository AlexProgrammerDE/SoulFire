package com.soulfiremc.server.protocol.bot.state.entity.reimpl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.data.EffectType;
import com.soulfiremc.server.data.EntityTypeTags;
import com.soulfiremc.server.data.ResourceKey;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;

public abstract class LivingEntity extends Entity {
  private static final String TAG_ACTIVE_EFFECTS = "active_effects";
  private static final ResourceKey SPEED_MODIFIER_POWDER_SNOW_ID = ResourceKey.key("powder_snow");
  private static final ResourceKey SPRINTING_MODIFIER_ID = ResourceKey.key("sprinting");
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
  private final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = Maps.newHashMap();
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
  public final WalkAnimationState walkAnimation = new WalkAnimationState();
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
    this.AttributeType = new AttributeMap(DefaultAttributeType.getSupplier(arg));
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

  public static AttributeTypeupplier.Builder createLivingAttributeType() {
    return AttributeTypeupplier.builder()
            .add(AttributeType.MAX_HEALTH)
            .add(AttributeType.KNOCKBACK_RESISTANCE)
            .add(AttributeType.MOVEMENT_SPEED)
            .add(AttributeType.ARMOR)
            .add(AttributeType.ARMOR_TOUGHNESS)
            .add(AttributeType.MAX_ABSORPTION)
            .add(AttributeType.STEP_HEIGHT)
            .add(AttributeType.SCALE)
            .add(AttributeType.GRAVITY)
            .add(AttributeType.SAFE_FALL_DISTANCE)
            .add(AttributeType.FALL_DAMAGE_MULTIPLIER)
            .add(AttributeType.JUMP_STRENGTH)
            .add(AttributeType.OXYGEN_BONUS)
            .add(AttributeType.BURNING_TIME)
            .add(AttributeType.EXPLOSION_KNOCKBACK_RESISTANCE)
            .add(AttributeType.WATER_MOVEMENT_EFFICIENCY)
            .add(AttributeType.MOVEMENT_EFFICIENCY)
            .add(AttributeType.ATTACK_KNOCKBACK);
  }

  @Override
  protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    if (!this.isInWater()) {
      this.updateInWaterStateAndDoWaterCurrentPushing();
    }

    super.checkFallDamage(y, onGround, state, pos);
    if (onGround) {
      this.lastClimbablePos = Optional.empty();
    }
  }

  public final boolean canBreatheUnderwater() {
    return this.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
  }

  public float getSwimAmount(float partialTicks) {
    return Mth.lerp(partialTicks, this.swimAmountO, this.swimAmount);
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
    this.level().getProfiler().push("livingEntityBaseTick");
    if (this.fireImmune() || this.level().isClientSide) {
      this.clearFire();
    }

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
    this.level().getProfiler().pop();
  }

  @Nullable
  private static EquipmentSlot getEquipmentSlot(int index) {
    if (index == 100 + EquipmentSlot.HEAD.getIndex()) {
      return EquipmentSlot.HEAD;
    } else if (index == 100 + EquipmentSlot.BODY.getIndex()) {
      return EquipmentSlot.BODY;
    } else if (index == 100 + EquipmentSlot.LEGS.getIndex()) {
      return EquipmentSlot.LEGS;
    } else if (index == 100 + EquipmentSlot.FEET.getIndex()) {
      return EquipmentSlot.FEET;
    } else if (index == 98) {
      return EquipmentSlot.MAINHAND;
    } else if (index == 99) {
      return EquipmentSlot.OFFHAND;
    } else {
      return index == 105 ? EquipmentSlot.BODY : null;
    }
  }

  @Override
  protected float getBlockSpeedFactor() {
    return Mth.lerp((float) this.getAttributeValue(AttributeType.MOVEMENT_EFFICIENCY), super.getBlockSpeedFactor(), 1.0F);
  }

  protected void removeFrost() {
    AttributeInstance lv = this.getAttribute(AttributeType.MOVEMENT_SPEED);
    if (lv != null) {
      if (lv.getModifier(SPEED_MODIFIER_POWDER_SNOW_ID) != null) {
        lv.removeModifier(SPEED_MODIFIER_POWDER_SNOW_ID);
      }
    }
  }

  public boolean isBaby() {
    return false;
  }

  public float getAgeScale() {
    return this.isBaby() ? 0.5F : 1.0F;
  }

  protected void tryAddFrost() {
    if (!this.getBlockStateOnLegacy().isAir()) {
      int i = this.getTicksFrozen();
      if (i > 0) {
        AttributeInstance lv = this.getAttribute(AttributeType.MOVEMENT_SPEED);
        if (lv == null) {
          return;
        }

        float f = -0.05F * this.getPercentFrozen();
        lv.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_ID, (double)f, AttributeModifier.Operation.ADD_VALUE));
      }
    }
  }

  protected float sanitizeScale(float scale) {
    return scale;
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

  public float getScale() {
    AttributeMap lv = this.getAttributeType();
    return lv == null ? 1.0F : this.sanitizeScale((float) lv.getValue(AttributeType.SCALE));
  }

  protected int increaseAirSupply(int currentAir) {
    return Math.min(currentAir + 4, this.getMaxAirSupply());
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

  public void setLastHurtByPlayer(@Nullable Player player) {
    this.lastHurtByPlayer = player;
    this.lastHurtByPlayerTime = this.tickCount;
  }

  public void setLastHurtByMob(@Nullable LivingEntity livingEntity) {
    this.lastHurtByMob = livingEntity;
    this.lastHurtByMobTimestamp = this.tickCount;
  }

  @Nullable
  public LivingEntity getLastHurtMob() {
    return this.lastHurtMob;
  }

  public int getLastHurtMobTimestamp() {
    return this.lastHurtMobTimestamp;
  }

  public void setLastHurtMob(Entity entity) {
    if (entity instanceof LivingEntity) {
      this.lastHurtMob = (LivingEntity)entity;
    } else {
      this.lastHurtMob = null;
    }

    this.lastHurtMobTimestamp = this.tickCount;
  }

  public int getNoActionTime() {
    return this.noActionTime;
  }

  public void setNoActionTime(int idleTime) {
    this.noActionTime = idleTime;
  }

  public boolean shouldDiscardFriction() {
    return this.discardFriction;
  }

  public void setDiscardFriction(boolean discardFriction) {
    this.discardFriction = discardFriction;
  }

  protected boolean doesEmitEquipEvent(EquipmentSlot slot) {
    return true;
  }

  public void onEquipItem(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem) {
  }

  protected int decreaseAirSupply(int currentAir) {
    AttributeInstance lv = this.getAttribute(AttributeType.OXYGEN_BONUS);
    double d;
    if (lv != null) {
      d = lv.getValue();
    } else {
      d = 0.0;
    }

    return d > 0.0 && this.random.nextDouble() >= 1.0 / (d + 1.0) ? currentAir : currentAir - 1;
  }

  @Override
  public void remove(Entity.RemovalReason reason) {
    if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
      this.triggerOnDeathEffectType(reason);
    }

    super.remove(reason);
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
  }

  public boolean canBeSeenAsEnemy() {
    return !this.isInvulnerable() && this.canBeSeenByAnyone();
  }

  public boolean canBeSeenByAnyone() {
    return !this.isSpectator() && this.isAlive();
  }

  public boolean removeAllEffects() {
    if (this.level().isClientSide) {
      return false;
    } else {
      Iterator<MobEffectInstance> iterator = this.activeEffects.values().iterator();

      boolean bl;
      for (bl = false; iterator.hasNext(); bl = true) {
        this.onEffectRemoved(iterator.next());
        iterator.remove();
      }

      return bl;
    }
  }

  public Collection<MobEffectInstance> getActiveEffects() {
    return this.activeEffects.values();
  }

  public Map<Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap() {
    return this.activeEffects;
  }

  protected void triggerOnDeathEffectType(Entity.RemovalReason removalReason) {
    for (MobEffectInstance lv : this.getActiveEffects()) {
      lv.onMobRemoved(this, removalReason);
    }

    this.activeEffects.clear();
  }

  @Nullable
  public MobEffectInstance getEffect(Holder<MobEffect> effect) {
    return this.activeEffects.get(effect);
  }

  public final boolean addEffect(MobEffectInstance effectInstance) {
    return this.addEffect(effectInstance, null);
  }

  public boolean addEffect(MobEffectInstance effectInstance, @Nullable Entity entity) {
    if (!this.canBeAffected(effectInstance)) {
      return false;
    } else {
      MobEffectInstance lv = this.activeEffects.get(effectInstance.getEffect());
      boolean bl = false;
      if (lv == null) {
        this.activeEffects.put(effectInstance.getEffect(), effectInstance);
        this.onEffectAdded(effectInstance, entity);
        bl = true;
        effectInstance.onEffectAdded(this);
      } else if (lv.update(effectInstance)) {
        this.onEffectUpdated(lv, true, entity);
        bl = true;
      }

      effectInstance.onEffectStarted(this);
      return bl;
    }
  }

  public boolean hasEffect(EffectType effect) {
    return this.activeEffects.containsKey(effect);
  }

  public void forceAddEffect(MobEffectInstance instance, @Nullable Entity entity) {
    if (this.canBeAffected(instance)) {
      MobEffectInstance lv = this.activeEffects.put(instance.getEffect(), instance);
      if (lv == null) {
        this.onEffectAdded(instance, entity);
      } else {
        instance.copyBlendState(lv);
        this.onEffectUpdated(instance, true, entity);
      }
    }
  }

  public boolean isInvertedHealAndHarm() {
    return this.getType().is(EntityTypeTags.INVERTED_HEALING_AND_HARM);
  }

  @Nullable
  public MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> effect) {
    return this.activeEffects.remove(effect);
  }

  public boolean removeEffect(Holder<MobEffect> effect) {
    MobEffectInstance lv = this.removeEffectNoUpdate(effect);
    if (lv != null) {
      this.onEffectRemoved(lv);
      return true;
    } else {
      return false;
    }
  }

  protected void onEffectAdded(MobEffectInstance effectInstance, @Nullable Entity entity) {
    this.effectsDirty = true;
  }

  protected void onEffectUpdated(MobEffectInstance effectInstance, boolean forced, @Nullable Entity entity) {
    this.effectsDirty = true;
  }

  protected void onEffectRemoved(MobEffectInstance effectInstance) {
    this.effectsDirty = true;
  }

  public boolean canBeAffected(MobEffectInstance effectInstance) {
    if (this.getType().is(EntityTypeTags.IMMUNE_TO_INFESTED)) {
      return !effectInstance.is(EffectType.INFESTED);
    } else if (this.getType().is(EntityTypeTags.IMMUNE_TO_OOZING)) {
      return !effectInstance.is(EffectType.OOZING);
    } else {
      return !this.getType().is(EntityTypeTags.IGNORES_POISON_AND_REGEN) || !effectInstance.is(EffectType.REGENERATION) && !effectInstance.is(EffectType.POISON);
    }
  }

  private void refreshDirtyAttributeType() {
    Set<AttributeInstance> set = this.getAttributeType().getAttributeTypeToUpdate();

    for (AttributeInstance lv : set) {
      this.onAttributeUpdated(lv.getAttribute());
    }

    set.clear();
  }

  public void heal(float healAmount) {
    float g = this.getHealth();
    if (g > 0.0F) {
      this.setHealth(g + healAmount);
    }
  }

  public float getHealth() {
    return this.entityData.get(DATA_HEALTH_ID);
  }

  public void setHealth(float health) {
    this.entityData.set(DATA_HEALTH_ID, Mth.clamp(health, 0.0F, this.getMaxHealth()));
  }

  public boolean isDeadOrDying() {
    return this.getHealth() <= 0.0F;
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    return false;
  }

  protected void blockUsingShield(LivingEntity attacker) {
    attacker.blockedByShield(this);
  }

  protected void blockedByShield(LivingEntity defender) {
    defender.knockback(0.5, defender.getX() - this.getX(), defender.getZ() - this.getZ());
  }

  @Nullable
  public DamageSource getLastDamageSource() {
    if (this.level().getGameTime() - this.lastDamageStamp > 40L) {
      this.lastDamageSource = null;
    }

    return this.lastDamageSource;
  }

  protected void playHurtSound(DamageSource source) {
    this.makeSound(this.getHurtSound(source));
  }

  public void makeSound(@Nullable SoundEvent sound) {
    if (sound != null) {
      this.playSound(sound, this.getSoundVolume(), this.getVoicePitch());
    }
  }

  public boolean isDamageSourceBlocked(DamageSource damageSource) {
    Entity lv = damageSource.getDirectEntity();
    boolean bl = lv instanceof AbstractArrow lv2 && lv2.getPierceLevel() > 0;

      if (!damageSource.is(DamageTypeTags.BYPASSES_SHIELD) && this.isBlocking() && !bl) {
      Vec3 lv3 = damageSource.getSourcePosition();
      if (lv3 != null) {
        Vec3 lv4 = this.calculateViewVector(0.0F, this.getYHeadRot());
        Vec3 lv5 = lv3.vectorTo(this.position());
        lv5 = new Vec3(lv5.x, 0.0, lv5.z).normalize();
        return lv5.dot(lv4) < 0.0;
      }
    }

    return false;
  }

  private void breakItem(ItemStack stack) {
    if (!stack.isEmpty()) {
      if (!this.isSilent()) {
        this.level()
          .playLocalSound(
            this.getX(),
            this.getY(),
            this.getZ(),
            stack.getBreakingSound(),
            this.getSoundSource(),
            0.8F,
            0.8F + this.level().random.nextFloat() * 0.4F,
            false
          );
      }

      this.spawnItemParticles(stack, 5);
    }
  }

  public void die(DamageSource damageSource) {
    if (!this.isRemoved() && !this.dead) {
      Entity lv = damageSource.getEntity();
      LivingEntity lv2 = this.getKillCredit();
      if (this.isSleeping()) {
        this.stopSleeping();
      }

      this.dead = true;
      this.getCombatTracker().recheckStatus();

      this.setPose(Pose.DYING);
    }
  }

  protected void dropEquipment() {
  }

  private void onAttributeUpdated(AttributeType attribute) {
    if (attribute.is(AttributeType.MAX_HEALTH)) {
      float f = this.getMaxHealth();
      if (this.getHealth() > f) {
        this.setHealth(f);
      }
    } else if (attribute.is(AttributeType.MAX_ABSORPTION)) {
      float f = this.getMaxAbsorption();
      if (this.getAbsorptionAmount() > f) {
        this.setAbsorptionAmount(f);
      }
    }
  }

  protected float getKnockback(Entity attacker, DamageSource damageSource) {
    return (float) this.getAttributeValue(AttributeType.ATTACK_KNOCKBACK);
  }

  public void indicateDamage(double xDistance, double zDistance) {
  }

  @Nullable
  protected SoundEvent getHurtSound(DamageSource damageSource) {
    return SoundEvents.GENERIC_HURT;
  }

  @Nullable
  protected SoundEvent getDeathSound() {
    return SoundEvents.GENERIC_DEATH;
  }

  private SoundEvent getFallDamageSound(int height) {
    return height > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
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

  private boolean trapdoorUsableAsLadder(BlockPos pos, BlockState state) {
    if (!state.getValue(TrapDoorBlock.OPEN)) {
      return false;
    } else {
      BlockState lv = this.level().getBlockState(pos.below());
      return lv.is(Blocks.LADDER) && lv.getValue(LadderBlock.FACING) == state.getValue(TrapDoorBlock.FACING);
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

  protected final int getComfortableFallDistance(float health) {
    return Mth.floor(health + 3.0F);
  }

  @Override
  public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
    boolean bl = super.causeFallDamage(fallDistance, multiplier, source);
    int i = this.calculateFallDamage(fallDistance, multiplier);
    if (i > 0) {
      this.hurt(source, (float)i);
      return true;
    } else {
      return bl;
    }
  }

  public void knockback(double strength, double x, double z) {
    strength *= 1.0 - this.getAttributeValue(AttributeType.KNOCKBACK_RESISTANCE);
    if (!(strength <= 0.0)) {
      this.hasImpulse = true;
      Vec3 lv = this.getDeltaMovement();

      while (x * x + z * z < 1.0E-5F) {
        x = (Math.random() - Math.random()) * 0.01;
        z = (Math.random() - Math.random()) * 0.01;
      }

      Vec3 lv2 = new Vec3(x, 0.0, z).normalize().scale(strength);
      this.setDeltaMovement(lv.x / 2.0 - lv2.x, this.onGround() ? Math.min(0.4, lv.y / 2.0 + strength) : lv.y, lv.z / 2.0 - lv2.z);
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
      }
    }
  }

  @Override
  public void animateHurt(float yaw) {
    this.hurtDuration = 10;
    this.hurtTime = this.hurtDuration;
  }

  protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
    if (this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
      return 0;
    } else {
      float h = (float) this.getAttributeValue(AttributeType.SAFE_FALL_DISTANCE);
      float i = fallDistance - h;
      return Mth.ceil((double) (i * damageMultiplier) * this.getAttributeValue(AttributeType.FALL_DAMAGE_MULTIPLIER));
    }
  }

  protected void hurtArmor(DamageSource damageSource, float damageAmount) {
  }

  protected void hurtHelmet(DamageSource damageSource, float damageAmount) {
  }

  protected void hurtCurrentlyUsedShield(float damageAmount) {
  }

  @Nullable
  public LivingEntity getKillCredit() {
    if (this.lastHurtByPlayer != null) {
      return this.lastHurtByPlayer;
    } else {
      return this.lastHurtByMob != null ? this.lastHurtByMob : null;
    }
  }

  public int getArmorValue() {
    return Mth.floor(this.getAttributeValue(AttributeType.ARMOR));
  }

  public final float getMaxHealth() {
    return (float) this.getAttributeValue(AttributeType.MAX_HEALTH);
  }

  public final int getArrowCount() {
    return this.entityData.get(DATA_ARROW_COUNT_ID);
  }

  public final void setArrowCount(int count) {
    this.entityData.set(DATA_ARROW_COUNT_ID, count);
  }

  public final int getStingerCount() {
    return this.entityData.get(DATA_STINGER_COUNT_ID);
  }

  public final void setStingerCount(int stingerCount) {
    this.entityData.set(DATA_STINGER_COUNT_ID, stingerCount);
  }

  public final float getMaxAbsorption() {
    return (float) this.getAttributeValue(AttributeType.MAX_ABSORPTION);
  }

  public void swing(InteractionHand hand) {
    this.swing(hand, false);
  }

  public void swing(InteractionHand hand, boolean updateSelf) {
    if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
      this.swingTime = -1;
      this.swinging = true;
      this.swingingArm = hand;
    }
  }

  @Override
  public void handleDamageEvent(DamageSource damageSource) {
    this.walkAnimation.setSpeed(1.5F);
    this.invulnerableTime = 20;
    this.hurtDuration = 10;
    this.hurtTime = this.hurtDuration;

    this.hurt(this.damageSources().generic(), 0.0F);
    this.lastDamageSource = damageSource;
    this.lastDamageStamp = this.level().getGameTime();
  }

  private int getCurrentSwingDuration() {
    if (MobEffectUtil.hasDigSpeed(this)) {
      return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this));
    } else {
      return this.hasEffect(EffectType.DIG_SLOWDOWN) ? 6 + (1 + this.getEffect(EffectType.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
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

  @Override
  public void handleEntityEvent(byte id) {
    switch (id) {
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
        this.breakItem(this.getItemBySlot(EquipmentSlot.BODY));
        break;
      case 51:
        this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
        break;
      case 52:
        this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
        break;
      case 54:
        break;
      case 55:
        this.swapHandItems();
        break;
      case 60:
        break;
      case 65:
        this.breakItem(this.getItemBySlot(EquipmentSlot.BODY));
        break;
      default:
        super.handleEntityEvent(id);
    }
  }

  @Nullable
  public AttributeInstance getAttribute(AttributeType attribute) {
    return this.getAttributeType().getInstance(attribute);
  }

  public double getAttributeValue(AttributeType attribute) {
    return this.getAttributeType().getValue(attribute);
  }

  public double getAttributeBaseValue(AttributeType attribute) {
    return this.getAttributeType().getBaseValue(attribute);
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

  public boolean isHolding(Item item) {
    return this.isHolding(arg2 -> arg2.is(item));
  }

  public boolean isHolding(Predicate<ItemStack> predicate) {
    return predicate.test(this.getMainHandItem()) || predicate.test(this.getOffhandItem());
  }

  public ItemStack getItemInHand(InteractionHand hand) {
    if (hand == InteractionHand.MAIN_HAND) {
      return this.getItemBySlot(EquipmentSlot.MAINHAND);
    } else if (hand == InteractionHand.OFF_HAND) {
      return this.getItemBySlot(EquipmentSlot.OFFHAND);
    } else {
      throw new IllegalArgumentException("Invalid hand " + hand);
    }
  }

  public void setItemInHand(InteractionHand hand, ItemStack stack) {
    if (hand == InteractionHand.MAIN_HAND) {
      this.setItemSlot(EquipmentSlot.MAINHAND, stack);
    } else {
      if (hand != InteractionHand.OFF_HAND) {
        throw new IllegalArgumentException("Invalid hand " + hand);
      }

      this.setItemSlot(EquipmentSlot.OFFHAND, stack);
    }
  }

  public boolean hasItemInSlot(EquipmentSlot slot) {
    return !this.getItemBySlot(slot).isEmpty();
  }

  public boolean canUseSlot(EquipmentSlot slot) {
    return false;
  }

  public abstract Iterable<ItemStack> getArmorSlots();

  public abstract ItemStack getItemBySlot(EquipmentSlot slot);

  public abstract void setItemSlot(EquipmentSlot slot, ItemStack stack);

  public Iterable<ItemStack> getHandSlots() {
    return List.of();
  }

  public Iterable<ItemStack> getArmorAndBodyArmorSlots() {
    return this.getArmorSlots();
  }

  public Iterable<ItemStack> getAllSlots() {
    return Iterables.concat(this.getHandSlots(), this.getArmorAndBodyArmorSlots());
  }

  protected void verifyEquippedItem(ItemStack stack) {
    stack.getItem().verifyComponentsAfterLoad(stack);
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

  public AttributeMap getAttributeType() {
    return this.AttributeType;
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
  public void push(Entity entity) {
    if (!this.isSleeping()) {
      super.push(entity);
    }
  }

  private void dismountVehicle(Entity vehicle) {
    Vec3 lv;
    if (this.isRemoved()) {
      lv = this.position();
    } else if (!vehicle.isRemoved() && !this.level().getBlockState(vehicle.blockPosition()).is(BlockTags.PORTALS)) {
      lv = vehicle.getDismountLocationForPassenger(this);
    } else {
      double d = Math.max(this.getY(), vehicle.getY());
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

  @Override
  public void setSprinting(boolean sprinting) {
    super.setSprinting(sprinting);
    AttributeInstance lv = this.getAttribute(AttributeType.MOVEMENT_SPEED);
    lv.removeModifier(SPEED_MODIFIER_SPRINTING.id());
    if (sprinting) {
      lv.addTransientModifier(SPEED_MODIFIER_SPRINTING);
    }
  }

  protected float getJumpPower(float multiplier) {
    return (float) this.getAttributeValue(AttributeType.JUMP_STRENGTH) * multiplier * this.getBlockJumpFactor() + this.getJumpBoostPower();
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

  protected void jumpInLiquid(TagKey<Fluid> fluidTag) {
    this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04F, 0.0));
  }

  protected float getWaterSlowDown() {
    return 0.8F;
  }

  public boolean canStandOnFluid(FluidState fluidState) {
    return false;
  }

  public float getJumpBoostPower() {
    return this.hasEffect(EffectType.JUMP) ? 0.1F * ((float) this.getEffect(EffectType.JUMP).getAmplifier() + 1.0F) : 0.0F;
  }

  @Override
  protected double getDefaultGravity() {
    return this.getAttributeValue(AttributeType.GRAVITY);
  }

  public void travel(Vec3 travelVector) {
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
        float h = (float) this.getAttributeValue(AttributeType.WATER_MOVEMENT_EFFICIENCY);
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

        this.moveRelative(g, travelVector);
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
        this.moveRelative(0.02F, travelVector);
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
        double l = Math.cos(fx);
        l = l * l * Math.min(1.0, k / 0.4);
        lv5 = this.getDeltaMovement().add(0.0, d * (-1.0 + l * 0.75), 0.0);
        if (lv5.y < 0.0 && i > 0.0) {
          double m = lv5.y * -0.1 * l;
          lv5 = lv5.add(lv6.x * m / i, m, lv6.z * m / i);
        }

        if (fx < 0.0F && i > 0.0) {
          double m = j * -Mth.sin(fx) * 0.04;
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
        Vec3 lv8 = this.handleRelativeFrictionAndCalculateMovement(travelVector, p);
        double q = lv8.y;
        if (this.hasEffect(EffectType.LEVITATION)) {
          q += (0.05 * (double) (this.getEffect(EffectType.LEVITATION).getAmplifier() + 1) - lv8.y) * 0.2;
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

  protected void tickRidden(Player player, Vec3 travelVector) {
  }

  protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
    return travelVector;
  }

  protected float getRiddenSpeed(Player player) {
    return this.getSpeed();
  }

  public void calculateEntityAnimation(boolean includeHeight) {
    float f = (float)Mth.length(this.getX() - this.xo, includeHeight ? this.getY() - this.yo : 0.0, this.getZ() - this.zo);
    this.updateWalkAnimation(f);
  }

  protected void updateWalkAnimation(float partialTick) {
    float g = Math.min(partialTick * 4.0F, 1.0F);
    this.walkAnimation.update(g, 0.4F);
  }

  public Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 deltaMovement, float friction) {
    this.moveRelative(this.getFrictionInfluencedSpeed(friction), deltaMovement);
    this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
    this.move(MoverType.SELF, this.getDeltaMovement());
    Vec3 lv = this.getDeltaMovement();
    if ((this.horizontalCollision || this.jumping)
      && (this.onClimbable() || this.getInBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
      lv = new Vec3(lv.x, 0.2, lv.z);
    }

    return lv;
  }

  public Vec3 getFluidFallingAdjustedMovement(double gravity, boolean isFalling, Vec3 deltaMovement) {
    if (gravity != 0.0 && !this.isSprinting()) {
      double e;
      if (isFalling && Math.abs(deltaMovement.y - 0.005) >= 0.003 && Math.abs(deltaMovement.y - gravity / 16.0) < 0.003) {
        e = -0.003;
      } else {
        e = deltaMovement.y - gravity / 16.0;
      }

      return new Vec3(deltaMovement.x, e, deltaMovement.z);
    } else {
      return deltaMovement;
    }
  }

  private Vec3 handleOnClimbable(Vec3 deltaMovement) {
    if (this.onClimbable()) {
      this.resetFallDistance();
      float f = 0.15F;
      double d = Mth.clamp(deltaMovement.x, -0.15F, 0.15F);
      double e = Mth.clamp(deltaMovement.z, -0.15F, 0.15F);
      double g = Math.max(deltaMovement.y, -0.15F);
      if (g < 0.0 && !this.getInBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
        g = 0.0;
      }

      deltaMovement = new Vec3(d, g, e);
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

  public boolean doHurtTarget(Entity target) {
    this.setLastHurtMob(target);
    return false;
  }

  private void travelRidden(Player player, Vec3 travelVector) {
    Vec3 lv = this.getRiddenInput(player, travelVector);
    this.tickRidden(player, lv);
    if (this.isControlledByLocalInstance()) {
      this.setSpeed(this.getRiddenSpeed(player));
      this.travel(lv);
    } else {
      this.calculateEntityAnimation(false);
      this.setDeltaMovement(Vec3.ZERO);
      this.checkInsideBlocks();
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
      h = (float)Math.sqrt(f) * 3.0F;
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
    this.level().getProfiler().push("headTurn");
    h = this.tickHeadTurn(g, h);
    this.level().getProfiler().pop();
    this.level().getProfiler().push("rangeChecks");

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

    this.level().getProfiler().pop();
    this.animStep += h;
    if (this.isFallFlying()) {
      this.fallFlyTicks++;
    } else {
      this.fallFlyTicks = 0;
    }

    if (this.isSleeping()) {
      this.setXRot(0.0F);
    }

    this.refreshDirtyAttributeType();
    float l = this.getScale();
    if (l != this.appliedScale) {
      this.appliedScale = l;
      this.refreshDimensions();
    }
  }

  public boolean equipmentHasChanged(ItemStack oldItem, ItemStack newItem) {
    return !ItemStack.matches(newItem, oldItem);
  }

  private void handleHandSwap(Map<EquipmentSlot, ItemStack> hands) {
    ItemStack lv = hands.get(EquipmentSlot.MAINHAND);
    ItemStack lv2 = hands.get(EquipmentSlot.OFFHAND);
    if (lv != null
      && lv2 != null
      && ItemStack.matches(lv, this.getLastHandItem(EquipmentSlot.OFFHAND))
      && ItemStack.matches(lv2, this.getLastHandItem(EquipmentSlot.MAINHAND))) {
      hands.remove(EquipmentSlot.MAINHAND);
      hands.remove(EquipmentSlot.OFFHAND);
      this.setLastHandItem(EquipmentSlot.MAINHAND, lv.copy());
      this.setLastHandItem(EquipmentSlot.OFFHAND, lv2.copy());
    }
  }

  private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> equipments) {
    List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayListWithCapacity(equipments.size());
    equipments.forEach((arg, arg2) -> {
      ItemStack lv = arg2.copy();
      list.add(Pair.of(arg, lv));
      switch (arg.getType()) {
        case HAND:
          this.setLastHandItem(arg, lv);
          break;
        case HUMANOID_ARMOR:
          this.setLastArmorItem(arg, lv);
          break;
        case ANIMAL_ARMOR:
          this.lastBodyItemStack = lv;
      }
    });
  }

  private ItemStack getLastArmorItem(EquipmentSlot slot) {
    return this.lastArmorItemStacks.get(slot.getIndex());
  }

  private void setLastArmorItem(EquipmentSlot slot, ItemStack stack) {
    this.lastArmorItemStacks.set(slot.getIndex(), stack);
  }

  private ItemStack getLastHandItem(EquipmentSlot slot) {
    return this.lastHandItemStacks.get(slot.getIndex());
  }

  private void setLastHandItem(EquipmentSlot slot, ItemStack stack) {
    this.lastHandItemStacks.set(slot.getIndex(), stack);
  }

  protected float tickHeadTurn(float yRot, float animStep) {
    float h = Mth.wrapDegrees(yRot - this.yBodyRot);
    this.yBodyRot += h * 0.3F;
    float i = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
    float j = this.getMaxHeadRotationRelativeToBody();
    if (Math.abs(i) > j) {
      this.yBodyRot = this.yBodyRot + (i - (float)Mth.sign((double)i) * j);
    }

    boolean bl = i < -90.0F || i >= 90.0F;
    if (bl) {
      animStep *= -1.0F;
    }

    return animStep;
  }

  protected float getMaxHeadRotationRelativeToBody() {
    return 50.0F;
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
        AttributeMap lv4 = this.getAttributeType();
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
            AttributeInstance lv = this.AttributeType.getInstance(arg3);
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

  public boolean isSensitiveToWater() {
    return false;
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
    this.level().getProfiler().push("ai");
    if (this.isImmobile()) {
      this.jumping = false;
      this.xxa = 0.0F;
      this.zza = 0.0F;
    } else if (this.isEffectiveAi()) {
      this.level().getProfiler().push("newAi");
      this.serverAiStep();
      this.level().getProfiler().pop();
    }

    this.level().getProfiler().pop();
    this.level().getProfiler().push("jump");
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

    this.level().getProfiler().pop();
    this.level().getProfiler().push("travel");
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

  protected void serverAiStep() {
  }

  protected void pushEntities() {
    if (this.level().isClientSide()) {
      this.level().getEntities(EntityTypeTest.forClass(Player.class), this.getBoundingBox(), EntitySelector.pushableBy(this)).forEach(this::doPush);
    } else {
      List<Entity> list = this.level().getEntities(this, this.getBoundingBox(), EntitySelector.pushableBy(this));
      if (!list.isEmpty()) {
        int i = this.level().getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
        if (i > 0 && list.size() > i - 1 && this.random.nextInt(4) == 0) {
          int j = 0;

          for (Entity lv : list) {
            if (!lv.isPassenger()) {
              j++;
            }
          }

          if (j > i - 1) {
            this.hurt(this.damageSources().cramming(), 6.0F);
          }
        }

        for (Entity lv2 : list) {
          this.doPush(lv2);
        }
      }
    }
  }

  protected void checkAutoSpinAttack(AABB boundingBoxBeforeSpin, AABB boundingBoxAfterSpin) {
    AABB lv = boundingBoxBeforeSpin.minmax(boundingBoxAfterSpin);
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

  protected void doPush(Entity entity) {
    entity.push(this);
  }

  protected void doAutoAttackOnTouch(LivingEntity target) {
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
  public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
    this.lerpX = x;
    this.lerpY = y;
    this.lerpZ = z;
    this.lerpYRot = yRot;
    this.lerpXRot = xRot;
    this.lerpSteps = steps;
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
  public void lerpHeadTo(float yaw, int pitch) {
    this.lerpYHeadRot = yaw;
    this.lerpHeadSteps = pitch;
  }

  public void setJumping(boolean jumping) {
    this.jumping = jumping;
  }

  @Override
  public float getViewYRot(float partialTick) {
    return partialTick == 1.0F ? this.yHeadRot : Mth.lerp(partialTick, this.yHeadRotO, this.yHeadRot);
  }

  public float getAttackAnim(float partialTick) {
    float g = this.attackAnim - this.oAttackAnim;
    if (g < 0.0F) {
      g++;
    }

    return this.oAttackAnim + g * partialTick;
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
  public void setYHeadRot(float yHeadRot) {
    this.yHeadRot = yHeadRot;
  }

  @Override
  public void setYBodyRot(float yBodyRot) {
    this.yBodyRot = yBodyRot;
  }

  @Override
  public Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle portal) {
    return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, portal));
  }

  public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 relativePortalPosition) {
    return new Vec3(relativePortalPosition.x, relativePortalPosition.y, 0.0);
  }

  public float getAbsorptionAmount() {
    return this.absorptionAmount;
  }

  public final void setAbsorptionAmount(float absorptionAmount) {
    this.internalSetAbsorptionAmount(Mth.clamp(absorptionAmount, 0.0F, this.getMaxAbsorption()));
  }

  protected void internalSetAbsorptionAmount(float absorptionAmount) {
    this.absorptionAmount = absorptionAmount;
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

  protected void updateUsingItem(ItemStack usingItem) {
    usingItem.onUseTick(this.level(), this, this.getUseItemRemainingTicks());
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

  protected void setLivingEntityFlag(int key, boolean value) {
    int j = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
    if (value) {
      j |= key;
    } else {
      j &= ~key;
    }

    this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)j);
  }

  public void startUsingItem(InteractionHand hand) {
    ItemStack lv = this.getItemInHand(hand);
    if (!lv.isEmpty() && !this.isUsingItem()) {
      this.useItem = lv;
      this.useItemRemaining = lv.getUseDuration(this);
    }
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> dataAccessor) {
    super.onSyncedDataUpdated(dataAccessor);
    if (SLEEPING_POS_ID.equals(dataAccessor)) {
      if (this.level().isClientSide) {
        this.getSleepingPos().ifPresent(this::setPosToBed);
      }
    } else if (DATA_LIVING_ENTITY_FLAGS.equals(dataAccessor) && this.level().isClientSide) {
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
  public void lookAt(EntityAnchorArgument.Anchor anchor, Vec3 target) {
    super.lookAt(anchor, target);
    this.yHeadRotO = this.yHeadRot;
    this.yBodyRot = this.yHeadRot;
    this.yBodyRotO = this.yBodyRot;
  }

  @Override
  public float getPreciseBodyRotation(float partialTick) {
    return Mth.lerp(partialTick, this.yBodyRotO, this.yBodyRot);
  }

  protected void completeUsingItem() {
    if (this.isUsingItem()) {
      InteractionHand lv = this.getUsedItemHand();
      if (!this.useItem.equals(this.getItemInHand(lv))) {
        this.releaseUsingItem();
      } else {
        if (!this.useItem.isEmpty() && this.isUsingItem()) {
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
      return lv.getUseAnimation(this.useItem) == UseAnim.BLOCK && lv.getUseDuration(this.useItem, this) - this.useItemRemaining >= 5;
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

  public boolean randomTeleport(double x, double y, double z, boolean broadcastTeleport) {
    double g = this.getX();
    double h = this.getY();
    double i = this.getZ();
    double j = y;
    boolean bl2 = false;
    BlockPos lv = BlockPos.containing(x, y, z);
    Level lv2 = this.level();
    if (lv2.hasChunkAt(lv)) {
      boolean bl3 = false;

      while (!bl3 && lv.getY() > lv2.getMinBuildHeight()) {
        BlockPos lv3 = lv.below();
        BlockState lv4 = lv2.getBlockState(lv3);
        if (lv4.blocksMotion()) {
          bl3 = true;
        } else {
          j--;
          lv = lv3;
        }
      }

      if (bl3) {
        this.teleportTo(x, j, z);
        if (lv2.noCollision(this) && !lv2.containsAnyLiquid(this.getBoundingBox())) {
          bl2 = true;
        }
      }
    }

    if (!bl2) {
      this.teleportTo(g, h, i);
      return false;
    } else {
      if (broadcastTeleport) {
        lv2.broadcastEntityEvent(this, (byte)46);
      }

      if (this instanceof PathfinderMob lv5) {
        lv5.getNavigation().stop();
      }

      return true;
    }
  }

  public boolean isAffectedByPotions() {
    return !this.isDeadOrDying();
  }

  public boolean attackable() {
    return true;
  }

  public void setRecordPlayingNearby(BlockPos jukebox, boolean partyParrot) {
  }

  public boolean canTakeItem(ItemStack stack) {
    return false;
  }

  @Override
  public final EntityDimensions getDimensions(Pose pose) {
    return pose == Pose.SLEEPING ? SLEEPING_DIMENSIONS : this.getDefaultDimensions(pose).scale(this.getScale());
  }

  protected EntityDimensions getDefaultDimensions(Pose pose) {
    return this.getType().getDimensions().scale(this.getAgeScale());
  }

  public ImmutableList<Pose> getDismountPoses() {
    return ImmutableList.of(Pose.STANDING);
  }

  public AABB getLocalBoundsForPose(Pose pose) {
    EntityDimensions lv = this.getDimensions(pose);
    return new AABB(
      (double)(-lv.width() / 2.0F), 0.0, (double)(-lv.width() / 2.0F), (double)(lv.width() / 2.0F), (double)lv.height(), (double)(lv.width() / 2.0F)
    );
  }

  protected boolean wouldNotSuffocateAtTargetPose(Pose pose) {
    AABB lv = this.getDimensions(pose).makeBoundingBox(this.position());
    return this.level().noBlockCollision(this, lv);
  }

  @Override
  public boolean canUsePortal(boolean allowPassengers) {
    return super.canUsePortal(allowPassengers) && !this.isSleeping();
  }

  public Optional<BlockPos> getSleepingPos() {
    return this.entityData.get(SLEEPING_POS_ID);
  }

  public void setSleepingPos(BlockPos pos) {
    this.entityData.set(SLEEPING_POS_ID, Optional.of(pos));
  }

  public void clearSleepingPos() {
    this.entityData.set(SLEEPING_POS_ID, Optional.empty());
  }

  public boolean isSleeping() {
    return this.getSleepingPos().isPresent();
  }

  public void startSleeping(BlockPos pos) {
    if (this.isPassenger()) {
      this.stopRiding();
    }

    BlockState lv = this.level().getBlockState(pos);
    if (lv.getBlock() instanceof BedBlock) {
      this.level().setBlock(pos, lv.setValue(BedBlock.OCCUPIED, Boolean.valueOf(true)), 3);
    }

    this.setPose(Pose.SLEEPING);
    this.setPosToBed(pos);
    this.setSleepingPos(pos);
    this.setDeltaMovement(Vec3.ZERO);
    this.hasImpulse = true;
  }

  private void setPosToBed(BlockPos pos) {
    this.setPos((double)pos.getX() + 0.5, (double)pos.getY() + 0.6875, (double)pos.getZ() + 0.5);
  }

  private boolean checkBedExists() {
    return this.getSleepingPos().map(pos -> this.level().getBlockState(pos).getBlock() instanceof BedBlock).orElse(false);
  }

  public void stopSleeping() {
    this.getSleepingPos().filter(this.level()::hasChunkAt).ifPresent(pos -> {
      BlockState lvx = this.level().getBlockState(pos);
      if (lvx.getBlock() instanceof BedBlock) {
        Direction lv2 = lvx.getValue(BedBlock.FACING);
        this.level().setBlock(pos, lvx.setValue(BedBlock.OCCUPIED, Boolean.valueOf(false)), 3);
        Vec3 lv3 = BedBlock.findStandUpPosition(this.getType(), this.level(), pos, lv2, this.getYRot()).orElseGet(() -> {
          BlockPos lvxx = pos.above();
          return new Vec3((double)lvxx.getX() + 0.5, (double)lvxx.getY() + 0.1, (double)lvxx.getZ() + 0.5);
        });
        Vec3 lv4 = Vec3.atBottomCenterOf(pos).subtract(lv3).normalize();
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

  private static byte entityEventForEquipmentBreak(EquipmentSlot slot) {
    return switch (slot) {
      case MAINHAND -> 47;
      case OFFHAND -> 48;
      case HEAD -> 49;
      case CHEST -> 50;
      case FEET -> 52;
      case LEGS -> 51;
      case BODY -> 65;
    };
  }

  public void onEquippedItemBroken(Item item, EquipmentSlot slot) {
    this.level().broadcastEntityEvent(this, entityEventForEquipmentBreak(slot));
  }

  public static EquipmentSlot getSlotForHand(InteractionHand hand) {
    return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
  }

  private void updateFallFlying() {
    boolean bl = this.getSharedFlag(7);
    if (bl && !this.onGround() && !this.isPassenger() && !this.hasEffect(EffectType.LEVITATION)) {
      ItemStack lv = this.getItemBySlot(EquipmentSlot.BODY);
      if (lv.is(ItemType.ELYTRA) && ElytraItem.isFlyEnabled(lv)) {
        bl = true;
        int i = this.fallFlyTicks + 1;
      } else {
        bl = false;
      }
    } else {
      bl = false;
    }
  }

  public EquipmentSlot getEquipmentSlotForItem(ItemStack stack) {
    Equipable lv = Equipable.get(stack);
    if (lv != null) {
      EquipmentSlot lv2 = lv.getEquipmentSlot();
      if (this.canUseSlot(lv2)) {
        return lv2;
      }
    }

    return EquipmentSlot.MAINHAND;
  }

  private static SlotAccess createEquipmentSlotAccess(LivingEntity entity, EquipmentSlot slot) {
    return slot != EquipmentSlot.HEAD && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND
      ? SlotAccess.forEquipmentSlot(entity, slot, stack -> stack.isEmpty() || entity.getEquipmentSlotForItem(stack) == slot)
      : SlotAccess.forEquipmentSlot(entity, slot);
  }

  @Override
  public AABB getBoundingBoxForCulling() {
    if (this.getItemBySlot(EquipmentSlot.HEAD).is(ItemType.DRAGON_HEAD)) {
      float f = 0.5F;
      return this.getBoundingBox().inflate(0.5, 0.5, 0.5);
    } else {
      return super.getBoundingBoxForCulling();
    }
  }

  @Override
  public SlotAccess getSlot(int slot) {
    EquipmentSlot lv = getEquipmentSlot(slot);
    return lv != null ? createEquipmentSlotAccess(this, lv) : super.getSlot(slot);
  }

  @Override
  public boolean canFreeze() {
    if (this.isSpectator()) {
      return false;
    } else {
      boolean bl = !this.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
              && !this.getItemBySlot(EquipmentSlot.BODY).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
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
  public void recreateFromPacket(ClientboundAddEntityPacket packet) {
    double d = packet.getX();
    double e = packet.getY();
    double f = packet.getZ();
    float g = packet.getYRot();
    float h = packet.getXRot();
    this.syncPacketPositionCodec(d, e, f);
    this.yBodyRot = packet.getYHeadRot();
    this.yHeadRot = packet.getYHeadRot();
    this.yBodyRotO = this.yBodyRot;
    this.yHeadRotO = this.yHeadRot;
    this.setId(packet.getId());
    this.setUUID(packet.getUUID());
    this.absMoveTo(d, e, f, g, h);
    this.setDeltaMovement(packet.getXa(), packet.getYa(), packet.getZa());
  }

  public boolean canDisableShield() {
    return this.getWeaponItem().getItem() instanceof AxeItem;
  }

  @Override
  public float maxUpStep() {
    float f = (float) this.getAttributeValue(AttributeType.STEP_HEIGHT);
    return this.getControllingPassenger() instanceof Player ? Math.max(f, 1.0F) : f;
  }

  @Override
  public Vec3 getPassengerRidingPosition(Entity entity) {
    return this.position().add(this.getPassengerAttachmentPoint(entity, this.getDimensions(this.getPose()), this.getScale() * this.getAgeScale()));
  }

  protected void lerpHeadRotationStep(int lerpHeadSteps, double lerpYHeadRot) {
    this.yHeadRot = (float)Mth.rotLerp(1.0 / (double)lerpHeadSteps, (double)this.yHeadRot, lerpYHeadRot);
  }

  @Override
  public void igniteForTicks(int ticks) {
    super.igniteForTicks(Mth.ceil((double) ticks * this.getAttributeValue(AttributeType.BURNING_TIME)));
  }

  public boolean hasInfiniteMaterials() {
    return false;
  }

  @Override
  public boolean isInvulnerableTo(DamageSource source) {
    return super.isInvulnerableTo(source);
  }
}
