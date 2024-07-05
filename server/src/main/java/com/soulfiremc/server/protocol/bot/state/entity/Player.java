package com.soulfiremc.server.protocol.bot.state.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

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
    this.inventoryMenu = new InventoryMenu(this.inventory, false, this);
    this.containerMenu = this.inventoryMenu;
    this.moveTo((double)arg2.getX() + 0.5, (double)(arg2.getY() + 1), (double)arg2.getZ() + 0.5, f, 0.0F);
    this.rotOffs = 180.0F;
  }

  public boolean blockActionRestricted(Level arg, BlockPos arg2, GameType arg3) {
    if (!arg3.isBlockPlacingRestricted()) {
      return false;
    } else if (arg3 == GameType.SPECTATOR) {
      return true;
    } else if (this.mayBuild()) {
      return false;
    } else {
      ItemStack lv = this.getMainHandItem();
      return lv.isEmpty() || !lv.canBreakBlockInAdventureMode(new BlockInWorld(arg, arg2, false));
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
  protected void defineSynchedData(SynchedEntityData.Builder arg) {
    super.defineSynchedData(arg);
    arg.define(DATA_PLAYER_ABSORPTION_ID, 0.0F);
    arg.define(DATA_SCORE_ID, 0);
    arg.define(DATA_PLAYER_MODE_CUSTOMISATION, (byte)0);
    arg.define(DATA_PLAYER_MAIN_HAND, (byte)DEFAULT_MAIN_HAND.getId());
    arg.define(DATA_SHOULDER_LEFT, new CompoundTag());
    arg.define(DATA_SHOULDER_RIGHT, new CompoundTag());
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
      this.addEffect(new MobEffectInstance(EffectType.WATER_BREATHING, 200, 0, false, false, true));
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

  protected boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose arg) {
    return this.level().noCollision(this, this.getDimensions(arg).makeBoundingBox(this.position()).deflate(1.0E-7));
  }

  @Override
  public int getDimensionChangingDelay() {
    return 10;
  }

  @Override
  protected int getFireImmuneTicks() {
    return 20;
  }

  @Override
  public void handleEntityEvent(byte b) {
    if (b == 9) {
      this.completeUsingItem();
    } else if (b == 23) {
      this.reducedDebugInfo = false;
    } else if (b == 22) {
      this.reducedDebugInfo = true;
    } else {
      super.handleEntityEvent(b);
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
    this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
    float f;
    if (this.onGround() && !this.isDeadOrDying() && !this.isSwimming()) {
      f = Math.min(0.1F, (float)this.getDeltaMovement().horizontalDistance());
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

  private void touch(Entity arg) {
    arg.playerTouch(this);
  }

  public int getScore() {
    return this.entityData.get(DATA_SCORE_ID);
  }

  public void setScore(int i) {
    this.entityData.set(DATA_SCORE_ID, i);
  }

  public void increaseScore(int i) {
    int j = this.getScore();
    this.entityData.set(DATA_SCORE_ID, j + i);
  }

  public void startAutoSpinAttack(int i, float f, ItemStack arg) {
    this.autoSpinAttackTicks = i;
    this.autoSpinAttackDmg = f;
    this.autoSpinAttackItemStack = arg;
  }

  @NotNull
  @Override
  public ItemStack getWeaponItem() {
    return this.isAutoSpinAttack() && this.autoSpinAttackItemStack != null ? this.autoSpinAttackItemStack : super.getWeaponItem();
  }

  @Override
  public void die(DamageSource arg) {
    super.die(arg);
    this.reapplyPosition();

    if (arg != null) {
      this.setDeltaMovement(
        (double)(-Mth.cos((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F),
        0.1F,
        (double)(-Mth.sin((this.getHurtDir() + this.getYRot()) * (float) (Math.PI / 180.0)) * 0.1F)
      );
    } else {
      this.setDeltaMovement(0.0, 0.1, 0.0);
    }

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

  @Nullable
  public ItemEntity drop(ItemStack arg, boolean bl) {
    return this.drop(arg, false, bl);
  }

  @Nullable
  public ItemEntity drop(ItemStack arg, boolean bl, boolean bl2) {
    if (arg.isEmpty()) {
      return null;
    } else {
      this.swing(InteractionHand.MAIN_HAND);

      double d = this.getEyeY() - 0.3F;
      ItemEntity lv = new ItemEntity(this.level(), this.getX(), d, this.getZ(), arg);
      lv.setPickUpDelay(40);
      if (bl2) {
        lv.setThrower(this);
      }

      if (bl) {
        float f = this.random.nextFloat() * 0.5F;
        float g = this.random.nextFloat() * (float) (Math.PI * 2);
        lv.setDeltaMovement((double)(-Mth.sin(g) * f), 0.2F, (double)(Mth.cos(g) * f));
      } else {
        float f = 0.3F;
        float g = Mth.sin(this.getXRot() * (float) (Math.PI / 180.0));
        float h = Mth.cos(this.getXRot() * (float) (Math.PI / 180.0));
        float i = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
        float j = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
        float k = this.random.nextFloat() * (float) (Math.PI * 2);
        float l = 0.02F * this.random.nextFloat();
        lv.setDeltaMovement(
          (double)(-i * h * 0.3F) + Math.cos((double)k) * (double)l,
          (double)(-g * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F),
          (double)(j * h * 0.3F) + Math.sin((double)k) * (double)l
        );
      }

      return lv;
    }
  }

  public float getDestroySpeed(BlockState arg) {
    float f = this.inventory.getDestroySpeed(arg);
    if (f > 1.0F) {
      f += (float)this.getAttributeValue(Attributes.MINING_EFFICIENCY);
    }

    if (MobEffectUtil.hasDigSpeed(this)) {
      f *= 1.0F + (float)(MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
    }

    if (this.hasEffect(EffectType.DIG_SLOWDOWN)) {
      f *= switch (this.getEffect(EffectType.DIG_SLOWDOWN).getAmplifier()) {
        case 0 -> 0.3F;
        case 1 -> 0.09F;
        case 2 -> 0.0027F;
        default -> 8.1E-4F;
      };
    }

    f *= (float)this.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
    if (this.isEyeInFluid(FluidTags.WATER)) {
      f *= (float)this.getAttribute(Attributes.SUBMERGED_MINING_SPEED).getValue();
    }

    if (!this.onGround()) {
      f /= 5.0F;
    }

    return f;
  }

  public boolean hasCorrectToolForDrops(BlockState arg) {
    return !arg.requiresCorrectToolForDrops() || this.inventory.getSelected().isCorrectToolForDrops(arg);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag arg) {
    super.readAdditionalSaveData(arg);
    this.setUUID(this.gameProfile.getId());
    ListTag lv = arg.getList("Inventory", 10);
    this.inventory.load(lv);
    this.inventory.selected = arg.getInt("SelectedItemSlot");
    this.sleepCounter = arg.getShort("SleepTimer");
    this.experienceProgress = arg.getFloat("XpP");
    this.experienceLevel = arg.getInt("XpLevel");
    this.totalExperience = arg.getInt("XpTotal");
    this.enchantmentSeed = arg.getInt("XpSeed");
    if (this.enchantmentSeed == 0) {
      this.enchantmentSeed = this.random.nextInt();
    }

    this.setScore(arg.getInt("Score"));
    this.foodData.readAdditionalSaveData(arg);
    this.abilities.loadSaveData(arg);
    this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double)this.abilities.getWalkingSpeed());
    if (arg.contains("EnderItems", 9)) {
      this.enderChestInventory.fromTag(arg.getList("EnderItems", 10), this.registryAccess());
    }

    if (arg.contains("ShoulderEntityLeft", 10)) {
      this.setShoulderEntityLeft(arg.getCompound("ShoulderEntityLeft"));
    }

    if (arg.contains("ShoulderEntityRight", 10)) {
      this.setShoulderEntityRight(arg.getCompound("ShoulderEntityRight"));
    }

    if (arg.contains("LastDeathLocation", 10)) {
      this.setLastDeathLocation(GlobalPos.CODEC.parse(NbtOps.INSTANCE, arg.get("LastDeathLocation")).resultOrPartial(LOGGER::error));
    }

    if (arg.contains("current_explosion_impact_pos", 9)) {
      Vec3.CODEC
        .parse(NbtOps.INSTANCE, arg.get("current_explosion_impact_pos"))
        .resultOrPartial(LOGGER::error)
        .ifPresent(argx -> this.currentImpulseImpactPos = argx);
    }

    this.ignoreFallDamageFromCurrentImpulse = arg.getBoolean("ignore_fall_damage_from_current_explosion");
    this.currentImpulseContextResetGraceTime = arg.getInt("current_impulse_context_reset_grace_time");
  }

  @Override
  public void addAdditionalSaveData(CompoundTag arg) {
    super.addAdditionalSaveData(arg);
    NbtUtils.addCurrentDataVersion(arg);
    arg.put("Inventory", this.inventory.save(new ListTag()));
    arg.putInt("SelectedItemSlot", this.inventory.selected);
    arg.putShort("SleepTimer", (short)this.sleepCounter);
    arg.putFloat("XpP", this.experienceProgress);
    arg.putInt("XpLevel", this.experienceLevel);
    arg.putInt("XpTotal", this.totalExperience);
    arg.putInt("XpSeed", this.enchantmentSeed);
    arg.putInt("Score", this.getScore());
    this.foodData.addAdditionalSaveData(arg);
    this.abilities.addSaveData(arg);
    arg.put("EnderItems", this.enderChestInventory.createTag(this.registryAccess()));
    if (!this.getShoulderEntityLeft().isEmpty()) {
      arg.put("ShoulderEntityLeft", this.getShoulderEntityLeft());
    }

    if (!this.getShoulderEntityRight().isEmpty()) {
      arg.put("ShoulderEntityRight", this.getShoulderEntityRight());
    }

    this.getLastDeathLocation()
      .flatMap(argx -> GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, argx).resultOrPartial(LOGGER::error))
      .ifPresent(arg2 -> arg.put("LastDeathLocation", arg2));
    if (this.currentImpulseImpactPos != null) {
      arg.put("current_explosion_impact_pos", (Tag)Vec3.CODEC.encodeStart(NbtOps.INSTANCE, this.currentImpulseImpactPos).getOrThrow());
    }

    arg.putBoolean("ignore_fall_damage_from_current_explosion", this.ignoreFallDamageFromCurrentImpulse);
    arg.putInt("current_impulse_context_reset_grace_time", this.currentImpulseContextResetGraceTime);
  }

  @Override
  public boolean isInvulnerableTo(DamageSource arg) {
    if (super.isInvulnerableTo(arg)) {
      return true;
    } else if (arg.is(DamageTypeTags.IS_DROWNING)) {
      return !this.level().getGameRules().getBoolean(GameRules.RULE_DROWNING_DAMAGE);
    } else if (arg.is(DamageTypeTags.IS_FALL)) {
      return !this.level().getGameRules().getBoolean(GameRules.RULE_FALL_DAMAGE);
    } else if (arg.is(DamageTypeTags.IS_FIRE)) {
      return !this.level().getGameRules().getBoolean(GameRules.RULE_FIRE_DAMAGE);
    } else {
      return arg.is(DamageTypeTags.IS_FREEZING) ? !this.level().getGameRules().getBoolean(GameRules.RULE_FREEZE_DAMAGE) : false;
    }
  }

  @Override
  public boolean hurt(DamageSource arg, float f) {
    if (this.isInvulnerableTo(arg)) {
      return false;
    } else if (this.abilities.invulnerable && !arg.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
      return false;
    } else {
      this.noActionTime = 0;
      if (this.isDeadOrDying()) {
        return false;
      } else {
        if (arg.scalesWithDifficulty()) {
          if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            f = 0.0F;
          }

          if (this.level().getDifficulty() == Difficulty.EASY) {
            f = Math.min(f / 2.0F + 1.0F, f);
          }

          if (this.level().getDifficulty() == Difficulty.HARD) {
            f = f * 3.0F / 2.0F;
          }
        }

        return f == 0.0F ? false : super.hurt(arg, f);
      }
    }
  }

  @Override
  protected void blockUsingShield(LivingEntity arg) {
    super.blockUsingShield(arg);
    if (arg.canDisableShield()) {
      this.disableShield();
    }
  }

  @Override
  public boolean canBeSeenAsEnemy() {
    return !this.getAbilities().invulnerable && super.canBeSeenAsEnemy();
  }

  public boolean canHarmPlayer(Player arg) {
    Team lv = this.getTeam();
    Team lv2 = arg.getTeam();
    if (lv == null) {
      return true;
    } else {
      return !lv.isAlliedTo(lv2) ? true : lv.isAllowFriendlyFire();
    }
  }

  @Override
  protected void hurtArmor(DamageSource arg, float f) {
    this.doHurtEquipment(arg, f, new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD});
  }

  @Override
  protected void hurtHelmet(DamageSource arg, float f) {
    this.doHurtEquipment(arg, f, new EquipmentSlot[]{EquipmentSlot.HEAD});
  }

  @Override
  protected void hurtCurrentlyUsedShield(float f) {
    if (this.useItem.is(Items.SHIELD)) {
      if (f >= 3.0F) {
        int i = 1 + Mth.floor(f);
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
  protected void actuallyHurt(DamageSource arg, float f) {
    if (!this.isInvulnerableTo(arg)) {
      f = this.getDamageAfterArmorAbsorb(arg, f);
      f = this.getDamageAfterMagicAbsorb(arg, f);
      float var7 = Math.max(f - this.getAbsorptionAmount(), 0.0F);
      this.setAbsorptionAmount(this.getAbsorptionAmount() - (f - var7));

      if (var7 != 0.0F) {
        this.causeFoodExhaustion(arg.getFoodExhaustion());
        this.getCombatTracker().recordDamage(arg, var7);
        this.setHealth(this.getHealth() - var7);
        this.gameEvent(GameEvent.ENTITY_DAMAGE);
      }
    }
  }

  public boolean isTextFilteringEnabled() {
    return false;
  }

  public void openTextEdit(SignBlockEntity arg, boolean bl) {
  }

  public void openMinecartCommandBlock(BaseCommandBlock arg) {
  }

  public void openCommandBlock(CommandBlockEntity arg) {
  }

  public void openStructureBlock(StructureBlockEntity arg) {
  }

  public void openJigsawBlock(JigsawBlockEntity arg) {
  }

  public void openHorseInventory(AbstractHorse arg, Container arg2) {
  }

  public OptionalInt openMenu(@Nullable MenuProvider arg) {
    return OptionalInt.empty();
  }

  public void sendMerchantOffers(int i, MerchantOffers arg, int j, int k, boolean bl, boolean bl2) {
  }

  public void openItemGui(ItemStack arg, InteractionHand arg2) {
  }

  public InteractionResult interactOn(Entity arg, InteractionHand arg2) {
    if (this.isSpectator()) {
      if (arg instanceof MenuProvider) {
        this.openMenu((MenuProvider)arg);
      }

      return InteractionResult.PASS;
    } else {
      ItemStack lv = this.getItemInHand(arg2);
      ItemStack lv2 = lv.copy();
      InteractionResult lv3 = arg.interact(this, arg2);
      if (lv3.consumesAction()) {
        if (this.abilities.instabuild && lv == this.getItemInHand(arg2) && lv.getCount() < lv2.getCount()) {
          lv.setCount(lv2.getCount());
        }

        return lv3;
      } else {
        if (!lv.isEmpty() && arg instanceof LivingEntity) {
          if (this.abilities.instabuild) {
            lv = lv2;
          }

          InteractionResult lv4 = lv.interactLivingEntity(this, (LivingEntity)arg, arg2);
          if (lv4.consumesAction()) {
            this.level().gameEvent(GameEvent.ENTITY_INTERACT, arg.position(), GameEvent.Context.of(this));
            if (lv.isEmpty() && !this.abilities.instabuild) {
              this.setItemInHand(arg2, ItemStack.EMPTY);
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
  protected Vec3 maybeBackOffFromEdge(Vec3 arg, MoverType arg2) {
    float f = this.maxUpStep();
    if (!this.abilities.flying
      && !(arg.y > 0.0)
      && (arg2 == MoverType.SELF || arg2 == MoverType.PLAYER)
      && this.isStayingOnGroundSurface()
      && this.isAboveGround(f)) {
      double d = arg.x;
      double e = arg.z;
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

      return new Vec3(d, arg.y, e);
    } else {
      return arg;
    }
  }

  private boolean isAboveGround(float f) {
    return this.onGround() || this.fallDistance < f && !this.canFallAtLeast(0.0, 0.0, f - this.fallDistance);
  }

  private boolean canFallAtLeast(double d, double e, float f) {
    AABB lv = this.getBoundingBox();
    return this.level().noCollision(this, new AABB(lv.minX + d, lv.minY - (double)f - 1.0E-5F, lv.minZ + e, lv.maxX + d, lv.minY, lv.maxZ + e));
  }

  public void attack(Entity arg) {
    if (arg.isAttackable()) {
      if (!arg.skipAttackInteraction(this)) {
        float f = this.isAutoSpinAttack() ? this.autoSpinAttackDmg : (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ItemStack lv = this.getWeaponItem();
        DamageSource lv2 = this.damageSources().playerAttack(this);
        float g = this.getEnchantedDamage(arg, f, lv2) - f;
        float h = this.getAttackStrengthScale(0.5F);
        f *= 0.2F + h * h * 0.8F;
        g *= h;
        this.resetAttackStrengthTicker();

        if (f > 0.0F || g > 0.0F) {
          boolean bl = h > 0.9F;
          boolean bl2;
          if (this.isSprinting() && bl) {
            bl2 = true;
          } else {
            bl2 = false;
          }

          f += lv.getItem().getAttackDamageBonus(arg, f, lv2);
          boolean bl3 = bl
            && this.fallDistance > 0.0F
            && !this.onGround()
            && !this.onClimbable()
            && !this.isInWater()
            && !this.hasEffect(EffectType.BLINDNESS)
            && !this.isPassenger()
            && arg instanceof LivingEntity
            && !this.isSprinting();
          if (bl3) {
            f *= 1.5F;
          }

          float i = f + g;
          boolean bl4 = false;
          double d = (double)(this.walkDist - this.walkDistO);
          if (bl && !bl3 && !bl2 && this.onGround() && d < (double)this.getSpeed()) {
            ItemStack lv4 = this.getItemInHand(InteractionHand.MAIN_HAND);
            if (lv4.getItem() instanceof SwordItem) {
              bl4 = true;
            }
          }

          float j = 0.0F;
          if (arg instanceof LivingEntity lv5) {
            j = lv5.getHealth();
          }

          Vec3 lv6 = arg.getDeltaMovement();
          boolean bl5 = arg.hurt(lv2, i);
          if (bl5) {
            float k = this.getKnockback(arg, lv2) + (bl2 ? 1.0F : 0.0F);
            if (k > 0.0F) {
              if (arg instanceof LivingEntity lv7) {
                lv7.knockback(
                  (double)(k * 0.5F),
                  (double)Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)),
                  (double)(-Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)))
                );
              } else {
                arg.push(
                  (double)(-Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)) * k * 0.5F),
                  0.1,
                  (double)(Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)) * k * 0.5F)
                );
              }

              this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
              this.setSprinting(false);
            }

            if (bl4) {
              float l = 1.0F + (float)this.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO) * f;

              for (LivingEntity lv8 : this.level().getEntitiesOfClass(LivingEntity.class, arg.getBoundingBox().inflate(1.0, 0.25, 1.0))) {
                if (lv8 != this
                  && lv8 != arg
                  && !this.isAlliedTo(lv8)
                  && (!(lv8 instanceof ArmorStand) || !((ArmorStand)lv8).isMarker())
                  && this.distanceToSqr(lv8) < 9.0) {
                  float m = this.getEnchantedDamage(lv8, l, lv2) * h;
                  lv8.knockback(
                    0.4F, (double)Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), (double)(-Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)))
                  );
                  lv8.hurt(lv2, m);
                }
              }

              this.sweepAttack();
            }

            if (bl3) {
              this.crit(arg);
            }

            if (g > 0.0F) {
              this.magicCrit(arg);
            }

            this.setLastHurtMob(arg);
            Entity lv10 = arg;
            if (arg instanceof EnderDragonPart) {
              lv10 = ((EnderDragonPart)arg).parentMob;
            }

            this.causeFoodExhaustion(0.1F);
          }
        }
      }
    }
  }

  protected float getEnchantedDamage(Entity arg, float f, DamageSource arg2) {
    return f;
  }

  @Override
  protected void doAutoAttackOnTouch(LivingEntity arg) {
    this.attack(arg);
  }

  public void disableShield() {
    this.getCooldowns().addCooldown(Items.SHIELD, 100);
    this.stopUsingItem();
    this.level().broadcastEntityEvent(this, (byte)30);
  }

  public void crit(Entity arg) {
  }

  public void magicCrit(Entity arg) {
  }

  public void sweepAttack() {
    double d = (double)(-Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)));
    double e = (double)Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
  }

  public void respawn() {
  }

  @Override
  public void remove(Entity.RemovalReason arg) {
    super.remove(arg);
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

  public void updateTutorialInventoryAction(ItemStack arg, ItemStack arg2, ClickAction arg3) {
  }

  public boolean hasContainerOpen() {
    return this.containerMenu != this.inventoryMenu;
  }

  public Either<BedSleepingProblem, Unit> startSleepInBed(BlockPos arg) {
    this.startSleeping(arg);
    this.sleepCounter = 0;
    return Either.right(Unit.INSTANCE);
  }

  public void stopSleepInBed(boolean bl, boolean bl2) {
    super.stopSleeping();

    this.sleepCounter = bl ? 0 : 100;
  }

  @Override
  public void stopSleeping() {
    this.stopSleepInBed(true, true);
  }

  public boolean isSleepingLongEnough() {
    return this.isSleeping() && this.sleepCounter >= 100;
  }

  public int getSleepTimer() {
    return this.sleepCounter;
  }

  public void displayClientMessage(Component arg, boolean bl) {
  }

  public int awardRecipes(Collection<RecipeHolder<?>> collection) {
    return 0;
  }

  public void triggerRecipeCrafted(RecipeHolder<?> arg, List<ItemStack> list) {
  }

  public void awardRecipesByKey(List<ResourceLocation> list) {
  }

  public int resetRecipes(Collection<RecipeHolder<?>> collection) {
    return 0;
  }

  @Override
  public void jumpFromGround() {
    super.jumpFromGround();
    if (this.isSprinting()) {
      this.causeFoodExhaustion(0.2F);
    } else {
      this.causeFoodExhaustion(0.05F);
    }
  }

  @Override
  public void travel(Vec3 arg) {
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
      super.travel(arg);
      Vec3 lv2 = this.getDeltaMovement();
      this.setDeltaMovement(lv2.x, d * 0.6, lv2.z);
      this.resetFallDistance();
      this.setSharedFlag(7, false);
    } else {
      super.travel(arg);
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

  protected boolean freeAt(BlockPos arg) {
    return !this.level().getBlockState(arg).isSuffocating(this.level(), arg);
  }

  @Override
  public float getSpeed() {
    return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
  }

  @Override
  public boolean causeFallDamage(float f, float g, DamageSource arg) {
    if (this.abilities.mayfly) {
      return false;
    } else {
      boolean bl;
      if (this.ignoreFallDamageFromCurrentImpulse && this.currentImpulseImpactPos != null) {
        double d = this.currentImpulseImpactPos.y;
        this.tryResetCurrentImpulseContext();
        if (d < this.getY()) {
          return false;
        }

        float h = Math.min(f, (float)(d - this.getY()));
        bl = super.causeFallDamage(h, g, arg);
      } else {
        bl = super.causeFallDamage(f, g, arg);
      }

      if (bl) {
        this.resetCurrentImpulseContext();
      }

      return bl;
    }
  }

  public boolean tryToStartFallFlying() {
    if (!this.onGround() && !this.isFallFlying() && !this.isInWater() && !this.hasEffect(EffectType.LEVITATION)) {
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
  protected void playStepSound(BlockPos arg, BlockState arg2) {
    if (this.isInWater()) {
      this.waterSwimSound();
      this.playMuffledStepSound(arg2);
    } else {
      BlockPos lv = this.getPrimaryStepSoundBlockPos(arg);
      if (!arg.equals(lv)) {
        BlockState lv2 = this.level().getBlockState(lv);
        if (lv2.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
          this.playCombinationStepSounds(lv2, arg2);
        } else {
          super.playStepSound(lv, lv2);
        }
      } else {
        super.playStepSound(arg, arg2);
      }
    }
  }

  @Override
  public void makeStuckInBlock(BlockState arg, Vec3 arg2) {
    if (!this.abilities.flying) {
      super.makeStuckInBlock(arg, arg2);
    }

    this.tryResetCurrentImpulseContext();
  }

  public void giveExperiencePoints(int i) {
    this.increaseScore(i);
    this.experienceProgress = this.experienceProgress + (float)i / (float)this.getXpNeededForNextLevel();
    this.totalExperience = Mth.clamp(this.totalExperience + i, 0, Integer.MAX_VALUE);

    while (this.experienceProgress < 0.0F) {
      float f = this.experienceProgress * (float)this.getXpNeededForNextLevel();
      if (this.experienceLevel > 0) {
        this.giveExperienceLevels(-1);
        this.experienceProgress = 1.0F + f / (float)this.getXpNeededForNextLevel();
      } else {
        this.giveExperienceLevels(-1);
        this.experienceProgress = 0.0F;
      }
    }

    while (this.experienceProgress >= 1.0F) {
      this.experienceProgress = (this.experienceProgress - 1.0F) * (float)this.getXpNeededForNextLevel();
      this.giveExperienceLevels(1);
      this.experienceProgress = this.experienceProgress / (float)this.getXpNeededForNextLevel();
    }
  }

  public int getEnchantmentSeed() {
    return this.enchantmentSeed;
  }

  public void onEnchantmentPerformed(ItemStack arg, int i) {
    this.experienceLevel -= i;
    if (this.experienceLevel < 0) {
      this.experienceLevel = 0;
      this.experienceProgress = 0.0F;
      this.totalExperience = 0;
    }

    this.enchantmentSeed = this.random.nextInt();
  }

  public void giveExperienceLevels(int i) {
    this.experienceLevel += i;
    if (this.experienceLevel < 0) {
      this.experienceLevel = 0;
      this.experienceProgress = 0.0F;
      this.totalExperience = 0;
    }

    if (i > 0 && this.experienceLevel % 5 == 0 && (float)this.lastLevelUpTime < (float)this.tickCount - 100.0F) {
      float f = this.experienceLevel > 30 ? 1.0F : (float)this.experienceLevel / 30.0F;
      this.lastLevelUpTime = this.tickCount;
    }
  }

  public int getXpNeededForNextLevel() {
    if (this.experienceLevel >= 30) {
      return 112 + (this.experienceLevel - 30) * 9;
    } else {
      return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
    }
  }

  public void causeFoodExhaustion(float f) {
  }

  public Optional<WardenSpawnTracker> getWardenSpawnTracker() {
    return Optional.empty();
  }

  public FoodData getFoodData() {
    return this.foodData;
  }

  public boolean canEat(boolean bl) {
    return this.abilities.invulnerable || bl || this.foodData.needsFood();
  }

  public boolean isHurt() {
    return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
  }

  public boolean mayBuild() {
    return this.abilities.mayBuild;
  }

  public boolean mayUseItemAt(BlockPos arg, Direction arg2, ItemStack arg3) {
    if (this.abilities.mayBuild) {
      return true;
    } else {
      BlockPos lv = arg.relative(arg2.getOpposite());
      BlockInWorld lv2 = new BlockInWorld(this.level(), lv, false);
      return arg3.canPlaceOnBlockInAdventureMode(lv2);
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
  public ItemStack getItemBySlot(EquipmentSlot arg) {
    if (arg == EquipmentSlot.MAINHAND) {
      return this.inventory.getSelected();
    } else if (arg == EquipmentSlot.OFFHAND) {
      return this.inventory.offhand.get(0);
    } else {
      return arg.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ? this.inventory.armor.get(arg.getIndex()) : ItemStack.EMPTY;
    }
  }

  @Override
  protected boolean doesEmitEquipEvent(EquipmentSlot arg) {
    return arg.getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
  }

  @Override
  public void setItemSlot(EquipmentSlot arg, ItemStack arg2) {
    this.verifyEquippedItem(arg2);
    if (arg == EquipmentSlot.MAINHAND) {
      this.onEquipItem(arg, this.inventory.items.set(this.inventory.selected, arg2), arg2);
    } else if (arg == EquipmentSlot.OFFHAND) {
      this.onEquipItem(arg, this.inventory.offhand.set(0, arg2), arg2);
    } else if (arg.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
      this.onEquipItem(arg, this.inventory.armor.set(arg.getIndex(), arg2), arg2);
    }
  }

  public boolean addItem(ItemStack arg) {
    return this.inventory.add(arg);
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
  public boolean canUseSlot(EquipmentSlot arg) {
    return arg != EquipmentSlot.BODY;
  }

  public boolean setEntityOnShoulder(CompoundTag arg) {
    if (this.isPassenger() || !this.onGround() || this.isInWater() || this.isInPowderSnow) {
      return false;
    } else if (this.getShoulderEntityLeft().isEmpty()) {
      this.setShoulderEntityLeft(arg);
      this.timeEntitySatOnShoulder = this.level().getGameTime();
      return true;
    } else if (this.getShoulderEntityRight().isEmpty()) {
      this.setShoulderEntityRight(arg);
      this.timeEntitySatOnShoulder = this.level().getGameTime();
      return true;
    } else {
      return false;
    }
  }

  protected void removeEntitiesOnShoulder() {
    if (this.timeEntitySatOnShoulder + 20L < this.level().getGameTime()) {
      this.respawnEntityOnShoulder(this.getShoulderEntityLeft());
      this.setShoulderEntityLeft(new CompoundTag());
      this.respawnEntityOnShoulder(this.getShoulderEntityRight());
      this.setShoulderEntityRight(new CompoundTag());
    }
  }

  private void respawnEntityOnShoulder(CompoundTag arg) {
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

  public Scoreboard getScoreboard() {
    return this.level().getScoreboard();
  }

  @Override
  public Component getDisplayName() {
    MutableComponent lv = PlayerTeam.formatNameForTeam(this.getTeam(), this.getName());
    return this.decorateDisplayNameComponent(lv);
  }

  private MutableComponent decorateDisplayNameComponent(MutableComponent arg) {
    String string = this.getGameProfile().getName();
    return arg.withStyle(
      argx -> argx.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + string + " "))
        .withHoverEvent(this.createHoverEvent())
        .withInsertion(string)
    );
  }

  @Override
  public String getScoreboardName() {
    return this.getGameProfile().getName();
  }

  @Override
  protected void internalSetAbsorptionAmount(float f) {
    this.getEntityData().set(DATA_PLAYER_ABSORPTION_ID, f);
  }

  @Override
  public float getAbsorptionAmount() {
    return this.getEntityData().get(DATA_PLAYER_ABSORPTION_ID);
  }

  public boolean isModelPartShown(PlayerModelPart arg) {
    return (this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) & arg.getMask()) == arg.getMask();
  }

  @Override
  public SlotAccess getSlot(int i) {
    if (i == 499) {
      return new SlotAccess() {
        @Override
        public ItemStack get() {
          return Player.this.containerMenu.getCarried();
        }

        @Override
        public boolean set(ItemStack arg) {
          Player.this.containerMenu.setCarried(arg);
          return true;
        }
      };
    } else {
      final int j = i - 500;
      if (j >= 0 && j < 4) {
        return new SlotAccess() {
          @Override
          public ItemStack get() {
            return Player.this.inventoryMenu.getCraftSlots().getItem(j);
          }

          @Override
          public boolean set(ItemStack arg) {
            Player.this.inventoryMenu.getCraftSlots().setItem(j, arg);
            Player.this.inventoryMenu.slotsChanged(Player.this.inventory);
            return true;
          }
        };
      } else if (i >= 0 && i < this.inventory.items.size()) {
        return SlotAccess.forContainer(this.inventory, i);
      } else {
        int k = i - 200;
        return k >= 0 && k < this.enderChestInventory.getContainerSize() ? SlotAccess.forContainer(this.enderChestInventory, k) : super.getSlot(i);
      }
    }
  }

  public boolean isReducedDebugInfo() {
    return this.reducedDebugInfo;
  }

  public void setReducedDebugInfo(boolean bl) {
    this.reducedDebugInfo = bl;
  }

  @Override
  public void setRemainingFireTicks(int i) {
    super.setRemainingFireTicks(this.abilities.invulnerable ? Math.min(i, 1) : i);
  }

  @Override
  public HumanoidArm getMainArm() {
    return this.entityData.get(DATA_PLAYER_MAIN_HAND) == 0 ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
  }

  public void setMainArm(HumanoidArm arg) {
    this.entityData.set(DATA_PLAYER_MAIN_HAND, (byte)(arg == HumanoidArm.LEFT ? 0 : 1));
  }

  public CompoundTag getShoulderEntityLeft() {
    return this.entityData.get(DATA_SHOULDER_LEFT);
  }

  protected void setShoulderEntityLeft(CompoundTag arg) {
    this.entityData.set(DATA_SHOULDER_LEFT, arg);
  }

  public CompoundTag getShoulderEntityRight() {
    return this.entityData.get(DATA_SHOULDER_RIGHT);
  }

  protected void setShoulderEntityRight(CompoundTag arg) {
    this.entityData.set(DATA_SHOULDER_RIGHT, arg);
  }

  public float getCurrentItemAttackStrengthDelay() {
    return (float)(1.0 / this.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0);
  }

  public float getAttackStrengthScale(float f) {
    return Mth.clamp(((float)this.attackStrengthTicker + f) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
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
    return (float)this.getAttributeValue(Attributes.LUCK);
  }

  public boolean canUseGameMasterBlocks() {
    return this.abilities.instabuild && this.getPermissionLevel() >= 2;
  }

  @Override
  public boolean canTakeItem(ItemStack arg) {
    EquipmentSlot lv = this.getEquipmentSlotForItem(arg);
    return this.getItemBySlot(lv).isEmpty();
  }

  @Override
  public EntityDimensions getDefaultDimensions(Pose arg) {
    return POSES.getOrDefault(arg, STANDING_DIMENSIONS);
  }

  @Override
  public ImmutableList<Pose> getDismountPoses() {
    return ImmutableList.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING);
  }

  @Override
  public ItemStack getProjectile(ItemStack arg) {
    if (!(arg.getItem() instanceof ProjectileWeaponItem)) {
      return ItemStack.EMPTY;
    } else {
      Predicate<ItemStack> predicate = ((ProjectileWeaponItem)arg.getItem()).getSupportedHeldProjectiles();
      ItemStack lv = ProjectileWeaponItem.getHeldProjectile(this, predicate);
      if (!lv.isEmpty()) {
        return lv;
      } else {
        predicate = ((ProjectileWeaponItem)arg.getItem()).getAllSupportedProjectiles();

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
  public ItemStack eat(Level arg, ItemStack arg2, FoodProperties arg3) {
    this.getFoodData().eat(arg3);

    ItemStack lv = super.eat(arg, arg2, arg3);
    Optional<ItemStack> optional = arg3.usingConvertsTo();
    if (optional.isPresent() && !this.hasInfiniteMaterials()) {
      if (lv.isEmpty()) {
        return optional.get().copy();
      }
    }

    return lv;
  }

  @Override
  public Vec3 getRopeHoldPosition(float f) {
    double d = 0.22 * (this.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0);
    float g = Mth.lerp(f * 0.5F, this.getXRot(), this.xRotO) * (float) (Math.PI / 180.0);
    float h = Mth.lerp(f, this.yBodyRotO, this.yBodyRot) * (float) (Math.PI / 180.0);
    if (this.isFallFlying() || this.isAutoSpinAttack()) {
      Vec3 lv = this.getViewVector(f);
      Vec3 lv2 = this.getDeltaMovement();
      double e = lv2.horizontalDistanceSqr();
      double i = lv.horizontalDistanceSqr();
      float l;
      if (e > 0.0 && i > 0.0) {
        double j = (lv2.x * lv.x + lv2.z * lv.z) / Math.sqrt(e * i);
        double k = lv2.x * lv.z - lv2.z * lv.x;
        l = (float)(Math.signum(k) * Math.acos(j));
      } else {
        l = 0.0F;
      }

      return this.getPosition(f).add(new Vec3(d, -0.11, 0.85).zRot(-l).xRot(-g).yRot(-h));
    } else if (this.isVisuallySwimming()) {
      return this.getPosition(f).add(new Vec3(d, 0.2, -0.15).xRot(-g).yRot(-h));
    } else {
      double m = this.getBoundingBox().getYsize() - 1.0;
      double e = this.isCrouching() ? -0.2 : 0.07;
      return this.getPosition(f).add(new Vec3(d, m, e).yRot(-h));
    }
  }

  @Override
  public boolean isAlwaysTicking() {
    return true;
  }

  public boolean isScoping() {
    return this.isUsingItem() && this.getUseItem().is(Items.SPYGLASS);
  }

  @Override
  public boolean shouldBeSaved() {
    return false;
  }

  public Optional<GlobalPos> getLastDeathLocation() {
    return this.lastDeathLocation;
  }

  public void setLastDeathLocation(Optional<GlobalPos> optional) {
    this.lastDeathLocation = optional;
  }

  @Override
  public float getHurtDir() {
    return this.hurtDir;
  }

  @Override
  public void animateHurt(float f) {
    super.animateHurt(f);
    this.hurtDir = f;
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

  public boolean canInteractWithEntity(Entity arg, double d) {
    return arg.isRemoved() ? false : this.canInteractWithEntity(arg.getBoundingBox(), d);
  }

  public boolean canInteractWithEntity(AABB arg, double d) {
    double e = this.entityInteractionRange() + d;
    return arg.distanceToSqr(this.getEyePosition()) < e * e;
  }

  public boolean canInteractWithBlock(BlockPos arg, double d) {
    double e = this.blockInteractionRange() + d;
    return new AABB(arg).distanceToSqr(this.getEyePosition()) < e * e;
  }

  public void setIgnoreFallDamageFromCurrentImpulse(boolean bl) {
    this.ignoreFallDamageFromCurrentImpulse = bl;
    if (bl) {
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
