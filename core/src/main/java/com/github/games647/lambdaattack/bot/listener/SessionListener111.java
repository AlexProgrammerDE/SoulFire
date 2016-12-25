package com.github.games647.lambdaattack.bot.listener;

import com.github.games647.lambdaattack.bot.Bot;
import com.github.games647.lambdaattack.bot.EntitiyLocation;

import java.util.logging.Level;

import org.spacehq.mc.protocol.v1_11.packet.login.server.LoginSuccessPacket;
import org.spacehq.mc.protocol.v1_11.data.message.Message;
import org.spacehq.mc.protocol.v1_11.packet.ingame.server.ServerChatPacket;
import org.spacehq.mc.protocol.v1_11.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import org.spacehq.mc.protocol.v1_11.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;

public class SessionListener111 extends SessionListener {

    public SessionListener111(Bot owner) {
        super(owner);
    }

    @Override
    public void packetReceived(PacketReceivedEvent receiveEvent) {
        if (receiveEvent.getPacket() instanceof ServerChatPacket) {
            Message message = receiveEvent.<ServerChatPacket>getPacket().getMessage();
            owner.getLogger().log(Level.INFO, "Received Message: {0}", message.getFullText());
        } else if (receiveEvent.getPacket() instanceof ServerPlayerPositionRotationPacket) {
            ServerPlayerPositionRotationPacket posPacket = receiveEvent.<ServerPlayerPositionRotationPacket>getPacket();

            double posX = posPacket.getX();
            double posY = posPacket.getY();
            double posZ = posPacket.getZ();
            float pitch = posPacket.getPitch();
            float yaw = posPacket.getYaw();
            EntitiyLocation location = new EntitiyLocation(posX, posY, posZ, pitch, yaw);
            owner.setLocation(location);
        } else if (receiveEvent.getPacket() instanceof ServerPlayerHealthPacket) {
            ServerPlayerHealthPacket healthPacket = receiveEvent.<ServerPlayerHealthPacket>getPacket();
            owner.setHealth(healthPacket.getHealth());
            owner.setFood(healthPacket.getFood());
        } else if (receiveEvent.getPacket() instanceof LoginSuccessPacket) {
            LoginSuccessPacket loginSuccessPacket = receiveEvent.<LoginSuccessPacket>getPacket();
            super.onJoin(loginSuccessPacket.getProfile());
        }
    }
}
