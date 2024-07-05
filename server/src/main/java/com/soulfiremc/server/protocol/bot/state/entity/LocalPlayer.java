package com.soulfiremc.server.protocol.bot.state.entity;

import com.google.common.collect.Lists;
import com.soulfiremc.server.data.EffectType;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import net.kyori.adventure.text.Component;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.client.gui.screens.inventory.JigsawBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.MinecartCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import org.jetbrains.annotations.Nullable;

public class LocalPlayer extends AbstractClientPlayer {
  private static final int POSITION_REMINDER_INTERVAL = 20;
  private static final int WATER_VISION_MAX_TIME = 600;
  private static final int WATER_VISION_QUICK_TIME = 100;
  private static final float WATER_VISION_QUICK_PERCENT = 0.6F;
  private static final double SUFFOCATING_COLLISION_CHECK_SCALE = 0.35;
  private static final double MINOR_COLLISION_ANGLE_THRESHOLD_RADIAN = 0.13962634F;
  public final ClientPacketListener connection;
  private final StatsCounter stats;
  private final ClientRecipeBook recipeBook;
  private final List<AmbientSoundHandler> ambientSoundHandlers = Lists.newArrayList();
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
  private boolean startedUsingItem;
  @Nullable
  private InteractionHand usingItemHand;
  private boolean handsBusy;
  private boolean autoJumpEnabled = true;
  private int autoJumpTime;
  private boolean wasFallFlying;
  private int waterVisionTime;
  private boolean showDeathScreen = true;
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
  public boolean hurt(DamageSource arg, float f) {
    return false;
  }

  @Override
  public void heal(float f) {
  }

  @Override
  public boolean startRiding(Entity arg, boolean bl) {
    if (!super.startRiding(arg, bl)) {
      return false;
    } else {
      if (arg instanceof AbstractMinecart) {
        this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, (AbstractMinecart)arg, true));
        this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, (AbstractMinecart)arg, false));
      }

      return true;
    }
  }

  @Override
  public void removeVehicle() {
    super.removeVehicle();
    this.handsBusy = false;
  }

  @Override
  public float getViewXRot(float f) {
    return this.getXRot();
  }

  @Override
  public float getViewYRot(float f) {
    return this.isPassenger() ? super.getViewYRot(f) : this.getYRot();
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
      double g = (double)(this.getYRot() - this.yRotLast);
      double h = (double)(this.getXRot() - this.xRotLast);
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

  public boolean drop(boolean bl) {
    ServerboundPlayerActionPacket.Action lv = bl ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS : ServerboundPlayerActionPacket.Action.DROP_ITEM;
    ItemStack lv2 = this.getInventory().removeFromSelected(bl);
    this.connection.send(new ServerboundPlayerActionPacket(lv, BlockPos.ZERO, Direction.DOWN));
    return !lv2.isEmpty();
  }

  @Override
  public void swing(InteractionHand arg) {
    super.swing(arg);
    this.connection.send(new ServerboundSwingPacket(arg));
  }

  @Override
  public void respawn() {
    this.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
    KeyMapping.resetToggleKeys();
  }

  @Override
  protected void actuallyHurt(DamageSource arg, float f) {
    if (!this.isInvulnerableTo(arg)) {
      this.setHealth(this.getHealth() - f);
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

  public void hurtTo(float f) {
    if (this.flashOnSetHealth) {
      float g = this.getHealth() - f;
      if (g <= 0.0F) {
        this.setHealth(f);
        if (g < 0.0F) {
          this.invulnerableTime = 10;
        }
      } else {
        this.lastHurt = g;
        this.invulnerableTime = 20;
        this.setHealth(f);
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
      }
    } else {
      this.setHealth(f);
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

  public StatsCounter getStats() {
    return this.stats;
  }

  public ClientRecipeBook getRecipeBook() {
    return this.recipeBook;
  }

  public void removeRecipeHighlight(RecipeHolder<?> arg) {
    if (this.recipeBook.willHighlight(arg)) {
      this.recipeBook.removeHighlight(arg);
      this.connection.send(new ServerboundRecipeBookSeenRecipePacket(arg));
    }
  }

  @Override
  protected int getPermissionLevel() {
    return this.permissionLevel;
  }

  public void setPermissionLevel(int i) {
    this.permissionLevel = i;
  }

  @Override
  public void displayClientMessage(Component arg, boolean bl) {
    this.minecraft.getChatListener().handleSystemMessage(arg, bl);
  }

  private void moveTowardsClosestSpace(double d, double e) {
    BlockPos lv = BlockPos.containing(d, this.getY(), e);
    if (this.suffocatesAt(lv)) {
      double f = d - (double)lv.getX();
      double g = e - (double)lv.getZ();
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

  private boolean suffocatesAt(BlockPos arg) {
    AABB lv = this.getBoundingBox();
    AABB lv2 = new AABB((double)arg.getX(), lv.minY, (double)arg.getZ(), (double)arg.getX() + 1.0, lv.maxY, (double)arg.getZ() + 1.0).deflate(1.0E-7);
    return this.level().collidesWithSuffocatingBlock(this, lv2);
  }

  public void setExperienceValues(float f, int i, int j) {
    this.experienceProgress = f;
    this.totalExperience = i;
    this.experienceLevel = j;
  }

  @Override
  public void sendSystemMessage(Component arg) {
    this.minecraft.gui.getChat().addMessage(arg);
  }

  @Override
  public void handleEntityEvent(byte b) {
    if (b >= 24 && b <= 28) {
      this.setPermissionLevel(b - 24);
    } else {
      super.handleEntityEvent(b);
    }
  }

  public void setShowDeathScreen(boolean bl) {
    this.showDeathScreen = bl;
  }

  public boolean shouldShowDeathScreen() {
    return this.showDeathScreen;
  }

  public void setDoLimitedCrafting(boolean bl) {
    this.doLimitedCrafting = bl;
  }

  public boolean getDoLimitedCrafting() {
    return this.doLimitedCrafting;
  }

  @Override
  public boolean isEffectiveAi() {
    return true;
  }

  @Override
  public void startUsingItem(InteractionHand arg) {
    ItemStack lv = this.getItemInHand(arg);
    if (!lv.isEmpty() && !this.isUsingItem()) {
      super.startUsingItem(arg);
      this.startedUsingItem = true;
      this.usingItemHand = arg;
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
  public void onSyncedDataUpdated(EntityDataAccessor<?> arg) {
    super.onSyncedDataUpdated(arg);
    if (DATA_LIVING_ENTITY_FLAGS.equals(arg)) {
      boolean bl = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
      InteractionHand lv = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      if (bl && !this.startedUsingItem) {
        this.startUsingItem(lv);
      } else if (!bl && this.startedUsingItem) {
        this.stopUsingItem();
      }
    }

    if (DATA_SHARED_FLAGS_ID.equals(arg) && this.isFallFlying() && !this.wasFallFlying) {
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
  public void openTextEdit(SignBlockEntity arg, boolean bl) {
    if (arg instanceof HangingSignBlockEntity lv) {
      this.minecraft.setScreen(new HangingSignEditScreen(lv, bl, this.minecraft.isTextFilteringEnabled()));
    } else {
      this.minecraft.setScreen(new SignEditScreen(arg, bl, this.minecraft.isTextFilteringEnabled()));
    }
  }

  @Override
  public void openMinecartCommandBlock(BaseCommandBlock arg) {
    this.minecraft.setScreen(new MinecartCommandBlockEditScreen(arg));
  }

  @Override
  public void openCommandBlock(CommandBlockEntity arg) {
    this.minecraft.setScreen(new CommandBlockEditScreen(arg));
  }

  @Override
  public void openStructureBlock(StructureBlockEntity arg) {
    this.minecraft.setScreen(new StructureBlockEditScreen(arg));
  }

  @Override
  public void openJigsawBlock(JigsawBlockEntity arg) {
    this.minecraft.setScreen(new JigsawBlockEditScreen(arg));
  }

  @Override
  public void openItemGui(ItemStack arg, InteractionHand arg2) {
    if (arg.is(Items.WRITABLE_BOOK)) {
      this.minecraft.setScreen(new BookEditScreen(this, arg, arg2));
    }
  }

  @Override
  public void crit(Entity arg) {
    this.minecraft.particleEngine.createTrackingEmitter(arg, ParticleTypes.CRIT);
  }

  @Override
  public void magicCrit(Entity arg) {
    this.minecraft.particleEngine.createTrackingEmitter(arg, ParticleTypes.ENCHANTED_HIT);
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
      this.processPortalCooldown();
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
    float f = (float)this.getAttributeValue(Attributes.SNEAKING_SPEED);
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
      if (lv2.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(lv2) && this.tryToStartFallFlying()) {
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

  private void handleConfusionTransitionEffect(boolean bl) {
    this.oSpinningEffectIntensity = this.spinningEffectIntensity;
    float f = 0.0F;
    if (bl && this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
      if (this.minecraft.screen != null
        && !this.minecraft.screen.isPauseScreen()
        && !(this.minecraft.screen instanceof DeathScreen)
        && !(this.minecraft.screen instanceof WinScreen)) {
        if (this.minecraft.screen instanceof AbstractContainerScreen) {
          this.closeContainer();
        }

        this.minecraft.setScreen(null);
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
  public MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> arg) {
    if (arg.is(EffectType.CONFUSION)) {
      this.oSpinningEffectIntensity = 0.0F;
      this.spinningEffectIntensity = 0.0F;
    }

    return super.removeEffectNoUpdate(arg);
  }

  @Override
  public void move(MoverType arg, Vec3 arg2) {
    double d = this.getX();
    double e = this.getZ();
    super.move(arg, arg2);
    this.updateAutoJump((float)(this.getX() - d), (float)(this.getZ() - e));
  }

  public boolean isAutoJumpEnabled() {
    return this.autoJumpEnabled;
  }

  protected void updateAutoJump(float f, float g) {
    if (this.canAutoJump()) {
      Vec3 lv = this.position();
      Vec3 lv2 = lv.add((double)f, 0.0, (double)g);
      Vec3 lv3 = new Vec3((double)f, 0.0, (double)g);
      float h = this.getSpeed();
      float i = (float)lv3.lengthSqr();
      if (i <= 0.001F) {
        Vec2 lv4 = this.input.getMoveVector();
        float j = h * lv4.x;
        float k = h * lv4.y;
        float l = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
        float m = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
        lv3 = new Vec3((double)(j * m - k * l), lv3.y, (double)(k * m + j * l));
        i = (float)lv3.lengthSqr();
        if (i <= 0.001F) {
          return;
        }
      }

      float n = Mth.invSqrt(i);
      Vec3 lv5 = lv3.scale((double)n);
      Vec3 lv6 = this.getForward();
      float l = (float)(lv6.x * lv5.x + lv6.z * lv5.z);
      if (!(l < -0.15F)) {
        CollisionContext lv7 = CollisionContext.of(this);
        BlockPos lv8 = BlockPos.containing(this.getX(), this.getBoundingBox().maxY, this.getZ());
        BlockState lv9 = this.level().getBlockState(lv8);
        if (lv9.getCollisionShape(this.level(), lv8, lv7).isEmpty()) {
          lv8 = lv8.above();
          BlockState lv10 = this.level().getBlockState(lv8);
          if (lv10.getCollisionShape(this.level(), lv8, lv7).isEmpty()) {
            float o = 7.0F;
            float p = 1.2F;
            if (this.hasEffect(EffectType.JUMP)) {
              p += (float)(this.getEffect(EffectType.JUMP).getAmplifier() + 1) * 0.75F;
            }

            float q = Math.max(h * 7.0F, 1.0F / n);
            Vec3 lv12 = lv2.add(lv5.scale((double)q));
            float r = this.getBbWidth();
            float s = this.getBbHeight();
            AABB lv13 = new AABB(lv, lv12.add(0.0, (double)s, 0.0)).inflate((double)r, 0.0, (double)r);
            Vec3 lv11 = lv.add(0.0, 0.51F, 0.0);
            lv12 = lv12.add(0.0, 0.51F, 0.0);
            Vec3 lv14 = lv5.cross(new Vec3(0.0, 1.0, 0.0));
            Vec3 lv15 = lv14.scale((double)(r * 0.5F));
            Vec3 lv16 = lv11.subtract(lv15);
            Vec3 lv17 = lv12.subtract(lv15);
            Vec3 lv18 = lv11.add(lv15);
            Vec3 lv19 = lv12.add(lv15);
            Iterable<VoxelShape> iterable = this.level().getCollisions(this, lv13);
            Iterator<AABB> iterator = StreamSupport.stream(iterable.spliterator(), false).flatMap(arg -> arg.toAabbs().stream()).iterator();
            float t = Float.MIN_VALUE;

            while (iterator.hasNext()) {
              AABB lv20 = iterator.next();
              if (lv20.intersects(lv16, lv17) || lv20.intersects(lv18, lv19)) {
                t = (float)lv20.maxY;
                Vec3 lv21 = lv20.getCenter();
                BlockPos lv22 = BlockPos.containing(lv21);

                for (int u = 1; (float)u < p; u++) {
                  BlockPos lv23 = lv22.above(u);
                  BlockState lv24 = this.level().getBlockState(lv23);
                  VoxelShape lv25;
                  if (!(lv25 = lv24.getCollisionShape(this.level(), lv23, lv7)).isEmpty()) {
                    t = (float)lv25.max(Direction.Axis.Y) + (float)lv23.getY();
                    if ((double)t - this.getY() > (double)p) {
                      return;
                    }
                  }

                  if (u > 1) {
                    lv8 = lv8.above();
                    BlockState lv26 = this.level().getBlockState(lv8);
                    if (!lv26.getCollisionShape(this.level(), lv8, lv7).isEmpty()) {
                      return;
                    }
                  }
                }
                break;
              }
            }

            if (t != Float.MIN_VALUE) {
              float v = (float)((double)t - this.getY());
              if (!(v <= 0.5F) && !(v > p)) {
                this.autoJumpTime = 1;
              }
            }
          }
        }
      }
    }
  }

  @Override
  protected boolean isHorizontalCollisionMinor(Vec3 arg) {
    float f = this.getYRot() * (float) (Math.PI / 180.0);
    double d = (double)Mth.sin(f);
    double e = (double)Mth.cos(f);
    double g = (double)this.xxa * e - (double)this.zza * d;
    double h = (double)this.zza * e + (double)this.xxa * d;
    double i = Mth.square(g) + Mth.square(h);
    double j = Mth.square(arg.x) + Mth.square(arg.z);
    if (!(i < 1.0E-5F) && !(j < 1.0E-5F)) {
      double k = g * arg.x + h * arg.z;
      double l = Math.acos(k / Math.sqrt(i * j));
      return l < 0.13962634F;
    } else {
      return false;
    }
  }

  private boolean canAutoJump() {
    return this.isAutoJumpEnabled()
      && this.autoJumpTime <= 0
      && this.onGround()
      && !this.isStayingOnGroundSurface()
      && !this.isPassenger()
      && this.isMoving()
      && (double)this.getBlockJumpFactor() >= 1.0;
  }

  private boolean isMoving() {
    Vec2 lv = this.input.getMoveVector();
    return lv.x != 0.0F || lv.y != 0.0F;
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

  private boolean vehicleCanSprint(Entity arg) {
    return arg.canSprint() && arg.isControlledByLocalInstance();
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

  public void onGameModeChanged(GameType arg) {
    if (arg == GameType.SPECTATOR) {
      this.setDeltaMovement(this.getDeltaMovement().with(Direction.Axis.Y, 0.0));
    }
  }

  @Override
  public boolean isUnderWater() {
    return this.wasUnderwater;
  }

  @Override
  protected boolean updateIsUnderwater() {
    boolean bl = this.wasUnderwater;
    boolean bl2 = super.updateIsUnderwater();
    if (this.isSpectator()) {
      return this.wasUnderwater;
    } else {
      return this.wasUnderwater;
    }
  }

  @Override
  public Vec3 getRopeHoldPosition(float f) {
    if (this.minecraft.options.getCameraType().isFirstPerson()) {
      float g = Mth.lerp(f * 0.5F, this.getYRot(), this.yRotO) * (float) (Math.PI / 180.0);
      float h = Mth.lerp(f * 0.5F, this.getXRot(), this.xRotO) * (float) (Math.PI / 180.0);
      double d = this.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0;
      Vec3 lv = new Vec3(0.39 * d, -0.6, 0.3);
      return lv.xRot(-h).yRot(-g).add(this.getEyePosition(f));
    } else {
      return super.getRopeHoldPosition(f);
    }
  }

  @Override
  public float getVisualRotationYInDegrees() {
    return this.getYRot();
  }
}
