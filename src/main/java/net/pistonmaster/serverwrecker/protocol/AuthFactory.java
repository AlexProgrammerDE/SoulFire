/*
 * ServerWrecker
 *
 * Copyright (C) 2022 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.AuthenticationService;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import net.pistonmaster.serverwrecker.common.GameVersion;
import net.pistonmaster.serverwrecker.common.ServiceServer;

import java.net.Proxy;
import java.util.Map;

public class AuthFactory {
    public static ProtocolWrapper authenticate(String username) {
        return new ProtocolWrapper(username);
    }

    public static ProtocolWrapper authenticate(String username, String password, Proxy proxy, ServiceServer serviceServer, Map<String, String> serviceServerConfig) throws Exception {
        AuthenticationService authService = switch (serviceServer) {
            case OFFLINE -> new OfflineAuthenticationService();
            case MOJANG -> new MojangAuthenticationService();
            case MICROSOFT -> new MsaAuthenticationService(serviceServerConfig.get("clientId"));
        };

        authService.setUsername(username);
        authService.setPassword(password);
        authService.setProxy(proxy);

        authService.login();

        GameProfile profile = authService.getSelectedProfile();
        String accessToken = authService.getAccessToken();

        return new ProtocolWrapper(profile, accessToken);
    }
}
