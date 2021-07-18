package com.github.games647.lambdaattack.version.v1_15;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import lombok.NonNull;

public class ChatPacket1_15 extends ClientChatPacket {
    public ChatPacket1_15(@NonNull String message) {
        super(message);
    }
}
