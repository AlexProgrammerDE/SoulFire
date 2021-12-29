package net.pistonmaster.serverwrecker.protocol;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import net.pistonmaster.serverwrecker.common.GameVersion;
import net.pistonmaster.serverwrecker.common.IPacketWrapper;
import net.pistonmaster.serverwrecker.common.ServiceServer;

import java.net.Proxy;

public class AuthFactory {
    public static IPacketWrapper authenticate(GameVersion gameVersion, String username) {
        return switch (gameVersion) {
            case VERSION_1_7 -> new net.pistonmaster.serverwrecker.version.v1_7.ProtocolWrapper(username);
            case VERSION_1_8 -> new net.pistonmaster.serverwrecker.version.v1_8.ProtocolWrapper(username);
            case VERSION_1_9 -> new net.pistonmaster.serverwrecker.version.v1_9.ProtocolWrapper(username);
            case VERSION_1_10 -> new net.pistonmaster.serverwrecker.version.v1_10.ProtocolWrapper(username);
            case VERSION_1_11 -> new net.pistonmaster.serverwrecker.version.v1_11.ProtocolWrapper(username);
            case VERSION_1_12 -> new net.pistonmaster.serverwrecker.version.v1_12.ProtocolWrapper(username);
            case VERSION_1_13 -> new net.pistonmaster.serverwrecker.version.v1_13.ProtocolWrapper(username);
            case VERSION_1_14 -> new net.pistonmaster.serverwrecker.version.v1_14.ProtocolWrapper(username);
            case VERSION_1_15 -> new net.pistonmaster.serverwrecker.version.v1_15.ProtocolWrapper(username);
            case VERSION_1_16 -> new net.pistonmaster.serverwrecker.version.v1_16.ProtocolWrapper(username);
            case VERSION_1_17 -> new net.pistonmaster.serverwrecker.version.v1_17.ProtocolWrapper(username);
            case VERSION_1_18 -> new net.pistonmaster.serverwrecker.version.v1_18.ProtocolWrapper(username);
        };
    }

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
            case VERSION_1_7 -> new net.pistonmaster.serverwrecker.version.v1_7.ProtocolWrapper(profile, accessToken);
            case VERSION_1_8 -> new net.pistonmaster.serverwrecker.version.v1_8.ProtocolWrapper(profile, accessToken);
            case VERSION_1_9 -> new net.pistonmaster.serverwrecker.version.v1_9.ProtocolWrapper(profile, accessToken);
            case VERSION_1_10 -> new net.pistonmaster.serverwrecker.version.v1_10.ProtocolWrapper(profile, accessToken);
            case VERSION_1_11 -> new net.pistonmaster.serverwrecker.version.v1_11.ProtocolWrapper(profile, accessToken);
            case VERSION_1_12 -> new net.pistonmaster.serverwrecker.version.v1_12.ProtocolWrapper(profile, accessToken);
            case VERSION_1_13 -> new net.pistonmaster.serverwrecker.version.v1_13.ProtocolWrapper(profile, accessToken, clientToken);
            case VERSION_1_14 -> new net.pistonmaster.serverwrecker.version.v1_14.ProtocolWrapper(profile, accessToken, clientToken);
            case VERSION_1_15 -> new net.pistonmaster.serverwrecker.version.v1_15.ProtocolWrapper(profile, accessToken, clientToken);
            case VERSION_1_16 -> new net.pistonmaster.serverwrecker.version.v1_16.ProtocolWrapper(profile, accessToken);
            case VERSION_1_17 -> new net.pistonmaster.serverwrecker.version.v1_17.ProtocolWrapper(profile, accessToken);
            case VERSION_1_18 -> new net.pistonmaster.serverwrecker.version.v1_18.ProtocolWrapper(profile, accessToken);
        };
    }
}
