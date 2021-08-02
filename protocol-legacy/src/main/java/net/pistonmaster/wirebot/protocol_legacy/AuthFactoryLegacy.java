package net.pistonmaster.wirebot.protocol_legacy;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.IPacketWrapper;
import net.pistonmaster.wirebot.common.ServiceServer;

import java.net.Proxy;

public class AuthFactoryLegacy {
    public static IPacketWrapper authenticate(GameVersion gameVersion, String username, String password, Proxy proxy, ServiceServer serviceServer) throws Exception {
        AuthenticationService authService = new AuthenticationService();

        //TODO baseurl

        authService.setUsername(username);
        authService.setPassword(password);

        // TODO proxy

        authService.login();

        GameProfile profile = authService.getSelectedProfile();
        String accessToken = authService.getAccessToken();

        return switch (gameVersion) {
            case VERSION_1_8 -> new net.pistonmaster.wirebot.version.v1_8.ProtocolWrapper(profile, accessToken);
            case VERSION_1_9 -> new net.pistonmaster.wirebot.version.v1_9.ProtocolWrapper(profile, accessToken);
            case VERSION_1_10 -> new net.pistonmaster.wirebot.version.v1_10.ProtocolWrapper(profile, accessToken);
            default -> throw new IllegalArgumentException("Invalid game version");
        };
    }
}
