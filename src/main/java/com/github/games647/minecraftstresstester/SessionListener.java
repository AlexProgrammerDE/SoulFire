package com.github.games647.minecraftstresstester;

import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import org.spacehq.packetlib.event.session.DisconnectedEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.event.session.SessionAdapter;

public class SessionListener extends SessionAdapter {

    @Override
    public void packetReceived(PacketReceivedEvent receiveEvent) {
        if ((receiveEvent.getPacket() instanceof ServerJoinGamePacket)) {
//            receiveEvent.getSession().send(new ClientChatPacket("JOINED"));
        } else if (receiveEvent.getPacket() instanceof ServerChatPacket) {
            Message message = receiveEvent.<ServerChatPacket>getPacket().getMessage();
            System.out.println("Received Message: " + message.getFullText());
        }
    }

    @Override
    public void disconnected(DisconnectedEvent disconnectedEvent) {
        System.out.println("Disconnected: " + Message.fromString(disconnectedEvent.getReason()).getFullText());
    }
}
