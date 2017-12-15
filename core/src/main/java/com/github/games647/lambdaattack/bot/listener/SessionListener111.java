package com.github.games647.lambdaattack.bot.listener;

import com.github.games647.lambdaattack.bot.Bot;
import com.github.games647.lambdaattack.bot.EntitiyLocation;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.protocol.v1_11.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.protocol.v1_11.data.message.Message;
import com.github.steveice10.protocol.v1_11.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.protocol.v1_11.packet.ingame.server.entity.player.ServerPlayerHealthPacket;
import com.github.steveice10.protocol.v1_11.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;

import java.util.logging.Level;

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
        } else if (receiveEvent.getPacket() instanceof ServerJoinGamePacket) {
            ServerJoinGamePacket loginSuccessPacket = receiveEvent.<ServerJoinGamePacket>getPacket();
            super.onJoin();
        }
    }
}
