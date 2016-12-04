package com.github.games647.lambdaattack;

import org.spacehq.packetlib.Session;

public class UniversalFactory {

    public static UniversalProtocol authenticate(GameVersion gameVersion, String username) {
        switch (gameVersion) {
            case VERSION_1_11:
                return new com.github.games647.lambdaattack.version.v1_11.ProtocolWrapper(username);
            case VERSION_1_10:
                return new com.github.games647.lambdaattack.version.v1_10.ProtocolWrapper(username);
            case VERSION_1_9:
                return new com.github.games647.lambdaattack.version.v1_9.ProtocolWrapper(username);
            case VERSION_1_8:
                return new com.github.games647.lambdaattack.version.v1_8.ProtocolWrapper(username);
            case VERSION_1_7:
                return new com.github.games647.lambdaattack.version.v1_7.ProtocolWrapper(username);
            default:
                throw new IllegalArgumentException("Invalid game version");
        }
    }

    public static void sendChatMessage(GameVersion gameVersion, String message, Session session) {
        switch (gameVersion) {
            case VERSION_1_11:
                session.send(new org.spacehq.mc.protocol.v1_11.packet.ingame.client.ClientChatPacket(message));
                break;
            case VERSION_1_10:
                session.send(new org.spacehq.mc.protocol.v1_10.packet.ingame.client.ClientChatPacket(message));
                break;
            case VERSION_1_9:
                session.send(new org.spacehq.mc.protocol.v1_9.packet.ingame.client.ClientChatPacket(message));
                break;
            case VERSION_1_8:
                session.send(new org.spacehq.mc.protocol.v1_8.packet.ingame.client.ClientChatPacket(message));
                break;
            case VERSION_1_7:
                session.send(new org.spacehq.mc.protocol.v1_7.packet.ingame.client.ClientChatPacket(message));
                break;
            default:
                throw new IllegalArgumentException("Invalid game version");
        }
    }
}
