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
package net.pistonmaster.soulfire.server.protocol.bot.state.entity;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerStatusOnlyPacket;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.soulfire.server.data.EntityType;
import net.pistonmaster.soulfire.server.protocol.bot.SessionDataManager;
import net.pistonmaster.soulfire.server.protocol.bot.movement.BotMovementManager;
import net.pistonmaster.soulfire.server.protocol.bot.movement.ControlState;
import net.pistonmaster.soulfire.server.protocol.bot.movement.PhysicsData;
import net.pistonmaster.soulfire.server.protocol.bot.movement.PlayerMovementState;
import net.pistonmaster.soulfire.server.util.MathHelper;

/**
 * Represents the bot itself as an entity.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ClientEntity extends Entity {
    private final PhysicsData physics = new PhysicsData();
    private final SessionDataManager sessionDataManager;
    private final ControlState controlState;
    private final PlayerMovementState movementState;
    private final BotMovementManager botMovementManager;
    private boolean showReducedDebug;
    private int opPermissionLevel;
    private double lastX = 0;
    private double lastY = 0;
    private double lastZ = 0;
    private float lastYaw = 0;
    private float lastPitch = 0;
    private boolean lastOnGround = false;
    private int positionReminder = 0;

    public ClientEntity(int entityId, SessionDataManager sessionDataManager, ControlState controlState) {
        super(entityId, EntityType.PLAYER);
        this.sessionDataManager = sessionDataManager;
        this.controlState = controlState;
        this.movementState = new PlayerMovementState(this, sessionDataManager.inventoryManager().getPlayerInventory());
        this.botMovementManager = new BotMovementManager(sessionDataManager, movementState, this);
        this.yaw = -180;
    }

    @Override
    public void tick() {
        super.tick();

        // Collect data for calculations
        movementState.updateData();

        // Tick physics movement
        var level = sessionDataManager.getCurrentLevel();
        if (level != null && level.isChunkLoaded(this.blockPos())) {
            botMovementManager.tick();
        }

        // Apply calculated state
        movementState.applyData();

        // Send position changes
        sendPositionChanges();
    }

    public void sendPositionChanges() {
        // Detect whether anything changed
        var xDiff = x - lastX;
        var yDiff = y - lastY;
        var zDiff = z - lastZ;
        var yawDiff = (double) (yaw - lastYaw);
        var pitchDiff = (double) (pitch - lastPitch);
        var sendPos = MathHelper.lengthSquared(xDiff, yDiff, zDiff) > MathHelper.square(2.0E-4) || ++positionReminder >= 20;
        var sendRot = pitchDiff != 0.0 || yawDiff != 0.0;
        var sendOnGround = onGround != lastOnGround;

        // Send position packets if changed
        if (sendPos && sendRot) {
            sendPosRot();
        } else if (sendPos) {
            sendPos();
        } else if (sendRot) {
            sendRot();
        } else if (sendOnGround) {
            sendOnGround();
        }
    }

    public void handleEntityEvent(EntityEvent event) {
        switch (event) {
            case PLAYER_ENABLE_REDUCED_DEBUG -> showReducedDebug = true;
            case PLAYER_DISABLE_REDUCED_DEBUG -> showReducedDebug = false;
            case PLAYER_OP_PERMISSION_LEVEL_0 -> opPermissionLevel = 0;
            case PLAYER_OP_PERMISSION_LEVEL_1 -> opPermissionLevel = 1;
            case PLAYER_OP_PERMISSION_LEVEL_2 -> opPermissionLevel = 2;
            case PLAYER_OP_PERMISSION_LEVEL_3 -> opPermissionLevel = 3;
            case PLAYER_OP_PERMISSION_LEVEL_4 -> opPermissionLevel = 4;
            default -> super.handleEntityEvent(event);
        }
    }

    @Override
    public double getEyeHeight() {
        return this.controlState.sneaking() ? 1.50F : 1.62F;
    }

    public void sendPosRot() {
        var onGround = movementState.onGround;

        lastOnGround = onGround;

        lastX = x;
        lastY = y;
        lastZ = z;
        positionReminder = 0;

        lastYaw = yaw;
        lastPitch = pitch;

        sessionDataManager.sendPacket(new ServerboundMovePlayerPosRotPacket(onGround, x, y, z, yaw, pitch));
    }

    public void sendPos() {
        var onGround = movementState.onGround;

        lastOnGround = onGround;

        lastX = x;
        lastY = y;
        lastZ = z;
        positionReminder = 0;

        sessionDataManager.sendPacket(new ServerboundMovePlayerPosPacket(onGround, x, y, z));
    }

    public void sendRot() {
        var onGround = movementState.onGround;

        lastOnGround = onGround;

        lastYaw = yaw;
        lastPitch = pitch;

        sessionDataManager.sendPacket(new ServerboundMovePlayerRotPacket(onGround, yaw, pitch));
    }

    public void sendOnGround() {
        var onGround = movementState.onGround;

        lastOnGround = onGround;

        sessionDataManager.sendPacket(new ServerboundMovePlayerStatusOnlyPacket(onGround));
    }

    public void jump() {
        movementState.jumpQueued = true;
    }

    @Override
    public float height() {
        return this.controlState.sneaking() ? physics.playerSneakHeight : physics.playerHeight;
    }
}
