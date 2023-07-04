package net.pistonmaster.serverwrecker.protocol.bot;

import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.protocol.bot.model.AbilitiesData;

/**
 * This class is used to control the bot.
 * The goal is to reduce friction for doing simple things.
 */
@RequiredArgsConstructor
public class BotControlAPI {
    private final SessionDataManager sessionDataManager;
    private final BotMovementManager botMovementManager;

    public boolean toggleFlight() {
        AbilitiesData abilitiesData = sessionDataManager.getAbilitiesData();
        if (abilitiesData != null && !abilitiesData.allowFlying()) {
            throw new IllegalStateException("You can't fly! (Server said so)");
        }

        boolean newFly = !botMovementManager.isFlying();
        botMovementManager.setFlying(newFly);

        // Let the server know we are flying
        sessionDataManager.getSession().send(new ServerboundPlayerAbilitiesPacket(newFly));

        return newFly;
    }

    public boolean toggleSprint() {
        boolean newSprint = !botMovementManager.isSprinting();
        botMovementManager.setSprinting(newSprint);

        // Let the server know we are sprinting
        sessionDataManager.getSession().send(new ServerboundPlayerCommandPacket(
                sessionDataManager.getLoginData().entityId(),
                newSprint ? PlayerState.START_SPRINTING : PlayerState.STOP_SPRINTING
        ));

        return newSprint;
    }

    public boolean toggleSneak() {
        boolean newSneak = !botMovementManager.isSneaking();
        botMovementManager.setSneaking(newSneak);

        // Let the server know we are sneaking
        sessionDataManager.getSession().send(new ServerboundPlayerCommandPacket(
                sessionDataManager.getLoginData().entityId(),
                newSneak ? PlayerState.START_SNEAKING : PlayerState.STOP_SNEAKING
        ));

        return newSneak;
    }
}
