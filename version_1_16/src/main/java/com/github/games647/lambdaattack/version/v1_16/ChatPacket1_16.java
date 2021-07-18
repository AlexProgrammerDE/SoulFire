package com.github.games647.lambdaattack.version.v1_16;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import lombok.NonNull;

public class ChatPacket1_16 extends ClientChatPacket {
    public ChatPacket1_16(@NonNull String message) {
        super(message);
    }
}
