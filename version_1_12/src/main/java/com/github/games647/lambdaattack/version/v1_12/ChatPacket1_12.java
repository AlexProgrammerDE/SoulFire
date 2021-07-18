package com.github.games647.lambdaattack.version.v1_12;

import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;

public class ChatPacket1_12 extends ClientChatPacket {
    public ChatPacket1_12(String message) {
        super(message);
    }
}
