package net.pistonmaster.wirebot.version.v1_9;

import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;

public class ChatPacket1_9 extends ClientChatPacket {
    public ChatPacket1_9(String message) {
        super(message);
    }
}
