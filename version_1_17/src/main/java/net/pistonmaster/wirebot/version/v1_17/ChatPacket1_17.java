package net.pistonmaster.wirebot.version.v1_17;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import lombok.NonNull;

public class ChatPacket1_17 extends ClientChatPacket {
    public ChatPacket1_17(@NonNull String message) {
        super(message);
    }
}
