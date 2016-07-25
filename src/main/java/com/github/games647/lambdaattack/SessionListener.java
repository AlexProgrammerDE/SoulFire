package com.github.games647.lambdaattack;

import java.util.logging.Level;

import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
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
        }
    }

    @Override
    public void disconnected(DisconnectedEvent disconnectedEvent) {
        String message = Message.fromString(disconnectedEvent.getReason()).getFullText();
        owner.getLogger().log(Level.INFO, "Disconnected: {0}", message);
    }
}
