package net.pistonmaster.wirebot;

import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.IPacketWrapper;
import net.pistonmaster.wirebot.version.v1_12.ProtocolWrapper;

public class UniversalFactory {
    public static IPacketWrapper authenticate(GameVersion gameVersion, String username) {
        switch (gameVersion) {
            case VERSION_1_11:
                return new net.pistonmaster.wirebot.version.v1_11.ProtocolWrapper(username);
            case VERSION_1_12:
                return new ProtocolWrapper(username);
            case VERSION_1_13:
                return new net.pistonmaster.wirebot.version.v1_13.ProtocolWrapper(username);
            case VERSION_1_14:
                return new net.pistonmaster.wirebot.version.v1_14.ProtocolWrapper(username);
            case VERSION_1_15:
                return new net.pistonmaster.wirebot.version.v1_15.ProtocolWrapper(username);
            case VERSION_1_16:
                return new net.pistonmaster.wirebot.version.v1_16.ProtocolWrapper(username);
            case VERSION_1_17:
                return new net.pistonmaster.wirebot.version.v1_17.ProtocolWrapper(username);
            default:
                throw new IllegalArgumentException("Invalid game version");
        }
    }
}
