package net.pistonmaster.wirebot;

import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.IPacketWrapper;
import net.pistonmaster.wirebot.common.ServiceServer;
import net.pistonmaster.wirebot.protocol.AuthFactory;

import java.net.Proxy;

public class UniversalFactory {
    public static IPacketWrapper authenticate(GameVersion gameVersion, String username) {
        return switch (gameVersion) {
            case VERSION_1_7 -> new net.pistonmaster.wirebot.version.v1_7.ProtocolWrapper(username);
            case VERSION_1_8 -> new net.pistonmaster.wirebot.version.v1_8.ProtocolWrapper(username);
            case VERSION_1_9 -> new net.pistonmaster.wirebot.version.v1_9.ProtocolWrapper(username);
            case VERSION_1_10 -> new net.pistonmaster.wirebot.version.v1_10.ProtocolWrapper(username);
            case VERSION_1_11 -> new net.pistonmaster.wirebot.version.v1_11.ProtocolWrapper(username);
            case VERSION_1_12 -> new net.pistonmaster.wirebot.version.v1_12.ProtocolWrapper(username);
            case VERSION_1_13 -> new net.pistonmaster.wirebot.version.v1_13.ProtocolWrapper(username);
            case VERSION_1_14 -> new net.pistonmaster.wirebot.version.v1_14.ProtocolWrapper(username);
            case VERSION_1_15 -> new net.pistonmaster.wirebot.version.v1_15.ProtocolWrapper(username);
            case VERSION_1_16 -> new net.pistonmaster.wirebot.version.v1_16.ProtocolWrapper(username);
            case VERSION_1_17 -> new net.pistonmaster.wirebot.version.v1_17.ProtocolWrapper(username);
        };
    }

    public static IPacketWrapper authenticate(GameVersion gameVersion, String username, String password, Proxy proxy, ServiceServer serviceServer) throws Exception {
        return AuthFactory.authenticate(gameVersion, username, password, proxy, serviceServer);
    }
}
