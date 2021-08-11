package net.pistonmaster.wirebot.protocol;

import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.IPacketWrapper;
import net.pistonmaster.wirebot.common.ServiceServer;
import net.pistonmaster.wirebot.version.v1_10.Auth1_10;
import net.pistonmaster.wirebot.version.v1_11.Auth1_11;
import net.pistonmaster.wirebot.version.v1_12.Auth1_12;
import net.pistonmaster.wirebot.version.v1_13.Auth1_13;
import net.pistonmaster.wirebot.version.v1_14.Auth1_14;
import net.pistonmaster.wirebot.version.v1_15.Auth1_15;
import net.pistonmaster.wirebot.version.v1_16.Auth1_16;
import net.pistonmaster.wirebot.version.v1_17.Auth1_17;
import net.pistonmaster.wirebot.version.v1_7.Auth1_7;
import net.pistonmaster.wirebot.version.v1_8.Auth1_8;
import net.pistonmaster.wirebot.version.v1_9.Auth1_9;

import java.net.Proxy;

public class AuthFactory {
    public static IPacketWrapper authenticate(GameVersion gameVersion, String username, String password, Proxy proxy, ServiceServer serviceServer) throws Exception {
        return switch (gameVersion) {
            case VERSION_1_7 -> new Auth1_7().authenticate(gameVersion, username, password, proxy, serviceServer);
            case VERSION_1_8 -> new Auth1_8().authenticate(gameVersion, username, password, proxy, serviceServer);
            case VERSION_1_9 -> new Auth1_9().authenticate(gameVersion, username, password, proxy, serviceServer);
            case VERSION_1_10 -> new Auth1_10().authenticate(gameVersion, username, password, proxy, serviceServer);
            case VERSION_1_11 -> new Auth1_11().authenticate(gameVersion, username, password, proxy, serviceServer);
            case VERSION_1_12 -> new Auth1_12().authenticate(gameVersion, username, password, proxy, serviceServer);
            case VERSION_1_13 -> new Auth1_13().authenticate(gameVersion, username, password, proxy, serviceServer);
            case VERSION_1_14 -> new Auth1_14().authenticate(gameVersion, username, password, proxy, serviceServer);
            case VERSION_1_15 -> new Auth1_15().authenticate(gameVersion, username, password, proxy, serviceServer);
            case VERSION_1_16 -> new Auth1_16().authenticate(gameVersion, username, password, proxy, serviceServer);
            case VERSION_1_17 -> new Auth1_17().authenticate(gameVersion, username, password, proxy, serviceServer);
        };
    }
}
