package net.pistonmaster.wirebot.version.v1_8;

import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;

public class ChatPacket1_8 extends ClientChatPacket {
    public ChatPacket1_8(String message) {
        super(message);
    }
}
