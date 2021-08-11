package net.pistonmaster.wirebot.version.v1_7;

import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerUpdateHealthPacket;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.PacketReceivedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.wirebot.common.SessionEventBus;

@RequiredArgsConstructor
public class SessionListener1_7 extends SessionAdapter {
    private final SessionEventBus bus;

    @Override
    public void packetReceived(PacketReceivedEvent receiveEvent) {
        if (receiveEvent.getPacket() instanceof ServerChatPacket) {
            ServerChatPacket chatPacket = receiveEvent.getPacket();
            Message message = chatPacket.getMessage();
            bus.onChat(message.getFullText());
        } else if (receiveEvent.getPacket() instanceof ServerPlayerPositionRotationPacket) {
            ServerPlayerPositionRotationPacket posPacket = receiveEvent.getPacket();

            double posX = posPacket.getX();
            double posY = posPacket.getY();
            double posZ = posPacket.getZ();
            float pitch = posPacket.getPitch();
            float yaw = posPacket.getYaw();
            bus.onPosition(posX, posY, posZ, pitch, yaw);
        } else if (receiveEvent.getPacket() instanceof ServerUpdateHealthPacket) {
            ServerUpdateHealthPacket healthPacket = receiveEvent.getPacket();
            bus.onHealth(healthPacket.getHealth(), healthPacket.getFood());
        } else if (receiveEvent.getPacket() instanceof ServerJoinGamePacket) {
            bus.onJoin();
        }
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        bus.onDisconnect(event.getReason(), event.getCause());
    }
}
