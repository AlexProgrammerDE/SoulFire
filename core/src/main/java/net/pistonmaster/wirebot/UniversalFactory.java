package net.pistonmaster.wirebot;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.IPacketWrapper;

import java.net.Proxy;

public class UniversalFactory {
    public static IPacketWrapper authenticate(GameVersion gameVersion, String username) {
        switch (gameVersion) {
            case VERSION_1_11:
                return new net.pistonmaster.wirebot.version.v1_11.ProtocolWrapper(username);
            case VERSION_1_12:
                return new net.pistonmaster.wirebot.version.v1_12.ProtocolWrapper(username);
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

    public static IPacketWrapper authenticate(GameVersion gameVersion, String username, String password, Proxy proxy, ServiceServer serviceServer) throws RequestException {
        AuthenticationService authService = new AuthenticationService();

        authService.setBaseUri(serviceServer.getAuth());

        authService.setUsername(username);
        authService.setPassword(password);
        authService.setProxy(proxy);
        authService.login();

        GameProfile profile = authService.getSelectedProfile();
        String accessToken = authService.getAccessToken();
        String clientToken = authService.getClientToken();

        switch (gameVersion) {
            case VERSION_1_11:
                return new net.pistonmaster.wirebot.version.v1_11.ProtocolWrapper(profile, accessToken);
            case VERSION_1_12:
                return new net.pistonmaster.wirebot.version.v1_12.ProtocolWrapper(profile, accessToken);
            case VERSION_1_13:
                return new net.pistonmaster.wirebot.version.v1_13.ProtocolWrapper(profile, clientToken, accessToken);
            case VERSION_1_14:
                return new net.pistonmaster.wirebot.version.v1_14.ProtocolWrapper(profile, clientToken, accessToken);
            case VERSION_1_15:
                return new net.pistonmaster.wirebot.version.v1_15.ProtocolWrapper(profile, clientToken, accessToken);
            case VERSION_1_16:
                return new net.pistonmaster.wirebot.version.v1_16.ProtocolWrapper(profile, accessToken);
            case VERSION_1_17:
                return new net.pistonmaster.wirebot.version.v1_17.ProtocolWrapper(profile, accessToken);
            default:
                throw new IllegalArgumentException("Invalid game version");
        }
    }
}
