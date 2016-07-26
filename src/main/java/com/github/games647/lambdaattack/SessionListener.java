package com.github.games647.lambdaattack;

import java.util.logging.Level;

import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.mc.protocol.packet.ingame.client.world.ClientTeleportConfirmPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import org.spacehq.packetlib.event.session.DisconnectedEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.event.session.SessionAdapter;

public class SessionListener extends SessionAdapter {

    private final Bot owner;

    public SessionListener(Bot owner) {
        this.owner = owner;
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

            //send confirm packet to the server
            int teleportId = posPacket.getTeleportId();
            owner.getSession().send(new ClientTeleportConfirmPacket(teleportId));
        }
    }

    @Override
    public void disconnected(DisconnectedEvent disconnectedEvent) {
        String message = Message.fromString(disconnectedEvent.getReason()).getFullText();
        owner.getLogger().log(Level.INFO, "Disconnected: {0}", message);
    }
}
