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

import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.data.EffectType;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.state.InputState;
import com.soulfiremc.server.protocol.bot.state.KeyPresses;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.SFBlockHelpers;
import com.soulfiremc.server.util.mcstructs.AABB;
import com.soulfiremc.server.util.mcstructs.Direction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;

/**
 * Represents the bot itself as an entity.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class LocalPlayer extends AbstractClientPlayer {
  private final BotConnection connection;
  private final InputState input;
  protected int sprintTriggerTime;
  private int permissionLevel;
  private double lastX = 0;
  private double lastY = 0;
  private double lastZ = 0;
  private float lastYRot = 0;
  private float lastXRot = 0;
  private boolean lastOnGround = false;
  private boolean lastHorizontalCollision = false;
  private boolean wasShiftKeyDown;
  private boolean wasSprinting;
  private boolean noPhysics = false;
  private boolean crouching;
  private int positionReminder = 0;
  private boolean wasFallFlying;
  private int autoJumpTime;
  private KeyPresses lastSentInput = KeyPresses.EMPTY;
  private boolean showDeathScreen = true;
  private boolean doLimitedCrafting = false;
  private boolean flashOnSetHealth;

  public LocalPlayer(BotConnection connection, Level level, GameProfile gameProfile) {
    this(connection, level, gameProfile, false, false);
  }

  public LocalPlayer(BotConnection connection, Level level, GameProfile gameProfile, boolean wasShiftKeyDown, boolean wasSprinting) {
    super(connection, level, gameProfile);
    this.connection = connection;
    this.input = new InputState(connection.controlState());
    uuid(gameProfile.getId());

    this.wasShiftKeyDown = wasShiftKeyDown;
    this.wasSprinting = wasSprinting;
  }

  public static boolean isAlwaysFlying(GameMode gameMode) {
    return gameMode == GameMode.SPECTATOR;
  }

  @Override
  public void tick() {
    this.tickClientLoadTimeout();
    if (this.hasClientLoaded()) {
      super.tick();

      this.sendShiftKeyState();

      if (!input.keyPresses.equals(this.lastSentInput)) {
        this.connection.sendPacket(input.keyPresses.toServerboundPlayerInputPacket());
        this.lastSentInput = input.keyPresses;
      }

      // Send position changes
      sendPositionChanges();
    }
  }

  @Override
  public void aiStep() {
    if (this.sprintTriggerTime > 0) {
      this.sprintTriggerTime--;
    }

    this.processPortalCooldown();

    var jumping = this.input.keyPresses.jump();
    var sneaking = this.input.keyPresses.shift();
    var enoughImpulseToStartSprint = this.hasEnoughImpulseToStartSprinting();
    var abilities = this.abilitiesState();
    this.crouching = !abilities.flying
      && !this.isSwimming()
      && this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SNEAKING)
      && (this.isShiftKeyDown() || !this.isSleeping() && !this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING));
    var f = (float) this.attributeValue(AttributeType.SNEAKING_SPEED);
    this.input.tick(this.isMovingSlowly(), f);
    if (this.isUsingItem()) {
      this.input.leftImpulse *= 0.2F;
      this.input.forwardImpulse *= 0.2F;
      this.sprintTriggerTime = 0;
    }

    if (this.autoJumpTime > 0) {
      this.autoJumpTime--;
      this.input.makeJump();
    }

    if (!this.noPhysics) {
      this.moveTowardsClosestSpace(this.x() - (double) this.getBbWidth() * 0.35, this.z() + (double) this.getBbWidth() * 0.35);
      this.moveTowardsClosestSpace(this.x() - (double) this.getBbWidth() * 0.35, this.z() - (double) this.getBbWidth() * 0.35);
      this.moveTowardsClosestSpace(this.x() + (double) this.getBbWidth() * 0.35, this.z() - (double) this.getBbWidth() * 0.35);
      this.moveTowardsClosestSpace(this.x() + (double) this.getBbWidth() * 0.35, this.z() + (double) this.getBbWidth() * 0.35);
    }

    if (sneaking) {
      this.sprintTriggerTime = 0;
    }

    var canStartSprinting = this.canStartSprinting();
    var onGround = this.onGround();
    var tooWeak = !sneaking && !enoughImpulseToStartSprint;
    if ((onGround || this.isUnderWater()) && tooWeak && canStartSprinting) {
      if (this.sprintTriggerTime <= 0 && !this.input.keyPresses.sprint()) {
        this.sprintTriggerTime = 7;
      } else {
        this.setSprinting(true);
      }
    }

    if ((!this.isInWater() || this.isUnderWater()) && canStartSprinting && this.input.keyPresses.sprint()) {
      this.setSprinting(true);
    }

    if (this.isSprinting()) {
      var bl8 = !this.input.hasForwardImpulse() || !this.hasEnoughFoodToStartSprinting();
      var bl9 = bl8 || this.horizontalCollision && !this.minorHorizontalCollision || this.isInWater() && !this.isUnderWater();
      if (this.isSwimming()) {
        if (!this.onGround() && !this.input.keyPresses.shift() && bl8 || !this.isInWater()) {
          this.setSprinting(false);
        }
      } else if (bl9) {
        this.setSprinting(false);
      }
    }

    var flyingChanged = false;
    if (abilities.mayfly) {
      if (isAlwaysFlying(this.connection.dataManager().gameModeState().localPlayerMode())) {
        if (!abilities.flying) {
          abilities.flying = true;
          flyingChanged = true;
          this.onUpdateAbilities();
        }
      } else if (abilities.flying != connection.controlState().flying()) {
        abilities.flying = connection.controlState().flying();
        if (abilities.flying && this.onGround()) {
          this.jumpFromGround();
        }

        flyingChanged = true;
        this.onUpdateAbilities();
      }
    }

    if (this.input.keyPresses.jump() && !flyingChanged && !jumping && !this.onClimbable() && this.tryToStartFallFlying()) {
      this.connection.sendPacket(new ServerboundPlayerCommandPacket(entityId, PlayerState.START_ELYTRA_FLYING));
    }

    this.wasFallFlying = this.isFallFlying();
    if (this.isInWater() && this.input.keyPresses.shift() && this.isAffectedByFluids()) {
      this.goDownInWater();
    }

    if (abilities.flying && this.isControlledCamera()) {
      var i = 0;
      if (this.input.keyPresses.shift()) {
        i--;
      }

      if (this.input.keyPresses.jump()) {
        i++;
      }

      if (i != 0) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, (float) i * abilities.flySpeed() * 3.0F, 0.0));
      }
    }

    super.aiStep();
    if (this.onGround() && abilities.flying && !isAlwaysFlying(this.connection.dataManager().gameModeState().localPlayerMode())) {
      abilities.flying = false;
      this.onUpdateAbilities();
    }
  }

  public void setExperienceValues(float currentXP, int maxXP, int level) {
    this.experienceProgress = currentXP;
    this.totalExperience = maxXP;
    this.experienceLevel = level;
  }

  public void resetPos() {
    this.setPose(Pose.STANDING);
    if (this.level() != null) {
      for (var y = this.y(); y > (double) this.level().getMinY() && y <= (double) this.level().getMaxY(); y++) {
        this.setPos(this.x(), y, this.z());
        if (this.level().noCollision(this.getBoundingBox())) {
          break;
        }
      }

      this.setDeltaMovement(Vector3d.ZERO);
      this.setXRot(0.0F);
    }

    this.setHealth(this.getMaxHealth());
    this.deathTime = 0;
  }

  public void swing(Hand hand) {
    this.connection.sendPacket(new ServerboundSwingPacket(hand));
  }

  public void hurtTo(float health) {
    if (this.flashOnSetHealth) {
      var newHealth = this.getHealth() - health;
      if (newHealth <= 0.0F) {
        this.setHealth(health);
        if (newHealth < 0.0F) {
          this.invulnerableTime = 10;
        }
      } else {
        this.lastHurt = newHealth;
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
  public boolean isSuppressingSlidingDownLadder() {
    return !this.abilitiesState().flying && super.isSuppressingSlidingDownLadder();
  }

  @Override
  public void serverAiStep() {
    super.serverAiStep();
    if (this.isControlledCamera()) {
      this.xxa = this.input.leftImpulse;
      this.zza = this.input.forwardImpulse;
      this.jumping = this.input.keyPresses.jump();
    }
  }

  private boolean canStartSprinting() {
    return !this.isSprinting()
      && this.hasEnoughImpulseToStartSprinting()
      && this.hasEnoughFoodToStartSprinting()
      && !this.isUsingItem()
      && !this.effectState.hasEffect(EffectType.BLINDNESS)
      && !this.isFallFlying();
  }

  public void setShowDeathScreen(boolean show) {
    this.showDeathScreen = show;
  }

  public boolean shouldShowDeathScreen() {
    return this.showDeathScreen;
  }

  public boolean getDoLimitedCrafting() {
    return this.doLimitedCrafting;
  }

  public void setDoLimitedCrafting(boolean doLimitedCrafting) {
    this.doLimitedCrafting = doLimitedCrafting;
  }

  private boolean hasEnoughFoodToStartSprinting() {
    return (float) this.foodData.getFoodLevel() > 6.0F || this.abilitiesState().mayfly;
  }

  private boolean hasEnoughImpulseToStartSprinting() {
    var minImpulse = 0.8;
    return this.isUnderWater() ? this.input.hasForwardImpulse() : (double) this.input.forwardImpulse >= minImpulse;
  }

  public void sendPositionChanges() {
    sendIsSprintingIfNeeded();
    if (isControlledCamera()) {
      // Detect whether anything changed
      var xDiff = x() - lastX;
      var yDiff = y() - lastY;
      var zDiff = z() - lastZ;
      var yRotDiff = (double) (yRot - lastYRot);
      var xRotDiff = (double) (xRot - lastXRot);
      var sendPos =
        MathHelper.lengthSquared(xDiff, yDiff, zDiff) > MathHelper.square(2.0E-4)
          || ++positionReminder >= 20;
      var sendRot = xRotDiff != 0.0 || yRotDiff != 0.0;
      var sendStatus = onGround != lastOnGround || horizontalCollision != lastHorizontalCollision;

      // Send position packets if changed
      if (sendPos && sendRot) {
        sendPosRot();
      } else if (sendPos) {
        sendPos();
      } else if (sendRot) {
        sendRot();
      } else if (sendStatus) {
        sendStatus();
      }
    }
  }

  private void moveTowardsClosestSpace(double x, double z) {
    var vec = Vector3i.from(x, this.y(), z);
    if (this.suffocatesAt(vec)) {
      var f = x - (double) vec.getX();
      var g = z - (double) vec.getZ();
      Direction bestDirection = null;
      var h = Double.MAX_VALUE;
      var directions = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

      for (var direction : directions) {
        var i = direction.getAxis().choose(f, 0.0, g);
        var j = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - i : i;
        if (j < h && !this.suffocatesAt(direction.offset(vec))) {
          h = j;
          bestDirection = direction;
        }
      }

      if (bestDirection != null) {
        var deltaMovement = this.getDeltaMovement();
        if (bestDirection.getAxis() == Direction.Axis.X) {
          this.setDeltaMovement(0.1 * (double) bestDirection.getStepX(), deltaMovement.getY(), deltaMovement.getZ());
        } else {
          this.setDeltaMovement(deltaMovement.getX(), deltaMovement.getY(), 0.1 * (double) bestDirection.getStepZ());
        }
      }
    }
  }

  private boolean suffocatesAt(Vector3i pos) {
    var bb = this.getBoundingBox();
    var checkBox = new AABB(pos.getX(), bb.minY, pos.getZ(), (double) pos.getX() + 1.0, bb.maxY, (double) pos.getZ() + 1.0).deflate(1.0E-7);
    for (var touchedPos : this.level().getTouchedPositions(checkBox)) {
      var blockState = this.level().getBlockState(touchedPos);
      if (!SFBlockHelpers.isSuffocating(blockState)) {
        continue;
      }

      var collisionBoxes = blockState.getCollisionBoxes(touchedPos);
      for (var collisionBox : collisionBoxes) {
        if (collisionBox.intersects(checkBox)) {
          return true;
        }
      }
    }

    return false;
  }

  protected boolean isControlledCamera() {
    return true;
  }

  public void handleEntityEvent(EntityEvent event) {
    switch (event) {
      case PLAYER_OP_PERMISSION_LEVEL_0 -> permissionLevel = 0;
      case PLAYER_OP_PERMISSION_LEVEL_1 -> permissionLevel = 1;
      case PLAYER_OP_PERMISSION_LEVEL_2 -> permissionLevel = 2;
      case PLAYER_OP_PERMISSION_LEVEL_3 -> permissionLevel = 3;
      case PLAYER_OP_PERMISSION_LEVEL_4 -> permissionLevel = 4;
      default -> super.handleEntityEvent(event);
    }
  }

  public void sendPosRot() {
    lastOnGround = onGround;
    lastHorizontalCollision = horizontalCollision;

    lastX = x();
    lastY = y();
    lastZ = z();
    positionReminder = 0;

    lastYRot = yRot;
    lastXRot = xRot;

    connection.sendPacket(
      new ServerboundMovePlayerPosRotPacket(onGround, horizontalCollision, x(), y(), z(), yRot, xRot));
  }

  public void sendPos() {
    lastOnGround = onGround;
    lastHorizontalCollision = horizontalCollision;

    lastX = x();
    lastY = y();
    lastZ = z();
    positionReminder = 0;

    connection.sendPacket(new ServerboundMovePlayerPosPacket(onGround, horizontalCollision, x(), y(), z()));
  }

  public void sendRot() {
    lastOnGround = onGround;
    lastHorizontalCollision = horizontalCollision;

    lastYRot = yRot;
    lastXRot = xRot;

    connection.sendPacket(new ServerboundMovePlayerRotPacket(onGround, horizontalCollision, yRot, xRot));
  }

  public void sendStatus() {
    lastOnGround = onGround;
    lastHorizontalCollision = horizontalCollision;

    connection.sendPacket(new ServerboundMovePlayerStatusOnlyPacket(onGround, horizontalCollision));
  }

  private void sendShiftKeyState() {
    var sneaking = input.keyPresses.shift();
    if (sneaking != this.wasShiftKeyDown) {
      this.connection.sendPacket(new ServerboundPlayerCommandPacket(entityId(), sneaking
        ? PlayerState.START_SNEAKING
        : PlayerState.STOP_SNEAKING));
      this.wasShiftKeyDown = sneaking;
    }
  }

  private void sendIsSprintingIfNeeded() {
    var sprinting = input.keyPresses.sprint();
    if (sprinting != this.wasSprinting) {
      this.connection.sendPacket(new ServerboundPlayerCommandPacket(entityId(), sprinting
        ? PlayerState.START_SPRINTING
        : PlayerState.STOP_SPRINTING));
      this.wasSprinting = sprinting;
    }
  }

  @Override
  public boolean isUnderWater() {
    return this.wasUnderwater;
  }

  @Override
  public boolean isShiftKeyDown() {
    return input.keyPresses.shift();
  }

  @Override
  public boolean isCrouching() {
    return this.crouching;
  }

  @Override
  protected boolean isHorizontalCollisionMinor(Vector3d deltaMovement) {
    var f = this.yRot() * (float) (Math.PI / 180.0);
    var d = (double) MathHelper.sin(f);
    var e = (double) MathHelper.cos(f);
    var g = (double) this.xxa * e - (double) this.zza * d;
    var h = (double) this.zza * e + (double) this.xxa * d;
    var i = MathHelper.square(g) + MathHelper.square(h);
    var j = MathHelper.square(deltaMovement.getX()) + MathHelper.square(deltaMovement.getZ());
    if (!(i < 1.0E-5F) && !(j < 1.0E-5F)) {
      var k = g * deltaMovement.getX() + h * deltaMovement.getZ();
      var l = Math.acos(k / Math.sqrt(i * j));
      return l < 0.13962634F;
    } else {
      return false;
    }
  }

  public boolean isMovingSlowly() {
    return this.isCrouching() || this.isVisuallyCrawling();
  }

  @Override
  public boolean isEffectiveAi() {
    return true;
  }

  @Override
  public boolean isLocalPlayer() {
    return true;
  }

  @Override
  public void onUpdateAbilities() {
    this.connection.sendPacket(this.abilitiesState().toPacket());
  }

  @Override
  protected boolean isImmobile() {
    return super.isImmobile() || this.isSleeping();
  }

  @Override
  public boolean isAffectedByFluids() {
    return !this.abilitiesState().flying;
  }

  @Override
  public int permissionLevel() {
    return this.permissionLevel;
  }

  public void onGameModeChanged(GameMode gameMode) {
    if (gameMode == GameMode.SPECTATOR) {
      var deltaMovement = this.getDeltaMovement();
      this.setDeltaMovement(Vector3d.from(deltaMovement.getX(), 0.0, deltaMovement.getZ()));
    }
  }
}
