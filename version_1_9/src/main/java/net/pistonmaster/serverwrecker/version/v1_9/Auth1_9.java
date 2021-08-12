package net.pistonmaster.serverwrecker.version.v1_9;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import net.pistonmaster.serverwrecker.common.GameVersion;
import net.pistonmaster.serverwrecker.common.IAuth;
import net.pistonmaster.serverwrecker.common.IPacketWrapper;
import net.pistonmaster.serverwrecker.common.ServiceServer;

import java.net.Proxy;

public class Auth1_9 implements IAuth {
    public IPacketWrapper authenticate(GameVersion gameVersion, String username, String password, Proxy proxy, ServiceServer serviceServer) throws Exception {
        AuthenticationService authService = new AuthenticationService();

        // authService.setBaseUri(serviceServer.getAuth()); // TODO

        authService.setUsername(username);
        authService.setPassword(password);
        // authService.setProxy(proxy); // TODO

        authService.login();

        GameProfile profile = authService.getSelectedProfile();
        String accessToken = authService.getAccessToken();

        return new ProtocolWrapper(profile, accessToken);
    }
}
