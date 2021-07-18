package com.github.games647.lambdaattack.version.v1_11;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;

public class ChatPacket1_11 extends ClientChatPacket {
    public ChatPacket1_11(String message) {
        super(message);
    }
}
