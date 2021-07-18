package com.github.games647.lambdaattack.version.v1_13;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import lombok.NonNull;

public class ChatPacket1_13 extends ClientChatPacket {
    public ChatPacket1_13(@NonNull String message) {
        super(message);
    }
}
