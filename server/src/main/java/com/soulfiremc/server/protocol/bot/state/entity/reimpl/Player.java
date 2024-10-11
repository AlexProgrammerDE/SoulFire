package com.soulfiremc.server.protocol.bot.state.entity.reimpl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

public abstract class Player extends LivingEntity {
  private static final Logger LOGGER = LogUtils.getLogger();
  public static final HumanoidArm DEFAULT_MAIN_HAND = HumanoidArm.RIGHT;
  public static final int DEFAULT_MODEL_CUSTOMIZATION = 0;
  public static final int MAX_HEALTH = 20;
  public static final int SLEEP_DURATION = 100;
  public static final int WAKE_UP_DURATION = 10;
  public static final int ENDER_SLOT_OFFSET = 200;
  public static final int HELD_ITEM_SLOT = 499;
  public static final int CRAFTING_SLOT_OFFSET = 500;
  public static final float DEFAULT_BLOCK_INTERACTION_RANGE = 4.5F;
  public static final float DEFAULT_ENTITY_INTERACTION_RANGE = 3.0F;
  public static final float CROUCH_BB_HEIGHT = 1.5F;
  public static final float SWIMMING_BB_WIDTH = 0.6F;
  public static final float SWIMMING_BB_HEIGHT = 0.6F;
  public static final float DEFAULT_EYE_HEIGHT = 1.62F;
  private static final int CURRENT_IMPULSE_CONTEXT_RESET_GRACE_TIME_TICKS = 40;
  public static final Vec3 DEFAULT_VEHICLE_ATTACHMENT = new Vec3(0.0, 0.6, 0.0);
  public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.8F)
    .withEyeHeight(1.62F)
    .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, DEFAULT_VEHICLE_ATTACHMENT));
  private static final Map<Pose, EntityDimensions> POSES = ImmutableMap.builder()
    .put(Pose.STANDING, STANDING_DIMENSIONS)
    .put(Pose.SLEEPING, SLEEPING_DIMENSIONS)
    .put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F))
    .put(
      Pose.CROUCHING,
      EntityDimensions.scalable(0.6F, 1.5F)
        .withEyeHeight(1.27F)
        .withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, DEFAULT_VEHICLE_ATTACHMENT))
    )
    .put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(1.62F))
    .build();
  private static final EntityDataAccessor<Float> DATA_PLAYER_ABSORPTION_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
  private static final EntityDataAccessor<Integer> DATA_SCORE_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
  protected static final EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
  protected static final EntityDataAccessor<Byte> DATA_PLAYER_MAIN_HAND = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
  protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_LEFT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
  protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_RIGHT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
  private long timeEntitySatOnShoulder;
  final Inventory inventory = new Inventory(this);
  protected PlayerEnderChestContainer enderChestInventory = new PlayerEnderChestContainer();
  public final InventoryMenu inventoryMenu;
  public AbstractContainerMenu containerMenu;
  protected FoodData foodData = new FoodData();
  protected int jumpTriggerTime;
  public float oBob;
  public float bob;
  public int takeXpDelay;
  public double xCloakO;
  public double yCloakO;
  public double zCloakO;
  public double xCloak;
  public double yCloak;
  public double zCloak;
  private int sleepCounter;
  protected boolean wasUnderwater;
  private final Abilities abilities = new Abilities();
  public int experienceLevel;
  public int totalExperience;
  public float experienceProgress;
  protected int enchantmentSeed;
  protected final float defaultFlySpeed = 0.02F;
  private int lastLevelUpTime;
  private final GameProfile gameProfile;
  private boolean reducedDebugInfo;
  private ItemStack lastItemInMainHand = ItemStack.EMPTY;
  private final ItemCooldowns cooldowns = this.createItemCooldowns();
  private Optional<GlobalPos> lastDeathLocation = Optional.empty();
  @Nullable
  public FishingHook fishing;
  protected float hurtDir;
  @Nullable
  public Vec3 currentImpulseImpactPos;
  @Nullable
  public Entity currentExplosionCause;
  private boolean ignoreFallDamageFromCurrentImpulse;
  private int currentImpulseContextResetGraceTime;

  public Player(Level arg, BlockPos arg2, float f, GameProfile gameProfile) {
    super(EntityType.PLAYER, arg);
    this.setUUID(gameProfile.getId());
    this.gameProfile = gameProfile;
    this.inventoryMenu = new InventoryMenu(this.inventory, !arg.isClientSide, this);
    this.containerMenu = this.inventoryMenu;
    this.moveTo((double) arg2.getX() + 0.5, (double) (arg2.getY() + 1), (double) arg2.getZ() + 0.5, f, 0.0F);
    this.rotOffs = 180.0F;
  }

  public boolean blockActionRestricted(Level level, BlockPos pos, GameType gameMode) {
    if (!gameMode.isBlockPlacingRestricted()) {
      return false;
    } else if (gameMode == GameType.SPECTATOR) {
      return true;
    } else if (this.mayBuild()) {
      return false;
    } else {
      ItemStack lv = this.getMainHandItem();
      return lv.isEmpty() || !lv.canBreakBlockInAdventureMode(new BlockInWorld(level, pos, false));
    }
  }

  public static AttributeSupplier.Builder createAttributes() {
    return LivingEntity.createLivingAttributes()
      .add(Attributes.ATTACK_DAMAGE, 1.0)
      .add(Attributes.MOVEMENT_SPEED, 0.1F)
      .add(Attributes.ATTACK_SPEED)
      .add(Attributes.LUCK)
      .add(Attributes.BLOCK_INTERACTION_RANGE, 4.5)
      .add(Attributes.ENTITY_INTERACTION_RANGE, 3.0)
      .add(Attributes.BLOCK_BREAK_SPEED)
      .add(Attributes.SUBMERGED_MINING_SPEED)
      .add(Attributes.SNEAKING_SPEED)
      .add(Attributes.MINING_EFFICIENCY)
      .add(Attributes.SWEEPING_DAMAGE_RATIO);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(DATA_PLAYER_ABSORPTION_ID, 0.0F);
    builder.define(DATA_SCORE_ID, 0);
    builder.define(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0);
    builder.define(DATA_PLAYER_MAIN_HAND, (byte) DEFAULT_MAIN_HAND.getId());
    builder.define(DATA_SHOULDER_LEFT, new CompoundTag());
    builder.define(DATA_SHOULDER_RIGHT, new CompoundTag());
  }

  @Override
  public void tick() {
    this.noPhysics = this.isSpectator();
    if (this.isSpectator()) {
      this.setOnGround(false);
    }

    if (this.takeXpDelay > 0) {
      this.takeXpDelay--;
    }

    if (this.isSleeping()) {
      this.sleepCounter++;
      if (this.sleepCounter > 100) {
        this.sleepCounter = 100;
      }
    } else if (this.sleepCounter > 0) {
      this.sleepCounter++;
      if (this.sleepCounter >= 110) {
        this.sleepCounter = 0;
      }
    }

    this.updateIsUnderwater();
    super.tick();

    this.moveCloak();

    int i = 29999999;
    double d = Mth.clamp(this.getX(), -2.9999999E7, 2.9999999E7);
    double e = Mth.clamp(this.getZ(), -2.9999999E7, 2.9999999E7);
    if (d != this.getX() || e != this.getZ()) {
      this.setPos(d, this.getY(), e);
    }

    this.attackStrengthTicker++;
    ItemStack lv = this.getMainHandItem();
    if (!ItemStack.matches(this.lastItemInMainHand, lv)) {
      if (!ItemStack.isSameItem(this.lastItemInMainHand, lv)) {
        this.resetAttackStrengthTicker();
      }

      this.lastItemInMainHand = lv.copy();
    }

    this.turtleHelmetTick();
    this.cooldowns.tick();
    this.updatePlayerPose();
    if (this.currentImpulseContextResetGraceTime > 0) {
      this.currentImpulseContextResetGraceTime--;
    }
  }

  @Override
  protected float getMaxHeadRotationRelativeToBody() {
    return this.isBlocking() ? 15.0F : super.getMaxHeadRotationRelativeToBody();
  }

  public boolean isSecondaryUseActive() {
    return this.isShiftKeyDown();
  }

  protected boolean wantsToStopRiding() {
    return this.isShiftKeyDown();
  }

  protected boolean isStayingOnGroundSurface() {
    return this.isShiftKeyDown();
  }

  protected boolean updateIsUnderwater() {
    this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
    return this.wasUnderwater;
  }

  private void turtleHelmetTick() {
    ItemStack lv = this.getItemBySlot(EquipmentSlot.HEAD);
    if (lv.is(Items.TURTLE_HELMET) && !this.isEyeInFluid(FluidTags.WATER)) {
      this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
    }
  }

  protected ItemCooldowns createItemCooldowns() {
    return new ItemCooldowns();
  }

  private void moveCloak() {
    this.xCloakO = this.xCloak;
    this.yCloakO = this.yCloak;
    this.zCloakO = this.zCloak;
    double d = this.getX() - this.xCloak;
    double e = this.getY() - this.yCloak;
    double f = this.getZ() - this.zCloak;
    double g = 10.0;
    if (d > 10.0) {
      this.xCloak = this.getX();
      this.xCloakO = this.xCloak;
    }

    if (f > 10.0) {
      this.zCloak = this.getZ();
      this.zCloakO = this.zCloak;
    }

    if (e > 10.0) {
      this.yCloak = this.getY();
      this.yCloakO = this.yCloak;
    }

    if (d < -10.0) {
      this.xCloak = this.getX();
      this.xCloakO = this.xCloak;
    }

    if (f < -10.0) {
      this.zCloak = this.getZ();
      this.zCloakO = this.zCloak;
    }

    if (e < -10.0) {
      this.yCloak = this.getY();
      this.yCloakO = this.yCloak;
    }

    this.xCloak += d * 0.25;
    this.zCloak += f * 0.25;
    this.yCloak += e * 0.25;
  }

  protected void updatePlayerPose() {
    if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {
      Pose lv;
      if (this.isFallFlying()) {
        lv = Pose.FALL_FLYING;
      } else if (this.isSleeping()) {
        lv = Pose.SLEEPING;
      } else if (this.isSwimming()) {
        lv = Pose.SWIMMING;
      } else if (this.isAutoSpinAttack()) {
        lv = Pose.SPIN_ATTACK;
      } else if (this.isShiftKeyDown() && !this.abilities.flying) {
        lv = Pose.CROUCHING;
      } else {
        lv = Pose.STANDING;
      }

      Pose lv2;
      if (this.isSpectator() || this.isPassenger() || this.canPlayerFitWithinBlocksAndEntitiesWhen(lv)) {
        lv2 = lv;
      } else if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)) {
        lv2 = Pose.CROUCHING;
      } else {
        lv2 = Pose.SWIMMING;
      }

      this.setPose(lv2);
    }
  }

  protected boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose) {
    return this.level().noCollision(this, this.getDimensions(pose).makeBoundingBox(this.position()).deflate(1.0E-7));
  }

  @Override
  protected SoundEvent getSwimSound() {
    return SoundEvents.PLAYER_SWIM;
  }

  @Override
  protected SoundEvent getSwimSplashSound() {
    return SoundEvents.PLAYER_SPLASH;
  }

  @Override
  protected SoundEvent getSwimHighSpeedSplashSound() {
    return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
  }

  @Override
  public int getDimensionChangingDelay() {
    return 10;
  }

  @Override
  public SoundSource getSoundSource() {
    return SoundSource.PLAYERS;
  }

  @Override
  protected int getFireImmuneTicks() {
    return 20;
  }

  @Override
  public void handleEntityEvent(byte id) {
    if (id == 9) {
      this.completeUsingItem();
    } else if (id == 23) {
      this.reducedDebugInfo = false;
    } else if (id == 22) {
      this.reducedDebugInfo = true;
    } else {
      super.handleEntityEvent(id);
    }
  }

  protected void closeContainer() {
    this.containerMenu = this.inventoryMenu;
  }

  protected void doCloseContainer() {
  }

  @Override
  public void rideTick() {
    super.rideTick();
    this.oBob = this.bob;
    this.bob = 0.0F;
  }

  @Override
  protected void serverAiStep() {
    super.serverAiStep();
    this.updateSwingTime();
    this.yHeadRot = this.getYRot();
  }

  @Override
  public void aiStep() {
    if (this.jumpTriggerTime > 0) {
      this.jumpTriggerTime--;
    }

    if (this.level().getDifficulty() == Difficulty.PEACEFUL && this.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
      if (this.getHealth() < this.getMaxHealth() && this.tickCount % 20 == 0) {
        this.heal(1.0F);
      }

      if (this.foodData.getSaturationLevel() < 20.0F && this.tickCount % 20 == 0) {
        this.foodData.setSaturation(this.foodData.getSaturationLevel() + 1.0F);
      }

      if (this.foodData.needsFood() && this.tickCount % 10 == 0) {
        this.foodData.setFoodLevel(this.foodData.getFoodLevel() + 1);
      }
    }

    this.inventory.tick();
    this.oBob = this.bob;
    super.aiStep();
    this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
    float f;
    if (this.onGround() && !this.isDeadOrDying() && !this.isSwimming()) {
      f = Math.min(0.1F, (float) this.getDeltaMovement().horizontalDistance());
    } else {
      f = 0.0F;
    }

    this.bob = this.bob + (f - this.bob) * 0.4F;
    if (this.getHealth() > 0.0F && !this.isSpectator()) {
      AABB lv;
      if (this.isPassenger() && !this.getVehicle().isRemoved()) {
        lv = this.getBoundingBox().minmax(this.getVehicle().getBoundingBox()).inflate(1.0, 0.0, 1.0);
      } else {
        lv = this.getBoundingBox().inflate(1.0, 0.5, 1.0);
      }

      List<Entity> list = this.level().getEntities(this, lv);
      List<Entity> list2 = Lists.newArrayList();

      for (Entity lv2 : list) {
        if (lv2.getType() == EntityType.EXPERIENCE_ORB) {
          list2.add(lv2);
        } else if (!lv2.isRemoved()) {
          this.touch(lv2);
        }
      }

      if (!list2.isEmpty()) {
        this.touch(Util.getRandom(list2, this.random));
      }
    }
  }

  private void touch(Entity entity) {
    entity.playerTouch(this);
  }

  public int getScore() {
    return this.entityData.get(DATA_SCORE_ID);
  }

  public void setScore(int score) {
    this.entityData.set(DATA_SCORE_ID, score);
  }

  public void increaseScore(int score) {
    int j = this.getScore();
    this.entityData.set(DATA_SCORE_ID, j + score);
  }

  public void startAutoSpinAttack(int ticks, float damage, ItemStack itemStack) {
    this.autoSpinAttackTicks = ticks;
    this.autoSpinAttackDmg = damage;
    this.autoSpinAttackItemStack = itemStack;
  }

  @NotNull
  @Override
  public ItemStack getWeaponItem() {
    return this.isAutoSpinAttack() && this.autoSpinAttackItemStack != null ? this.autoSpinAttackItemStack : super.getWeaponItem();
  }

  @Override
  public void die(DamageSource damageSource) {
    super.die(damageSource);
    this.reapplyPosition();
    if (!this.isSpectator() && this.level() instanceof ServerLevel lv) {
      this.dropAllDeathLoot(lv, damageSource);
    }

    if (damageSource != null) {
      this.setDeltaMovement(
        (double) (-Mth.cos((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F),
        0.1F,
        (double) (-Mth.sin((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F)
      );
    } else {
      this.setDeltaMovement(0.0, 0.1, 0.0);
    }

    this.awardStat(Stats.DEATHS);
    this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
    this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
    this.clearFire();
    this.setSharedFlagOnFire(false);
    this.setLastDeathLocation(Optional.of(GlobalPos.of(this.level().dimension(), this.blockPosition())));
  }

  @Override
  protected void dropEquipment() {
    super.dropEquipment();
    if (!this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
      this.destroyVanishingCursedItems();
      this.inventory.dropAll();
    }
  }

  protected void destroyVanishingCursedItems() {
    for (int i = 0; i < this.inventory.getContainerSize(); i++) {
      ItemStack lv = this.inventory.getItem(i);
      if (!lv.isEmpty() && EnchantmentHelper.has(lv, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
        this.inventory.removeItemNoUpdate(i);
      }
    }
  }

  @Override
  protected SoundEvent getHurtSound(DamageSource damageSource) {
    return damageSource.type().effects().sound();
  }

  @Override
  protected SoundEvent getDeathSound() {
    return SoundEvents.PLAYER_DEATH;
  }

  @Nullable
  public ItemEntity drop(ItemStack itemStack, boolean includeThrowerName) {
    return this.drop(itemStack, false, includeThrowerName);
  }

  @Nullable
  public ItemEntity drop(ItemStack droppedItem, boolean dropAround, boolean includeThrowerName) {
    if (droppedItem.isEmpty()) {
      return null;
    } else {
      if (this.level().isClientSide) {
        this.swing(InteractionHand.MAIN_HAND);
      }

      double d = this.getEyeY() - 0.3F;
      ItemEntity lv = new ItemEntity(this.level(), this.getX(), d, this.getZ(), droppedItem);
      lv.setPickUpDelay(40);
      if (includeThrowerName) {
        lv.setThrower(this);
      }

      if (dropAround) {
        float f = this.random.nextFloat() * 0.5F;
        float g = this.random.nextFloat() * (float) (Math.PI * 2);
        lv.setDeltaMovement((double) (-Mth.sin(g) * f), 0.2F, (double) (Mth.cos(g) * f));
      } else {
        float f = 0.3F;
        float g = Mth.sin(this.getXRot() * (float) (Math.PI / 180.0));
        float h = Mth.cos(this.getXRot() * (float) (Math.PI / 180.0));
        float i = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
        float j = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
        float k = this.random.nextFloat() * (float) (Math.PI * 2);
        float l = 0.02F * this.random.nextFloat();
        lv.setDeltaMovement(
          (double) (-i * h * 0.3F) + Math.cos((double) k) * (double) l,
          (double) (-g * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F),
          (double) (j * h * 0.3F) + Math.sin((double) k) * (double) l
        );
      }

      return lv;
    }
  }

  public float getDestroySpeed(BlockState state) {
    float f = this.inventory.getDestroySpeed(state);
    if (f > 1.0F) {
      f += (float) this.getAttributeValue(Attributes.MINING_EFFICIENCY);
    }

    if (MobEffectUtil.hasDigSpeed(this)) {
      f *= 1.0F + (float) (MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
    }

    if (this.hasEffect(MobEffects.DIG_SLOWDOWN)) {
      f *= switch (this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) {
        case 0 -> 0.3F;
        case 1 -> 0.09F;
        case 2 -> 0.0027F;
        default -> 8.1E-4F;
      };
    }

    f *= (float) this.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
    if (this.isEyeInFluid(FluidTags.WATER)) {
      f *= (float) this.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();
    }

    if (!this.onGround()) {
      f /= 5.0F;
    }

    return f;
  }

  public boolean hasCorrectToolForDrops(BlockState state) {
    return !state.requiresCorrectToolForDrops() || this.inventory.getSelected().isCorrectToolForDrops(state);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag compound) {
    super.readAdditionalSaveData(compound);
    this.setUUID(this.gameProfile.getId());
    ListTag lv = compound.getList("Inventory", 10);
    this.inventory.load(lv);
    this.inventory.selected = compound.getInt("SelectedItemSlot");
    this.sleepCounter = compound.getShort("SleepTimer");
    this.experienceProgress = compound.getFloat("XpP");
    this.experienceLevel = compound.getInt("XpLevel");
    this.totalExperience = compound.getInt("XpTotal");
    this.enchantmentSeed = compound.getInt("XpSeed");
    if (this.enchantmentSeed == 0) {
      this.enchantmentSeed = this.random.nextInt();
    }

    this.setScore(compound.getInt("Score"));
    this.foodData.readAdditionalSaveData(compound);
    this.abilities.loadSaveData(compound);
    this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double) this.abilities.getWalkingSpeed());
    if (compound.contains("EnderItems", 9)) {
      this.enderChestInventory.fromTag(compound.getList("EnderItems", 10), this.registryAccess());
    }

    if (compound.contains("ShoulderEntityLeft", 10)) {
      this.setShoulderEntityLeft(compound.getCompound("ShoulderEntityLeft"));
    }

    if (compound.contains("ShoulderEntityRight", 10)) {
      this.setShoulderEntityRight(compound.getCompound("ShoulderEntityRight"));
    }

    if (compound.contains("LastDeathLocation", 10)) {
      this.setLastDeathLocation(GlobalPos.CODEC.parse(NbtOps.INSTANCE, compound.get("LastDeathLocation")).resultOrPartial(LOGGER::error));
    }

    if (compound.contains("current_explosion_impact_pos", 9)) {
      Vec3.CODEC
        .parse(NbtOps.INSTANCE, compound.get("current_explosion_impact_pos"))
        .resultOrPartial(LOGGER::error)
        .ifPresent(currentImpulseImpactPos -> this.currentImpulseImpactPos = currentImpulseImpactPos);
    }

    this.ignoreFallDamageFromCurrentImpulse = compound.getBoolean("ignore_fall_damage_from_current_explosion");
    this.currentImpulseContextResetGraceTime = compound.getInt("current_impulse_context_reset_grace_time");
  }

  @Override
  public void addAdditionalSaveData(CompoundTag compound) {
    super.addAdditionalSaveData(compound);
    NbtUtils.addCurrentDataVersion(compound);
    compound.put("Inventory", this.inventory.save(new ListTag()));
    compound.putInt("SelectedItemSlot", this.inventory.selected);
    compound.putShort("SleepTimer", (short) this.sleepCounter);
    compound.putFloat("XpP", this.experienceProgress);
    compound.putInt("XpLevel", this.experienceLevel);
    compound.putInt("XpTotal", this.totalExperience);
    compound.putInt("XpSeed", this.enchantmentSeed);
    compound.putInt("Score", this.getScore());
    this.foodData.addAdditionalSaveData(compound);
    this.abilities.addSaveData(compound);
    compound.put("EnderItems", this.enderChestInventory.createTag(this.registryAccess()));
    if (!this.getShoulderEntityLeft().isEmpty()) {
      compound.put("ShoulderEntityLeft", this.getShoulderEntityLeft());
    }

    if (!this.getShoulderEntityRight().isEmpty()) {
      compound.put("ShoulderEntityRight", this.getShoulderEntityRight());
    }

    this.getLastDeathLocation()
      .flatMap(arg -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, arg).resultOrPartial(LOGGER::error))
      .ifPresent(arg2 -> compound.put("LastDeathLocation", arg2));
    if (this.currentImpulseImpactPos != null) {
      compound.put("current_explosion_impact_pos", (Tag) Vec3.CODEC.encodeStart(NbtOps.INSTANCE, this.currentImpulseImpactPos).getOrThrow());
    }

    compound.putBoolean("ignore_fall_damage_from_current_explosion", this.ignoreFallDamageFromCurrentImpulse);
    compound.putInt("current_impulse_context_reset_grace_time", this.currentImpulseContextResetGraceTime);
  }

  @Override
  public boolean isInvulnerableTo(DamageSource source) {
    if (super.isInvulnerableTo(source)) {
      return true;
    } else if (source.is(DamageTypeTags.IS_DROWNING)) {
      return !this.level().getGameRules().getBoolean(GameRules.RULE_DROWNING_DAMAGE);
    } else if (source.is(DamageTypeTags.IS_FALL)) {
      return !this.level().getGameRules().getBoolean(GameRules.RULE_FALL_DAMAGE);
    } else if (source.is(DamageTypeTags.IS_FIRE)) {
      return !this.level().getGameRules().getBoolean(GameRules.RULE_FIRE_DAMAGE);
    } else {
      return source.is(DamageTypeTags.IS_FREEZING) ? !this.level().getGameRules().getBoolean(GameRules.RULE_FREEZE_DAMAGE) : false;
    }
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    if (this.isInvulnerableTo(source)) {
      return false;
    } else if (this.abilities.invulnerable && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
      return false;
    } else {
      this.noActionTime = 0;
      if (this.isDeadOrDying()) {
        return false;
      } else {
        if (source.scalesWithDifficulty()) {
          if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            amount = 0.0F;
          }

          if (this.level().getDifficulty() == Difficulty.EASY) {
            amount = Math.min(amount / 2.0F + 1.0F, amount);
          }

          if (this.level().getDifficulty() == Difficulty.HARD) {
            amount = amount * 3.0F / 2.0F;
          }
        }

        return amount == 0.0F ? false : super.hurt(source, amount);
      }
    }
  }

  @Override
  protected void blockUsingShield(LivingEntity attacker) {
    super.blockUsingShield(attacker);
    if (attacker.canDisableShield()) {
      this.disableShield();
    }
  }

  @Override
  public boolean canBeSeenAsEnemy() {
    return !this.getAbilities().invulnerable && super.canBeSeenAsEnemy();
  }

  public boolean canHarmPlayer(Player other) {
    Team lv = this.getTeam();
    Team lv2 = other.getTeam();
    if (lv == null) {
      return true;
    } else {
      return !lv.isAlliedTo(lv2) ? true : lv.isAllowFriendlyFire();
    }
  }

  @Override
  protected void hurtArmor(DamageSource damageSource, float damageAmount) {
    this.doHurtEquipment(damageSource, damageAmount, new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD});
  }

  @Override
  protected void hurtHelmet(DamageSource damageSource, float damageAmount) {
    this.doHurtEquipment(damageSource, damageAmount, new EquipmentSlot[]{EquipmentSlot.HEAD});
  }

  @Override
  protected void hurtCurrentlyUsedShield(float damageAmount) {
    if (this.useItem.is(Items.SHIELD)) {
      if (damageAmount >= 3.0F) {
        int i = 1 + Mth.floor(damageAmount);
        InteractionHand lv = this.getUsedItemHand();
        this.useItem.hurtAndBreak(i, this, getSlotForHand(lv));
        if (this.useItem.isEmpty()) {
          if (lv == InteractionHand.MAIN_HAND) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
          } else {
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
          }

          this.useItem = ItemStack.EMPTY;
        }
      }
    }
  }

  @Override
  protected void actuallyHurt(DamageSource damageSource, float damageAmount) {
    if (!this.isInvulnerableTo(damageSource)) {
      damageAmount = this.getDamageAfterArmorAbsorb(damageSource, damageAmount);
      damageAmount = this.getDamageAfterMagicAbsorb(damageSource, damageAmount);
      float var7 = Math.max(damageAmount - this.getAbsorptionAmount(), 0.0F);
      this.setAbsorptionAmount(this.getAbsorptionAmount() - (damageAmount - var7));
      float h = damageAmount - var7;
      if (h > 0.0F && h < 3.4028235E37F) {
        this.awardStat(Stats.DAMAGE_ABSORBED, Math.round(h * 10.0F));
      }

      if (var7 != 0.0F) {
        this.getCombatTracker().recordDamage(damageSource, var7);
        this.setHealth(this.getHealth() - var7);
        if (var7 < 3.4028235E37F) {
          this.awardStat(Stats.DAMAGE_TAKEN, Math.round(var7 * 10.0F));
        }

        this.gameEvent(GameEvent.ENTITY_DAMAGE);
      }
    }
  }

  public boolean isTextFilteringEnabled() {
    return false;
  }

  public void openTextEdit(SignBlockEntity signEntity, boolean isFrontText) {
  }

  public void openMinecartCommandBlock(BaseCommandBlock commandEntity) {
  }

  public void openCommandBlock(CommandBlockEntity commandBlockEntity) {
  }

  public void openStructureBlock(StructureBlockEntity structureEntity) {
  }

  public void openJigsawBlock(JigsawBlockEntity jigsawBlockEntity) {
  }

  public void openHorseInventory(AbstractHorse horse, Container inventory) {
  }

  public OptionalInt openMenu(@Nullable MenuProvider menu) {
    return OptionalInt.empty();
  }

  public void sendMerchantOffers(int containerId, MerchantOffers offers, int villagerLevel, int villagerXp, boolean showProgress, boolean canRestock) {
  }

  public void openItemGui(ItemStack stack, InteractionHand hand) {
  }

  public InteractionResult interactOn(Entity entityToInteractOn, InteractionHand hand) {
    if (this.isSpectator()) {
      if (entityToInteractOn instanceof MenuProvider) {
        this.openMenu((MenuProvider) entityToInteractOn);
      }

      return InteractionResult.PASS;
    } else {
      ItemStack lv = this.getItemInHand(hand);
      ItemStack lv2 = lv.copy();
      InteractionResult lv3 = entityToInteractOn.interact(this, hand);
      if (lv3.consumesAction()) {
        if (this.abilities.instabuild && lv == this.getItemInHand(hand) && lv.getCount() < lv2.getCount()) {
          lv.setCount(lv2.getCount());
        }

        return lv3;
      } else {
        if (!lv.isEmpty() && entityToInteractOn instanceof LivingEntity) {
          if (this.abilities.instabuild) {
            lv = lv2;
          }

          InteractionResult lv4 = lv.interactLivingEntity(this, (LivingEntity) entityToInteractOn, hand);
          if (lv4.consumesAction()) {
            this.level().gameEvent(GameEvent.ENTITY_INTERACT, entityToInteractOn.position(), GameEvent.Context.of(this));
            if (lv.isEmpty() && !this.abilities.instabuild) {
              this.setItemInHand(hand, ItemStack.EMPTY);
            }

            return lv4;
          }
        }

        return InteractionResult.PASS;
      }
    }
  }

  @Override
  public void removeVehicle() {
    super.removeVehicle();
    this.boardingCooldown = 0;
  }

  @Override
  protected boolean isImmobile() {
    return super.isImmobile() || this.isSleeping();
  }

  @Override
  public boolean isAffectedByFluids() {
    return !this.abilities.flying;
  }

  @Override
  protected Vec3 maybeBackOffFromEdge(Vec3 vec, MoverType mover) {
    float f = this.maxUpStep();
    if (!this.abilities.flying
      && !(vec.y > 0.0)
      && (mover == MoverType.SELF || mover == MoverType.PLAYER)
      && this.isStayingOnGroundSurface()
      && this.isAboveGround(f)) {
      double d = vec.x;
      double e = vec.z;
      double g = 0.05;
      double h = Math.signum(d) * 0.05;

      double i;
      for (i = Math.signum(e) * 0.05; d != 0.0 && this.canFallAtLeast(d, 0.0, f); d -= h) {
        if (Math.abs(d) <= 0.05) {
          d = 0.0;
          break;
        }
      }

      while (e != 0.0 && this.canFallAtLeast(0.0, e, f)) {
        if (Math.abs(e) <= 0.05) {
          e = 0.0;
          break;
        }

        e -= i;
      }

      while (d != 0.0 && e != 0.0 && this.canFallAtLeast(d, e, f)) {
        if (Math.abs(d) <= 0.05) {
          d = 0.0;
        } else {
          d -= h;
        }

        if (Math.abs(e) <= 0.05) {
          e = 0.0;
        } else {
          e -= i;
        }
      }

      return new Vec3(d, vec.y, e);
    } else {
      return vec;
    }
  }

  private boolean isAboveGround(float maxUpStep) {
    return this.onGround() || this.fallDistance < maxUpStep && !this.canFallAtLeast(0.0, 0.0, maxUpStep - this.fallDistance);
  }

  private boolean canFallAtLeast(double x, double z, float distance) {
    AABB lv = this.getBoundingBox();
    return this.level().noCollision(this, new AABB(lv.minX + x, lv.minY - (double) distance - 1.0E-5F, lv.minZ + z, lv.maxX + x, lv.minY, lv.maxZ + z));
  }

  public void attack(Entity target) {
    if (target.isAttackable()) {
      if (!target.skipAttackInteraction(this)) {
        float f = this.isAutoSpinAttack() ? this.autoSpinAttackDmg : (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ItemStack lv = this.getWeaponItem();
        DamageSource lv2 = this.damageSources().playerAttack(this);
        float g = this.getEnchantedDamage(target, f, lv2) - f;
        float h = this.getAttackStrengthScale(0.5F);
        f *= 0.2F + h * h * 0.8F;
        g *= h;
        this.resetAttackStrengthTicker();
        if (target.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE)
          && target instanceof Projectile lv3
          && lv3.deflect(ProjectileDeflection.AIM_DEFLECT, this, this, true)) {
          return;
        }

        if (f > 0.0F || g > 0.0F) {
          boolean bl = h > 0.9F;
          boolean bl2;
          if (this.isSprinting() && bl) {
            bl2 = true;
          } else {
            bl2 = false;
          }

          f += lv.getItem().getAttackDamageBonus(target, f, lv2);
          boolean bl3 = bl
            && this.fallDistance > 0.0F
            && !this.onGround()
            && !this.onClimbable()
            && !this.isInWater()
            && !this.hasEffect(MobEffects.BLINDNESS)
            && !this.isPassenger()
            && target instanceof LivingEntity
            && !this.isSprinting();
          if (bl3) {
            f *= 1.5F;
          }

          float i = f + g;
          boolean bl4 = false;
          double d = (double) (this.walkDist - this.walkDistO);
          if (bl && !bl3 && !bl2 && this.onGround() && d < (double) this.getSpeed()) {
            ItemStack lv4 = this.getItemInHand(InteractionHand.MAIN_HAND);
            if (lv4.getItem() instanceof SwordItem) {
              bl4 = true;
            }
          }

          float j = 0.0F;
          if (target instanceof LivingEntity lv5) {
            j = lv5.getHealth();
          }

          Vec3 lv6 = target.getDeltaMovement();
          boolean bl5 = target.hurt(lv2, i);
          if (bl5) {
            float k = this.getKnockback(target, lv2) + (bl2 ? 1.0F : 0.0F);
            if (k > 0.0F) {
              if (target instanceof LivingEntity lv7) {
                lv7.knockback(
                  (double) (k * 0.5F),
                  (double) Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)),
                  (double) (-Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)))
                );
              } else {
                target.push(
                  (double) (-Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)) * k * 0.5F),
                  0.1,
                  (double) (Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * k * 0.5F)
                );
              }

              this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
              this.setSprinting(false);
            }

            if (bl4) {
              float l = 1.0F + (float) this.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * f;

              for (LivingEntity lv8 : this.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.0, 0.25, 1.0))) {
                if (lv8 != this
                  && lv8 != target
                  && !this.isAlliedTo(lv8)
                  && (!(lv8 instanceof ArmorStand) || !((ArmorStand) lv8).isMarker())
                  && this.distanceToSqr(lv8) < 9.0) {
                  float m = this.getEnchantedDamage(lv8, l, lv2) * h;
                  lv8.knockback(
                    0.4F, (double) Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), (double) (-Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)))
                  );
                  lv8.hurt(lv2, m);
                  if (this.level() instanceof ServerLevel lv9) {
                    EnchantmentHelper.doPostAttackEffects(lv9, lv8, lv2);
                  }
                }
              }

              this.sweepAttack();
            }

            if (target instanceof ServerPlayer && target.hurtMarked) {
              ((ServerPlayer) target).connection.send(new ClientboundSetEntityMotionPacket(target));
              target.hurtMarked = false;
              target.setDeltaMovement(lv6);
            }

            if (bl3) {
              this.crit(target);
            }

            if (g > 0.0F) {
              this.magicCrit(target);
            }

            this.setLastHurtMob(target);
            Entity lv10 = target;
            if (target instanceof EnderDragonPart) {
              lv10 = ((EnderDragonPart) target).parentMob;
            }

            boolean bl6 = false;
            if (this.level() instanceof ServerLevel lv11) {
              if (lv10 instanceof LivingEntity lv8x) {
                bl6 = lv.hurtEnemy(lv8x, this);
              }

              EnchantmentHelper.doPostAttackEffects(lv11, target, lv2);
            }

            if (target instanceof LivingEntity) {
              float n = j - ((LivingEntity) target).getHealth();
              this.awardStat(Stats.DAMAGE_DEALT, Math.round(n * 10.0F));
              if (this.level() instanceof ServerLevel && n > 2.0F) {
                int o = (int) ((double) n * 0.5);
                ((ServerLevel) this.level())
                  .sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY(0.5), target.getZ(), o, 0.1, 0.0, 0.1, 0.2);
              }
            }
          }
        }
      }
    }
  }

  protected float getEnchantedDamage(Entity entity, float damage, DamageSource damageSource) {
    return damage;
  }

  @Override
  protected void doAutoAttackOnTouch(LivingEntity target) {
    this.attack(target);
  }

  public void disableShield() {
    this.getCooldowns().addCooldown(Items.SHIELD, 100);
    this.stopUsingItem();
    this.level().broadcastEntityEvent(this, (byte) 30);
  }

  public void crit(Entity entityHit) {
  }

  public void magicCrit(Entity entityHit) {
  }

  public void sweepAttack() {
    double d = (double) (-Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)));
    double e = (double) Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
    if (this.level() instanceof ServerLevel) {
      ((ServerLevel) this.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + d, this.getY(0.5), this.getZ() + e, 0, d, 0.0, e, 0.0);
    }
  }

  public void respawn() {
  }

  @Override
  public void remove(Entity.RemovalReason reason) {
    super.remove(reason);
    this.inventoryMenu.removed(this);
    if (this.containerMenu != null && this.hasContainerOpen()) {
      this.doCloseContainer();
    }
  }

  public boolean isLocalPlayer() {
    return false;
  }

  public GameProfile getGameProfile() {
    return this.gameProfile;
  }

  public Inventory getInventory() {
    return this.inventory;
  }

  public Abilities getAbilities() {
    return this.abilities;
  }

  @Override
  public boolean hasInfiniteMaterials() {
    return this.abilities.instabuild;
  }

  public void updateTutorialInventoryAction(ItemStack carried, ItemStack clicked, ClickAction action) {
  }

  public boolean hasContainerOpen() {
    return this.containerMenu != this.inventoryMenu;
  }

  public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos bedPos) {
    this.startSleeping(bedPos);
    this.sleepCounter = 0;
    return Either.right(Unit.INSTANCE);
  }

  public void stopSleepInBed(boolean wakeImmediately, boolean updateLevelForSleepingPlayers) {
    super.stopSleeping();
    if (this.level() instanceof ServerLevel && updateLevelForSleepingPlayers) {
      ((ServerLevel) this.level()).updateSleepingPlayerList();
    }

    this.sleepCounter = wakeImmediately ? 0 : 100;
  }

  @Override
  public void stopSleeping() {
    this.stopSleepInBed(true, true);
  }

  @Override
  public void jumpFromGround() {
    super.jumpFromGround();
    this.awardStat(Stats.JUMP);
  }

  @Override
  public void travel(Vec3 travelVector) {
    if (this.isSwimming() && !this.isPassenger()) {
      double d = this.getLookAngle().y;
      double e = d < -0.2 ? 0.085 : 0.06;
      if (d <= 0.0
        || this.jumping
        || !this.level().getBlockState(BlockPos.containing(this.getX(), this.getY() + 1.0 - 0.1, this.getZ())).getFluidState().isEmpty()) {
        Vec3 lv = this.getDeltaMovement();
        this.setDeltaMovement(lv.add(0.0, (d - lv.y) * e, 0.0));
      }
    }

    if (this.abilities.flying && !this.isPassenger()) {
      double d = this.getDeltaMovement().y;
      super.travel(travelVector);
      Vec3 lv2 = this.getDeltaMovement();
      this.setDeltaMovement(lv2.x, d * 0.6, lv2.z);
      this.resetFallDistance();
      this.setSharedFlag(7, false);
    } else {
      super.travel(travelVector);
    }
  }

  @Override
  public void updateSwimming() {
    if (this.abilities.flying) {
      this.setSwimming(false);
    } else {
      super.updateSwimming();
    }
  }

  @Override
  public float getSpeed() {
    return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
  }

  @Override
  public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
    if (this.abilities.mayfly) {
      return false;
    } else {
      if (fallDistance >= 2.0F) {
        this.awardStat(Stats.FALL_ONE_CM, (int) Math.round((double) fallDistance * 100.0));
      }

      boolean bl;
      if (this.ignoreFallDamageFromCurrentImpulse && this.currentImpulseImpactPos != null) {
        double d = this.currentImpulseImpactPos.y;
        this.tryResetCurrentImpulseContext();
        if (d < this.getY()) {
          return false;
        }

        float h = Math.min(fallDistance, (float) (d - this.getY()));
        bl = super.causeFallDamage(h, multiplier, source);
      } else {
        bl = super.causeFallDamage(fallDistance, multiplier, source);
      }

      if (bl) {
        this.resetCurrentImpulseContext();
      }

      return bl;
    }
  }

  public boolean tryToStartFallFlying() {
    if (!this.onGround() && !this.isFallFlying() && !this.isInWater() && !this.hasEffect(MobEffects.LEVITATION)) {
      ItemStack lv = this.getItemBySlot(EquipmentSlot.CHEST);
      if (lv.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(lv)) {
        this.startFallFlying();
        return true;
      }
    }

    return false;
  }

  public void startFallFlying() {
    this.setSharedFlag(7, true);
  }

  public void stopFallFlying() {
    this.setSharedFlag(7, true);
    this.setSharedFlag(7, false);
  }

  @Override
  protected void doWaterSplashEffect() {
    if (!this.isSpectator()) {
      super.doWaterSplashEffect();
    }
  }

  @Override
  public LivingEntity.Fallsounds getFallSounds() {
    return new LivingEntity.Fallsounds(SoundEvents.PLAYER_SMALL_FALL, SoundEvents.PLAYER_BIG_FALL);
  }

  @Override
  public boolean killedEntity(ServerLevel level, LivingEntity entity) {
    this.awardStat(Stats.ENTITY_KILLED.get(entity.getType()));
    return true;
  }

  @Override
  public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
    if (!this.abilities.flying) {
      super.makeStuckInBlock(state, motionMultiplier);
    }

    this.tryResetCurrentImpulseContext();
  }

  public FoodData getFoodData() {
    return this.foodData;
  }

  public boolean canEat(boolean canAlwaysEat) {
    return this.abilities.invulnerable || canAlwaysEat || this.foodData.needsFood();
  }

  public boolean isHurt() {
    return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
  }

  public boolean mayBuild() {
    return this.abilities.mayBuild;
  }

  public boolean mayUseItemAt(BlockPos pos, Direction facing, ItemStack stack) {
    if (this.abilities.mayBuild) {
      return true;
    } else {
      BlockPos lv = pos.relative(facing.getOpposite());
      BlockInWorld lv2 = new BlockInWorld(this.level(), lv, false);
      return stack.canPlaceOnBlockInAdventureMode(lv2);
    }
  }

  @Override
  protected int getBaseExperienceReward() {
    if (!this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && !this.isSpectator()) {
      int i = this.experienceLevel * 7;
      return i > 100 ? 100 : i;
    } else {
      return 0;
    }
  }

  @Override
  protected boolean isAlwaysExperienceDropper() {
    return true;
  }

  @Override
  public boolean shouldShowName() {
    return true;
  }

  @Override
  protected Entity.MovementEmission getMovementEmission() {
    return this.abilities.flying || this.onGround() && this.isDiscrete() ? Entity.MovementEmission.NONE : Entity.MovementEmission.ALL;
  }

  public void onUpdateAbilities() {
  }

  @Override
  public Component getName() {
    return Component.literal(this.gameProfile.getName());
  }

  public PlayerEnderChestContainer getEnderChestInventory() {
    return this.enderChestInventory;
  }

  @Override
  public ItemStack getItemBySlot(EquipmentSlot slot) {
    if (slot == EquipmentSlot.MAINHAND) {
      return this.inventory.getSelected();
    } else if (slot == EquipmentSlot.OFFHAND) {
      return this.inventory.offhand.get(0);
    } else {
      return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ? this.inventory.armor.get(slot.getIndex()) : ItemStack.EMPTY;
    }
  }

  @Override
  protected boolean doesEmitEquipEvent(EquipmentSlot slot) {
    return slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
  }

  @Override
  public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    this.verifyEquippedItem(stack);
    if (slot == EquipmentSlot.MAINHAND) {
      this.onEquipItem(slot, this.inventory.items.set(this.inventory.selected, stack), stack);
    } else if (slot == EquipmentSlot.OFFHAND) {
      this.onEquipItem(slot, this.inventory.offhand.set(0, stack), stack);
    } else if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
      this.onEquipItem(slot, this.inventory.armor.set(slot.getIndex(), stack), stack);
    }
  }

  public boolean addItem(ItemStack stack) {
    return this.inventory.add(stack);
  }

  @Override
  public Iterable<ItemStack> getHandSlots() {
    return Lists.newArrayList(new ItemStack[]{this.getMainHandItem(), this.getOffhandItem()});
  }

  @Override
  public Iterable<ItemStack> getArmorSlots() {
    return this.inventory.armor;
  }

  @Override
  public boolean canUseSlot(EquipmentSlot slot) {
    return slot != EquipmentSlot.BODY;
  }

  public boolean setEntityOnShoulder(CompoundTag entityCompound) {
    if (this.isPassenger() || !this.onGround() || this.isInWater() || this.isInPowderSnow) {
      return false;
    } else if (this.getShoulderEntityLeft().isEmpty()) {
      this.setShoulderEntityLeft(entityCompound);
      this.timeEntitySatOnShoulder = this.level().getGameTime();
      return true;
    } else if (this.getShoulderEntityRight().isEmpty()) {
      this.setShoulderEntityRight(entityCompound);
      this.timeEntitySatOnShoulder = this.level().getGameTime();
      return true;
    } else {
      return false;
    }
  }

  @Override
  public abstract boolean isSpectator();

  @Override
  public boolean canBeHitByProjectile() {
    return !this.isSpectator() && super.canBeHitByProjectile();
  }

  @Override
  public boolean isSwimming() {
    return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
  }

  public abstract boolean isCreative();

  @Override
  public boolean isPushedByFluid() {
    return !this.abilities.flying;
  }

  @Override
  public Component getDisplayName() {
    MutableComponent lv = PlayerTeam.formatNameForTeam(this.getTeam(), this.getName());
    return this.decorateDisplayNameComponent(lv);
  }

  private MutableComponent decorateDisplayNameComponent(MutableComponent displayName) {
    String string = this.getGameProfile().getName();
    return displayName.withStyle(
      arg -> arg.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + string + " "))
        .withHoverEvent(this.createHoverEvent())
        .withInsertion(string)
    );
  }

  @Override
  public String getScoreboardName() {
    return this.getGameProfile().getName();
  }

  @Override
  protected void internalSetAbsorptionAmount(float absorptionAmount) {
    this.getEntityData().set(DATA_PLAYER_ABSORPTION_ID, absorptionAmount);
  }

  @Override
  public float getAbsorptionAmount() {
    return this.getEntityData().get(DATA_PLAYER_ABSORPTION_ID);
  }

  public boolean isModelPartShown(PlayerModelPart part) {
    return (this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) & part.getMask()) == part.getMask();
  }

  @Override
  public SlotAccess getSlot(int slot) {
    if (slot == 499) {
      return new SlotAccess() {
        @Override
        public ItemStack get() {
          return Player.this.containerMenu.getCarried();
        }

        @Override
        public boolean set(ItemStack carried) {
          Player.this.containerMenu.setCarried(carried);
          return true;
        }
      };
    } else {
      final int j = slot - 500;
      if (j >= 0 && j < 4) {
        return new SlotAccess() {
          @Override
          public ItemStack get() {
            return Player.this.inventoryMenu.getCraftSlots().getItem(j);
          }

          @Override
          public boolean set(ItemStack carried) {
            Player.this.inventoryMenu.getCraftSlots().setItem(j, carried);
            Player.this.inventoryMenu.slotsChanged(Player.this.inventory);
            return true;
          }
        };
      } else if (slot >= 0 && slot < this.inventory.items.size()) {
        return SlotAccess.forContainer(this.inventory, slot);
      } else {
        int k = slot - 200;
        return k >= 0 && k < this.enderChestInventory.getContainerSize() ? SlotAccess.forContainer(this.enderChestInventory, k) : super.getSlot(slot);
      }
    }
  }

  public boolean isReducedDebugInfo() {
    return this.reducedDebugInfo;
  }

  public void setReducedDebugInfo(boolean reducedDebugInfo) {
    this.reducedDebugInfo = reducedDebugInfo;
  }

  @Override
  public void setRemainingFireTicks(int remainingFireTicks) {
    super.setRemainingFireTicks(this.abilities.invulnerable ? Math.min(remainingFireTicks, 1) : remainingFireTicks);
  }

  @Override
  public HumanoidArm getMainArm() {
    return this.entityData.get(DATA_PLAYER_MAIN_HAND) == 0 ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
  }

  public void setMainArm(HumanoidArm hand) {
    this.entityData.set(DATA_PLAYER_MAIN_HAND, (byte) (hand == HumanoidArm.LEFT ? 0 : 1));
  }

  public CompoundTag getShoulderEntityLeft() {
    return this.entityData.get(DATA_SHOULDER_LEFT);
  }

  protected void setShoulderEntityLeft(CompoundTag entityCompound) {
    this.entityData.set(DATA_SHOULDER_LEFT, entityCompound);
  }

  public CompoundTag getShoulderEntityRight() {
    return this.entityData.get(DATA_SHOULDER_RIGHT);
  }

  protected void setShoulderEntityRight(CompoundTag entityCompound) {
    this.entityData.set(DATA_SHOULDER_RIGHT, entityCompound);
  }

  public float getCurrentItemAttackStrengthDelay() {
    return (float) (1.0 / this.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0);
  }

  public float getAttackStrengthScale(float adjustTicks) {
    return Mth.clamp(((float) this.attackStrengthTicker + adjustTicks) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
  }

  public void resetAttackStrengthTicker() {
    this.attackStrengthTicker = 0;
  }

  public ItemCooldowns getCooldowns() {
    return this.cooldowns;
  }

  @Override
  protected float getBlockSpeedFactor() {
    return !this.abilities.flying && !this.isFallFlying() ? super.getBlockSpeedFactor() : 1.0F;
  }

  public float getLuck() {
    return (float) this.getAttributeValue(Attributes.LUCK);
  }

  public boolean canUseGameMasterBlocks() {
    return this.abilities.instabuild && this.getPermissionLevel() >= 2;
  }

  @Override
  public boolean canTakeItem(ItemStack stack) {
    EquipmentSlot lv = this.getEquipmentSlotForItem(stack);
    return this.getItemBySlot(lv).isEmpty();
  }

  @Override
  public EntityDimensions getDefaultDimensions(Pose pose) {
    return POSES.getOrDefault(pose, STANDING_DIMENSIONS);
  }

  @Override
  public ImmutableList<Pose> getDismountPoses() {
    return ImmutableList.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING);
  }

  @Override
  public ItemStack getProjectile(ItemStack weaponStack) {
    if (!(weaponStack.getItem() instanceof ProjectileWeaponItem)) {
      return ItemStack.EMPTY;
    } else {
      Predicate<ItemStack> predicate = ((ProjectileWeaponItem) weaponStack.getItem()).getSupportedHeldProjectiles();
      ItemStack lv = ProjectileWeaponItem.getHeldProjectile(this, predicate);
      if (!lv.isEmpty()) {
        return lv;
      } else {
        predicate = ((ProjectileWeaponItem) weaponStack.getItem()).getAllSupportedProjectiles();

        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
          ItemStack lv2 = this.inventory.getItem(i);
          if (predicate.test(lv2)) {
            return lv2;
          }
        }

        return this.abilities.instabuild ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
      }
    }
  }

  @Override
  public ItemStack eat(Level level, ItemStack food, FoodProperties foodProperties) {
    this.getFoodData().eat(foodProperties);
    this.awardStat(Stats.ITEM_USED.get(food.getItem()));

    ItemStack lv = super.eat(level, food, foodProperties);
    Optional<ItemStack> optional = foodProperties.usingConvertsTo();
    if (optional.isPresent() && !this.hasInfiniteMaterials()) {
      if (lv.isEmpty()) {
        return optional.get().copy();
      }
    }

    return lv;
  }

  @Override
  public Vec3 getRopeHoldPosition(float partialTicks) {
    double d = 0.22 * (this.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0);
    float g = Mth.lerp(partialTicks * 0.5F, this.getXRot(), this.xRotO) * (float) (Math.PI / 180.0);
    float h = Mth.lerp(partialTicks, this.yBodyRotO, this.yBodyRot) * (float) (Math.PI / 180.0);
    if (this.isFallFlying() || this.isAutoSpinAttack()) {
      Vec3 lv = this.getViewVector(partialTicks);
      Vec3 lv2 = this.getDeltaMovement();
      double e = lv2.horizontalDistanceSqr();
      double i = lv.horizontalDistanceSqr();
      float l;
      if (e > 0.0 && i > 0.0) {
        double j = (lv2.x * lv.x + lv2.z * lv.z) / Math.sqrt(e * i);
        double k = lv2.x * lv.z - lv2.z * lv.x;
        l = (float) (Math.signum(k) * Math.acos(j));
      } else {
        l = 0.0F;
      }

      return this.getPosition(partialTicks).add(new Vec3(d, -0.11, 0.85).zRot(-l).xRot(-g).yRot(-h));
    } else if (this.isVisuallySwimming()) {
      return this.getPosition(partialTicks).add(new Vec3(d, 0.2, -0.15).xRot(-g).yRot(-h));
    } else {
      double m = this.getBoundingBox().getYsize() - 1.0;
      double e = this.isCrouching() ? -0.2 : 0.07;
      return this.getPosition(partialTicks).add(new Vec3(d, m, e).yRot(-h));
    }
  }

  @Override
  public boolean isAlwaysTicking() {
    return true;
  }

  @Override
  public boolean shouldBeSaved() {
    return false;
  }

  public Optional<GlobalPos> getLastDeathLocation() {
    return this.lastDeathLocation;
  }

  public void setLastDeathLocation(Optional<GlobalPos> lastDeathLocation) {
    this.lastDeathLocation = lastDeathLocation;
  }

  @Override
  public float getHurtDir() {
    return this.hurtDir;
  }

  @Override
  public void animateHurt(float yaw) {
    super.animateHurt(yaw);
    this.hurtDir = yaw;
  }

  @Override
  public boolean canSprint() {
    return true;
  }

  @Override
  protected float getFlyingSpeed() {
    if (this.abilities.flying && !this.isPassenger()) {
      return this.isSprinting() ? this.abilities.getFlyingSpeed() * 2.0F : this.abilities.getFlyingSpeed();
    } else {
      return this.isSprinting() ? 0.025999999F : 0.02F;
    }
  }

  public double blockInteractionRange() {
    return this.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
  }

  public double entityInteractionRange() {
    return this.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
  }

  public boolean canInteractWithEntity(Entity entity, double distance) {
    return entity.isRemoved() ? false : this.canInteractWithEntity(entity.getBoundingBox(), distance);
  }

  public boolean canInteractWithEntity(AABB boundingBox, double distance) {
    double e = this.entityInteractionRange() + distance;
    return boundingBox.distanceToSqr(this.getEyePosition()) < e * e;
  }

  public boolean canInteractWithBlock(BlockPos pos, double distance) {
    double e = this.blockInteractionRange() + distance;
    return new AABB(pos).distanceToSqr(this.getEyePosition()) < e * e;
  }

  public void setIgnoreFallDamageFromCurrentImpulse(boolean ignoreFallDamageFromCurrentImpulse) {
    this.ignoreFallDamageFromCurrentImpulse = ignoreFallDamageFromCurrentImpulse;
    if (ignoreFallDamageFromCurrentImpulse) {
      this.currentImpulseContextResetGraceTime = 40;
    } else {
      this.currentImpulseContextResetGraceTime = 0;
    }
  }

  public boolean isIgnoringFallDamageFromCurrentImpulse() {
    return this.ignoreFallDamageFromCurrentImpulse;
  }

  public void tryResetCurrentImpulseContext() {
    if (this.currentImpulseContextResetGraceTime == 0) {
      this.resetCurrentImpulseContext();
    }
  }

  public void resetCurrentImpulseContext() {
    this.currentImpulseContextResetGraceTime = 0;
    this.currentExplosionCause = null;
    this.currentImpulseImpactPos = null;
    this.ignoreFallDamageFromCurrentImpulse = false;
  }

  public static enum BedSleepingProblem {
    NOT_POSSIBLE_HERE,
    NOT_POSSIBLE_NOW(Component.translatable("block.minecraft.bed.no_sleep")),
    TOO_FAR_AWAY(Component.translatable("block.minecraft.bed.too_far_away")),
    OBSTRUCTED(Component.translatable("block.minecraft.bed.obstructed")),
    OTHER_PROBLEM,
    NOT_SAFE(Component.translatable("block.minecraft.bed.not_safe"));

    @Nullable
    private final Component message;

    private BedSleepingProblem() {
      this.message = null;
    }

    private BedSleepingProblem(final Component arg) {
      this.message = arg;
    }

    @Nullable
    public Component getMessage() {
      return this.message;
    }
  }
}
