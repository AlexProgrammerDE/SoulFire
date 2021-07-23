package net.pistonmaster.wirebot.protocol;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.IPacketWrapper;
import net.pistonmaster.wirebot.common.ServiceServer;

import java.net.Proxy;

public class AuthFactory {
    public static IPacketWrapper authenticate(GameVersion gameVersion, String username, String password, Proxy proxy, ServiceServer serviceServer) throws Exception {
        AuthenticationService authService = new AuthenticationService();

        authService.setBaseUri(serviceServer.getAuth());

        authService.setUsername(username);
        authService.setPassword(password);
        authService.setProxy(proxy);

        authService.login();

        GameProfile profile = authService.getSelectedProfile();
        String accessToken = authService.getAccessToken();
        String clientToken = authService.getClientToken();

        return switch (gameVersion) {
            case VERSION_1_11 -> new net.pistonmaster.wirebot.version.v1_11.ProtocolWrapper(profile, accessToken);
            case VERSION_1_12 -> new net.pistonmaster.wirebot.version.v1_12.ProtocolWrapper(profile, accessToken);
            case VERSION_1_13 -> new net.pistonmaster.wirebot.version.v1_13.ProtocolWrapper(profile, clientToken, accessToken);
            case VERSION_1_14 -> new net.pistonmaster.wirebot.version.v1_14.ProtocolWrapper(profile, clientToken, accessToken);
            case VERSION_1_15 -> new net.pistonmaster.wirebot.version.v1_15.ProtocolWrapper(profile, clientToken, accessToken);
            case VERSION_1_16 -> new net.pistonmaster.wirebot.version.v1_16.ProtocolWrapper(profile, accessToken);
            case VERSION_1_17 -> new net.pistonmaster.wirebot.version.v1_17.ProtocolWrapper(profile, accessToken);
            default -> throw new IllegalArgumentException("Invalid game version");
        };
    }
}
