package com.github.games647.lambdaattack;

import com.github.steveice10.packetlib.Session;

public class UniversalFactory {

    public static UniversalProtocol authenticate(GameVersion gameVersion, String username) {
        switch (gameVersion) {
            case VERSION_1_11:
                return new com.github.games647.lambdaattack.version.v1_11.ProtocolWrapper(username);
            case VERSION_1_12:
                return new com.github.games647.lambdaattack.version.v1_12.ProtocolWrapper(username);
            case VERSION_1_14:
                return new com.github.games647.lambdaattack.version.v1_14.ProtocolWrapper(username);
            case VERSION_1_15:
                return new com.github.games647.lambdaattack.version.v1_15.ProtocolWrapper(username);
            default:
                throw new IllegalArgumentException("Invalid game version");
        }
    }

    public static void sendChatMessage(GameVersion gameVersion, String message, Session session) {
        switch (gameVersion) {
            case VERSION_1_11:
                session.send(new com.github.steveice10.protocol.v1_11.packet.ingame.client.ClientChatPacket(message));
                break;
            case VERSION_1_12:
                session.send(new com.github.steveice10.protocol.v1_12.packet.ingame.client.ClientChatPacket(message));
                break;
            case VERSION_1_14:
                session.send(new com.github.steveice10.protocol.v1_14.packet.ingame.client.ClientChatPacket(message));
                break;
            case VERSION_1_15:
                session.send(new com.github.steveice10.protocol.v1_15.packet.ingame.client.ClientChatPacket(message));
                break;
            default:
                throw new IllegalArgumentException("Invalid game version");
        }
    }
}
