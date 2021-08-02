package net.pistonmaster.wirebot.version.v1_8;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import net.pistonmaster.wirebot.common.GameVersion;
import net.pistonmaster.wirebot.common.IAuth;
import net.pistonmaster.wirebot.common.IPacketWrapper;
import net.pistonmaster.wirebot.common.ServiceServer;

import java.net.Proxy;

public class Auth1_8 implements IAuth {
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
