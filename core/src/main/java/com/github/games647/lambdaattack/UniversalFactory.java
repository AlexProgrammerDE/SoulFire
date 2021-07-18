package com.github.games647.lambdaattack;

import com.github.games647.lambdaattack.common.GameVersion;
import com.github.games647.lambdaattack.common.IPacketWrapper;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.packet.PacketProtocol;

public class UniversalFactory {
    public static IPacketWrapper authenticate(GameVersion gameVersion, String username) {
        switch (gameVersion) {
            case VERSION_1_11:
                return new com.github.games647.lambdaattack.version.v1_11.ProtocolWrapper(username);
            case VERSION_1_12:
                return new com.github.games647.lambdaattack.version.v1_12.ProtocolWrapper(username);
            case VERSION_1_14:
                return new com.github.games647.lambdaattack.version.v1_14.ProtocolWrapper(username);
            case VERSION_1_15:
                return new com.github.games647.lambdaattack.version.v1_15.ProtocolWrapper(username);
            case VERSION_1_16:
                return new com.github.games647.lambdaattack.version.v1_16.ProtocolWrapper(username);
            default:
                throw new IllegalArgumentException("Invalid game version");
        }
    }
}
