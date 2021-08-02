package net.pistonmaster.wirebot.version.v1_8;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;

public class ChatPacket1_8 extends ClientChatPacket {
    public ChatPacket1_8(String message) {
        super(message);
    }
}
