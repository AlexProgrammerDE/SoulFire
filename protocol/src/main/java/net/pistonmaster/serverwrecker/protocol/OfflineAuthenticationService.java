package net.pistonmaster.serverwrecker.protocol;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.AuthenticationService;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class OfflineAuthenticationService extends AuthenticationService {
    public OfflineAuthenticationService() {
        super(URI.create(""));
    }

    @Override
    public void login() throws RequestException {
        // No login procedure needed
        setAccessToken("");
        selectedProfile = new GameProfile(
                UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8)),
                username
        );
    }
}
