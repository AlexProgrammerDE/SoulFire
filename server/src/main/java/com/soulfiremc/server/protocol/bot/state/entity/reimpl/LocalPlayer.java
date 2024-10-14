package com.soulfiremc.server.protocol.bot.state.entity.reimpl;

import com.soulfiremc.server.data.EffectType;
import com.soulfiremc.server.data.FluidTags;
import com.soulfiremc.server.data.ItemType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class LocalPlayer extends AbstractClientPlayer {
  private static final int POSITION_REMINDER_INTERVAL = 20;
  private static final int WATER_VISION_MAX_TIME = 600;
  private static final int WATER_VISION_QUICK_TIME = 100;
  private static final float WATER_VISION_QUICK_PERCENT = 0.6F;
  private static final double SUFFOCATING_COLLISION_CHECK_SCALE = 0.35;
  private static final double MINOR_COLLISION_ANGLE_THRESHOLD_RADIAN = 0.13962634F;
  private int permissionLevel = 0;
  private double xLast;
  private double yLast1;
  private double zLast;
  private float yRotLast;
  private float xRotLast;
  private boolean lastOnGround;
  private boolean crouching;
  private boolean wasShiftKeyDown;
  private boolean wasSprinting;
  private int positionReminder;
  private boolean flashOnSetHealth;
  public Input input;
  protected final Minecraft minecraft;
  protected int sprintTriggerTime;
  public float yBob;
  public float xBob;
  public float yBobO;
  public float xBobO;
  private int jumpRidingTicks;
  private float jumpRidingScale;
  public float spinningEffectIntensity;
  public float oSpinningEffectIntensity;
  private boolean startedUsingItem;
  @Nullable
  private InteractionHand usingItemHand;
  private boolean handsBusy;
  private boolean autoJumpEnabled = true;
  private int autoJumpTime;
  private boolean wasFallFlying;
  private int waterVisionTime;
  private final boolean showDeathScreen = true;
  private boolean doLimitedCrafting = false;

  public LocalPlayer(Minecraft arg, ClientLevel arg2, ClientPacketListener arg3, StatsCounter arg4, ClientRecipeBook arg5, boolean bl, boolean bl2) {
    super(arg2, arg3.getLocalGameProfile());
    this.minecraft = arg;
    this.connection = arg3;
    this.stats = arg4;
    this.recipeBook = arg5;
    this.wasShiftKeyDown = bl;
    this.wasSprinting = bl2;
  }

  @Override
  public boolean hurt(DamageSource source, float amount) {
    return false;
  }

  @Override
  public void heal(float healAmount) {
  }

  @Override
  public void removeVehicle() {
    super.removeVehicle();
    this.handsBusy = false;
  }

  @Override
  public float getViewXRot(float partialTicks) {
    return this.getXRot();
  }

  @Override
  public float getViewYRot(float partialTick) {
    return this.isPassenger() ? super.getViewYRot(partialTick) : this.getYRot();
  }

  @Override
  public void tick() {
    if (this.level().hasChunkAt(this.getBlockX(), this.getBlockZ())) {
      super.tick();
      if (this.isPassenger()) {
        this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround()));
        this.connection.send(new ServerboundPlayerInputPacket(this.xxa, this.zza, this.input.jumping, this.input.shiftKeyDown));
        Entity lv = this.getRootVehicle();
        if (lv != this && lv.isControlledByLocalInstance()) {
          this.connection.send(new ServerboundMoveVehiclePacket(lv));
          this.sendIsSprintingIfNeeded();
        }
      } else {
        this.sendPosition();
      }

      for (AmbientSoundHandler lv2 : this.ambientSoundHandlers) {
        lv2.tick();
      }
    }
  }

  private void sendPosition() {
    this.sendIsSprintingIfNeeded();
    boolean bl = this.isShiftKeyDown();
    if (bl != this.wasShiftKeyDown) {
      ServerboundPlayerCommandPacket.Action lv = bl
        ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY
        : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY;
      this.connection.send(new ServerboundPlayerCommandPacket(this, lv));
      this.wasShiftKeyDown = bl;
    }

    if (this.isControlledCamera()) {
      double d = this.getX() - this.xLast;
      double e = this.getY() - this.yLast1;
      double f = this.getZ() - this.zLast;
      double g = this.getYRot() - this.yRotLast;
      double h = this.getXRot() - this.xRotLast;
      this.positionReminder++;
      boolean bl2 = Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4) || this.positionReminder >= 20;
      boolean bl3 = g != 0.0 || h != 0.0;
      if (this.isPassenger()) {
        Vec3 lv2 = this.getDeltaMovement();
        this.connection.send(new ServerboundMovePlayerPacket.PosRot(lv2.x, -999.0, lv2.z, this.getYRot(), this.getXRot(), this.onGround()));
        bl2 = false;
      } else if (bl2 && bl3) {
        this.connection
          .send(new ServerboundMovePlayerPacket.PosRot(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), this.onGround()));
      } else if (bl2) {
        this.connection.send(new ServerboundMovePlayerPacket.Pos(this.getX(), this.getY(), this.getZ(), this.onGround()));
      } else if (bl3) {
        this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround()));
      } else if (this.lastOnGround != this.onGround()) {
        this.connection.send(new ServerboundMovePlayerPacket.StatusOnly(this.onGround()));
      }

      if (bl2) {
        this.xLast = this.getX();
        this.yLast1 = this.getY();
        this.zLast = this.getZ();
        this.positionReminder = 0;
      }

      if (bl3) {
        this.yRotLast = this.getYRot();
        this.xRotLast = this.getXRot();
      }

      this.lastOnGround = this.onGround();
      this.autoJumpEnabled = this.minecraft.options.autoJump().get();
    }
  }

  private void sendIsSprintingIfNeeded() {
    boolean bl = this.isSprinting();
    if (bl != this.wasSprinting) {
      ServerboundPlayerCommandPacket.Action lv = bl
        ? ServerboundPlayerCommandPacket.Action.START_SPRINTING
        : ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
      this.connection.send(new ServerboundPlayerCommandPacket(this, lv));
      this.wasSprinting = bl;
    }
  }

  public boolean drop(boolean fullStack) {
    ServerboundPlayerActionPacket.Action lv = fullStack
      ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
      : ServerboundPlayerActionPacket.Action.DROP_ITEM;
    ItemStack lv2 = this.getInventory().removeFromSelected(fullStack);
    this.connection.send(new ServerboundPlayerActionPacket(lv, BlockPos.ZERO, Direction.DOWN));
    return !lv2.isEmpty();
  }

  @Override
  public void swing(InteractionHand hand) {
    super.swing(hand);
    this.connection.send(new ServerboundSwingPacket(hand));
  }

  @Override
  public void respawn() {
    this.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
    KeyMapping.resetToggleKeys();
  }

  @Override
  protected void actuallyHurt(DamageSource damageSource, float damageAmount) {
    if (!this.isInvulnerableTo(damageSource)) {
      this.setHealth(this.getHealth() - damageAmount);
    }
  }

  @Override
  public void closeContainer() {
    this.connection.send(new ServerboundContainerClosePacket(this.containerMenu.containerId));
    this.clientSideCloseContainer();
  }

  public void clientSideCloseContainer() {
    super.closeContainer();
    this.minecraft.setScreen(null);
  }

  public void hurtTo(float health) {
    if (this.flashOnSetHealth) {
      float g = this.getHealth() - health;
      if (g <= 0.0F) {
        this.setHealth(health);
        if (g < 0.0F) {
          this.invulnerableTime = 10;
        }
      } else {
        this.lastHurt = g;
        this.invulnerableTime = 20;
        this.setHealth(health);
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
      }
    } else {
      this.setHealth(health);
      this.flashOnSetHealth = true;
    }
  }

  @Override
  public void onUpdateAbilities() {
    this.connection.send(new ServerboundPlayerAbilitiesPacket(this.getAbilities()));
  }

  @Override
  public boolean isLocalPlayer() {
    return true;
  }

  @Override
  public boolean isSuppressingSlidingDownLadder() {
    return !this.getAbilities().flying && super.isSuppressingSlidingDownLadder();
  }

  @Override
  public boolean canSpawnSprintParticle() {
    return !this.getAbilities().flying && super.canSpawnSprintParticle();
  }

  protected void sendRidingJump() {
    this.connection
      .send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP, Mth.floor(this.getJumpRidingScale() * 100.0F)));
  }

  public void sendOpenInventory() {
    this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
  }

  public void removeRecipeHighlight(RecipeHolder<?> recipe) {
    if (this.recipeBook.willHighlight(recipe)) {
      this.recipeBook.removeHighlight(recipe);
      this.connection.send(new ServerboundRecipeBookSeenRecipePacket(recipe));
    }
  }

  @Override
  protected int getPermissionLevel() {
    return this.permissionLevel;
  }

  public void setPermissionLevel(int permissionLevel) {
    this.permissionLevel = permissionLevel;
  }

  @Override
  public void displayClientMessage(Component chatComponent, boolean actionBar) {
    this.minecraft.getChatListener().handleSystemMessage(chatComponent, actionBar);
  }

  private void moveTowardsClosestSpace(double x, double z) {
    BlockPos lv = BlockPos.containing(x, this.getY(), z);
    if (this.suffocatesAt(lv)) {
      double f = x - (double)lv.getX();
      double g = z - (double)lv.getZ();
      Direction lv2 = null;
      double h = Double.MAX_VALUE;
      Direction[] lvs = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

      for (Direction lv3 : lvs) {
        double i = lv3.getAxis().choose(f, 0.0, g);
        double j = lv3.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - i : i;
        if (j < h && !this.suffocatesAt(lv.relative(lv3))) {
          h = j;
          lv2 = lv3;
        }
      }

      if (lv2 != null) {
        Vec3 lv4 = this.getDeltaMovement();
        if (lv2.getAxis() == Direction.Axis.X) {
          this.setDeltaMovement(0.1 * (double)lv2.getStepX(), lv4.y, lv4.z);
        } else {
          this.setDeltaMovement(lv4.x, lv4.y, 0.1 * (double)lv2.getStepZ());
        }
      }
    }
  }

  private boolean suffocatesAt(BlockPos pos) {
    AABB lv = this.getBoundingBox();
    AABB lv2 = new AABB((double)pos.getX(), lv.minY, (double)pos.getZ(), (double)pos.getX() + 1.0, lv.maxY, (double)pos.getZ() + 1.0).deflate(1.0E-7);
    return this.level().collidesWithSuffocatingBlock(this, lv2);
  }

  public void setExperienceValues(float currentXP, int maxXP, int level) {
    this.experienceProgress = currentXP;
    this.totalExperience = maxXP;
    this.experienceLevel = level;
  }

  @Override
  public void handleEntityEvent(byte id) {
    if (id >= 24 && id <= 28) {
      this.setPermissionLevel(id - 24);
    } else {
      super.handleEntityEvent(id);
    }
  }

  public void setDoLimitedCrafting(boolean doLimitedCrafting) {
    this.doLimitedCrafting = doLimitedCrafting;
  }

  public boolean getDoLimitedCrafting() {
    return this.doLimitedCrafting;
  }

  @Override
  public boolean isEffectiveAi() {
    return true;
  }

  @Override
  public void startUsingItem(InteractionHand hand) {
    ItemStack lv = this.getItemInHand(hand);
    if (!lv.isEmpty() && !this.isUsingItem()) {
      super.startUsingItem(hand);
      this.startedUsingItem = true;
      this.usingItemHand = hand;
    }
  }

  @Override
  public boolean isUsingItem() {
    return this.startedUsingItem;
  }

  @Override
  public void stopUsingItem() {
    super.stopUsingItem();
    this.startedUsingItem = false;
  }

  @Override
  public InteractionHand getUsedItemHand() {
    return Objects.requireNonNullElse(this.usingItemHand, InteractionHand.MAIN_HAND);
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> dataAccessor) {
    super.onSyncedDataUpdated(dataAccessor);
    if (DATA_LIVING_ENTITY_FLAGS.equals(dataAccessor)) {
      boolean bl = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
      InteractionHand lv = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      if (bl && !this.startedUsingItem) {
        this.startUsingItem(lv);
      } else if (!bl && this.startedUsingItem) {
        this.stopUsingItem();
      }
    }

    if (DATA_SHARED_FLAGS_ID.equals(dataAccessor) && this.isFallFlying() && !this.wasFallFlying) {
      this.minecraft.getSoundManager().play(new ElytraOnPlayerSoundInstance(this));
    }
  }

  @Nullable
  public PlayerRideableJumping jumpableVehicle() {
    if (this.getControlledVehicle() instanceof PlayerRideableJumping lv && lv.canJump()) {
      return lv;
    }

    return null;
  }

  public float getJumpRidingScale() {
    return this.jumpRidingScale;
  }

  @Override
  public boolean isTextFilteringEnabled() {
    return this.minecraft.isTextFilteringEnabled();
  }

  @Override
  public void openTextEdit(SignBlockEntity signEntity, boolean isFrontText) {
    if (signEntity instanceof HangingSignBlockEntity lv) {
      this.minecraft.setScreen(new HangingSignEditScreen(lv, isFrontText, this.minecraft.isTextFilteringEnabled()));
    } else {
      this.minecraft.setScreen(new SignEditScreen(signEntity, isFrontText, this.minecraft.isTextFilteringEnabled()));
    }
  }

  @Override
  public void openMinecartCommandBlock(BaseCommandBlock commandEntity) {
    this.minecraft.setScreen(new MinecartCommandBlockEditScreen(commandEntity));
  }

  @Override
  public void openCommandBlock(CommandBlockEntity commandBlockEntity) {
    this.minecraft.setScreen(new CommandBlockEditScreen(commandBlockEntity));
  }

  @Override
  public void openStructureBlock(StructureBlockEntity structureEntity) {
    this.minecraft.setScreen(new StructureBlockEditScreen(structureEntity));
  }

  @Override
  public void openJigsawBlock(JigsawBlockEntity jigsawBlockEntity) {
    this.minecraft.setScreen(new JigsawBlockEditScreen(jigsawBlockEntity));
  }

  @Override
  public void openItemGui(ItemStack stack, InteractionHand hand) {
    if (stack.is(ItemType.WRITABLE_BOOK)) {
      this.minecraft.setScreen(new BookEditScreen(this, stack, hand));
    }
  }

  @Override
  public boolean isShiftKeyDown() {
    return this.input != null && this.input.shiftKeyDown;
  }

  @Override
  public boolean isCrouching() {
    return this.crouching;
  }

  public boolean isMovingSlowly() {
    return this.isCrouching() || this.isVisuallyCrawling();
  }

  @Override
  public void serverAiStep() {
    super.serverAiStep();
    if (this.isControlledCamera()) {
      this.xxa = this.input.leftImpulse;
      this.zza = this.input.forwardImpulse;
      this.jumping = this.input.jumping;
      this.yBobO = this.yBob;
      this.xBobO = this.xBob;
      this.xBob = this.xBob + (this.getXRot() - this.xBob) * 0.5F;
      this.yBob = this.yBob + (this.getYRot() - this.yBob) * 0.5F;
    }
  }

  protected boolean isControlledCamera() {
    return this.minecraft.getCameraEntity() == this;
  }

  public void resetPos() {
    this.setPose(Pose.STANDING);
    if (this.level() != null) {
      for (double d = this.getY(); d > (double)this.level().getMinBuildHeight() && d < (double)this.level().getMaxBuildHeight(); d++) {
        this.setPos(this.getX(), d, this.getZ());
        if (this.level().noCollision(this)) {
          break;
        }
      }

      this.setDeltaMovement(Vec3.ZERO);
      this.setXRot(0.0F);
    }

    this.setHealth(this.getMaxHealth());
    this.deathTime = 0;
  }

  @Override
  public void aiStep() {
    if (this.sprintTriggerTime > 0) {
      this.sprintTriggerTime--;
    }

    if (!(this.minecraft.screen instanceof ReceivingLevelScreen)) {
      this.handleConfusionTransitionEffect(this.getActivePortalLocalTransition() == Portal.Transition.CONFUSION);
    }

    boolean bl = this.input.jumping;
    boolean bl2 = this.input.shiftKeyDown;
    boolean bl3 = this.hasEnoughImpulseToStartSprinting();
    Abilities lv = this.getAbilities();
    this.crouching = !lv.flying
      && !this.isSwimming()
      && !this.isPassenger()
      && this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)
      && (this.isShiftKeyDown() || !this.isSleeping() && !this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING));
    float f = (float) this.getAttributeValue(AttributeType.SNEAKING_SPEED);
    this.input.tick(this.isMovingSlowly(), f);
    this.minecraft.getTutorial().onInput(this.input);
    if (this.isUsingItem() && !this.isPassenger()) {
      this.input.leftImpulse *= 0.2F;
      this.input.forwardImpulse *= 0.2F;
      this.sprintTriggerTime = 0;
    }

    boolean bl4 = false;
    if (this.autoJumpTime > 0) {
      this.autoJumpTime--;
      bl4 = true;
      this.input.jumping = true;
    }

    if (!this.noPhysics) {
      this.moveTowardsClosestSpace(this.getX() - (double)this.getBbWidth() * 0.35, this.getZ() + (double)this.getBbWidth() * 0.35);
      this.moveTowardsClosestSpace(this.getX() - (double)this.getBbWidth() * 0.35, this.getZ() - (double)this.getBbWidth() * 0.35);
      this.moveTowardsClosestSpace(this.getX() + (double)this.getBbWidth() * 0.35, this.getZ() - (double)this.getBbWidth() * 0.35);
      this.moveTowardsClosestSpace(this.getX() + (double)this.getBbWidth() * 0.35, this.getZ() + (double)this.getBbWidth() * 0.35);
    }

    if (bl2) {
      this.sprintTriggerTime = 0;
    }

    boolean bl5 = this.canStartSprinting();
    boolean bl6 = this.isPassenger() ? this.getVehicle().onGround() : this.onGround();
    boolean bl7 = !bl2 && !bl3;
    if ((bl6 || this.isUnderWater()) && bl7 && bl5) {
      if (this.sprintTriggerTime <= 0 && !this.minecraft.options.keySprint.isDown()) {
        this.sprintTriggerTime = 7;
      } else {
        this.setSprinting(true);
      }
    }

    if ((!this.isInWater() || this.isUnderWater()) && bl5 && this.minecraft.options.keySprint.isDown()) {
      this.setSprinting(true);
    }

    if (this.isSprinting()) {
      boolean bl8 = !this.input.hasForwardImpulse() || !this.hasEnoughFoodToStartSprinting();
      boolean bl9 = bl8 || this.horizontalCollision && !this.minorHorizontalCollision || this.isInWater() && !this.isUnderWater();
      if (this.isSwimming()) {
        if (!this.onGround() && !this.input.shiftKeyDown && bl8 || !this.isInWater()) {
          this.setSprinting(false);
        }
      } else if (bl9) {
        this.setSprinting(false);
      }
    }

    boolean bl8 = false;
    if (lv.mayfly) {
      if (this.minecraft.gameMode.isAlwaysFlying()) {
        if (!lv.flying) {
          lv.flying = true;
          bl8 = true;
          this.onUpdateAbilities();
        }
      } else if (!bl && this.input.jumping && !bl4) {
        if (this.jumpTriggerTime == 0) {
          this.jumpTriggerTime = 7;
        } else if (!this.isSwimming()) {
          lv.flying = !lv.flying;
          if (lv.flying && this.onGround()) {
            this.jumpFromGround();
          }

          bl8 = true;
          this.onUpdateAbilities();
          this.jumpTriggerTime = 0;
        }
      }
    }

    if (this.input.jumping && !bl8 && !bl && !lv.flying && !this.isPassenger() && !this.onClimbable()) {
      ItemStack lv2 = this.getItemBySlot(EquipmentSlot.CHEST);
      if (lv2.is(ItemType.ELYTRA) && ElytraItem.isFlyEnabled(lv2) && this.tryToStartFallFlying()) {
        this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
      }
    }

    this.wasFallFlying = this.isFallFlying();
    if (this.isInWater() && this.input.shiftKeyDown && this.isAffectedByFluids()) {
      this.goDownInWater();
    }

    if (this.isEyeInFluid(FluidTags.WATER)) {
      int i = this.isSpectator() ? 10 : 1;
      this.waterVisionTime = Mth.clamp(this.waterVisionTime + i, 0, 600);
    } else if (this.waterVisionTime > 0) {
      this.isEyeInFluid(FluidTags.WATER);
      this.waterVisionTime = Mth.clamp(this.waterVisionTime - 10, 0, 600);
    }

    if (lv.flying && this.isControlledCamera()) {
      int i = 0;
      if (this.input.shiftKeyDown) {
        i--;
      }

      if (this.input.jumping) {
        i++;
      }

      if (i != 0) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, (double)((float)i * lv.getFlyingSpeed() * 3.0F), 0.0));
      }
    }

    PlayerRideableJumping lv3 = this.jumpableVehicle();
    if (lv3 != null && lv3.getJumpCooldown() == 0) {
      if (this.jumpRidingTicks < 0) {
        this.jumpRidingTicks++;
        if (this.jumpRidingTicks == 0) {
          this.jumpRidingScale = 0.0F;
        }
      }

      if (bl && !this.input.jumping) {
        this.jumpRidingTicks = -10;
        lv3.onPlayerJump(Mth.floor(this.getJumpRidingScale() * 100.0F));
        this.sendRidingJump();
      } else if (!bl && this.input.jumping) {
        this.jumpRidingTicks = 0;
        this.jumpRidingScale = 0.0F;
      } else if (bl) {
        this.jumpRidingTicks++;
        if (this.jumpRidingTicks < 10) {
          this.jumpRidingScale = (float)this.jumpRidingTicks * 0.1F;
        } else {
          this.jumpRidingScale = 0.8F + 2.0F / (float)(this.jumpRidingTicks - 9) * 0.1F;
        }
      }
    } else {
      this.jumpRidingScale = 0.0F;
    }

    super.aiStep();
    if (this.onGround() && lv.flying && !this.minecraft.gameMode.isAlwaysFlying()) {
      lv.flying = false;
      this.onUpdateAbilities();
    }
  }

  public Portal.Transition getActivePortalLocalTransition() {
    return this.portalProcess == null ? Portal.Transition.NONE : this.portalProcess.getPortalLocalTransition();
  }

  @Override
  protected void tickDeath() {
    this.deathTime++;
    if (this.deathTime == 20) {
      this.remove(Entity.RemovalReason.KILLED);
    }
  }

  private void handleConfusionTransitionEffect(boolean useConfusion) {
    this.oSpinningEffectIntensity = this.spinningEffectIntensity;
    float f = 0.0F;
    if (useConfusion && this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
      if (this.minecraft.screen != null
        && !this.minecraft.screen.isPauseScreen()
        && !(this.minecraft.screen instanceof DeathScreen)
        && !(this.minecraft.screen instanceof WinScreen)) {
        if (this.minecraft.screen instanceof AbstractContainerScreen) {
          this.closeContainer();
        }

        this.minecraft.setScreen(null);
      }

      if (this.spinningEffectIntensity == 0.0F) {
        this.minecraft
          .getSoundManager()
          .play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRIGGER, this.random.nextFloat() * 0.4F + 0.8F, 0.25F));
      }

      f = 0.0125F;
      this.portalProcess.setAsInsidePortalThisTick(false);
    } else if (this.hasEffect(EffectType.CONFUSION) && !this.getEffect(EffectType.CONFUSION).endsWithin(60)) {
      f = 0.006666667F;
    } else if (this.spinningEffectIntensity > 0.0F) {
      f = -0.05F;
    }

    this.spinningEffectIntensity = Mth.clamp(this.spinningEffectIntensity + f, 0.0F, 1.0F);
  }

  @Override
  public void rideTick() {
    super.rideTick();
    this.handsBusy = false;
    if (this.getControlledVehicle() instanceof Boat lv) {
      lv.setInput(this.input.left, this.input.right, this.input.up, this.input.down);
      this.handsBusy = this.handsBusy | (this.input.left || this.input.right || this.input.up || this.input.down);
    }
  }

  public boolean isHandsBusy() {
    return this.handsBusy;
  }

  @Nullable
  @Override
  public MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> effect) {
    if (effect.is(EffectType.CONFUSION)) {
      this.oSpinningEffectIntensity = 0.0F;
      this.spinningEffectIntensity = 0.0F;
    }

    return super.removeEffectNoUpdate(effect);
  }

  @Override
  public void move(MoverType type, Vec3 pos) {
    double d = this.getX();
    double e = this.getZ();
    super.move(type, pos);
  }

  @Override
  protected boolean isHorizontalCollisionMinor(Vec3 deltaMovement) {
    float f = this.getYRot() * (float) (Math.PI / 180.0);
    double d = (double)Mth.sin(f);
    double e = (double)Mth.cos(f);
    double g = (double)this.xxa * e - (double)this.zza * d;
    double h = (double)this.zza * e + (double)this.xxa * d;
    double i = Mth.square(g) + Mth.square(h);
    double j = Mth.square(deltaMovement.x) + Mth.square(deltaMovement.z);
    if (!(i < 1.0E-5F) && !(j < 1.0E-5F)) {
      double k = g * deltaMovement.x + h * deltaMovement.z;
      double l = Math.acos(k / Math.sqrt(i * j));
      return l < 0.13962634F;
    } else {
      return false;
    }
  }

  private boolean canStartSprinting() {
    return !this.isSprinting()
      && this.hasEnoughImpulseToStartSprinting()
      && this.hasEnoughFoodToStartSprinting()
      && !this.isUsingItem()
            && !this.hasEffect(EffectType.BLINDNESS)
      && (!this.isPassenger() || this.vehicleCanSprint(this.getVehicle()))
      && !this.isFallFlying();
  }

  private boolean vehicleCanSprint(Entity vehicle) {
    return vehicle.canSprint() && vehicle.isControlledByLocalInstance();
  }

  private boolean hasEnoughImpulseToStartSprinting() {
    double d = 0.8;
    return this.isUnderWater() ? this.input.hasForwardImpulse() : (double)this.input.forwardImpulse >= 0.8;
  }

  private boolean hasEnoughFoodToStartSprinting() {
    return this.isPassenger() || (float)this.getFoodData().getFoodLevel() > 6.0F || this.getAbilities().mayfly;
  }

  public float getWaterVision() {
    if (!this.isEyeInFluid(FluidTags.WATER)) {
      return 0.0F;
    } else {
      float f = 600.0F;
      float g = 100.0F;
      if ((float)this.waterVisionTime >= 600.0F) {
        return 1.0F;
      } else {
        float h = Mth.clamp((float)this.waterVisionTime / 100.0F, 0.0F, 1.0F);
        float i = (float)this.waterVisionTime < 100.0F ? 0.0F : Mth.clamp(((float)this.waterVisionTime - 100.0F) / 500.0F, 0.0F, 1.0F);
        return h * 0.6F + i * 0.39999998F;
      }
    }
  }

  public void onGameModeChanged(GameType gameMode) {
    if (gameMode == GameType.SPECTATOR) {
      this.setDeltaMovement(this.getDeltaMovement().with(Direction.Axis.Y, 0.0));
    }
  }

  @Override
  public boolean isUnderWater() {
    return this.wasUnderwater;
  }

  @Override
  public float getVisualRotationYInDegrees() {
    return this.getYRot();
  }
}
