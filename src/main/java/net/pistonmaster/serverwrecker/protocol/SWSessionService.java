/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
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

import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.service.Service;
import com.github.steveice10.mc.auth.util.HTTP;
import lombok.AllArgsConstructor;
import net.pistonmaster.serverwrecker.auth.AuthType;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.UUID;

public class SWSessionService extends Service {
    private static final URI DEFAULT_MOJANG_BASE_URI = URI.create("https://sessionserver.mojang.com/session/minecraft/");
    private static final URI DEFAULT_THE_ALTENING_BASE_URI = URI.create("https://sessionserver.thealtening.com/session/minecraft/");
    private static final String JOIN_ENDPOINT = "join";
    private final AuthType authType;

    /**
     * Creates a new SessionService instance.
     */
    public SWSessionService(AuthType authType) {
        super(switch (authType) {
            case MICROSOFT -> DEFAULT_MOJANG_BASE_URI;
            case THE_ALTENING -> DEFAULT_THE_ALTENING_BASE_URI;
            default -> throw new IllegalStateException("Unexpected value: " + authType);
        });
        this.authType = authType;
    }

    /**
     * Calculates the server ID from a base string, public key, and secret key.
     *
     * @param base      Base server ID to use.
     * @param publicKey Public key to use.
     * @param secretKey Secret key to use.
     * @return The calculated server ID.
     * @throws IllegalStateException If the server ID hash algorithm is unavailable.
     */
    public String getServerId(String base, PublicKey publicKey, SecretKey secretKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(base.getBytes(StandardCharsets.ISO_8859_1));
            digest.update(secretKey.getEncoded());
            digest.update(publicKey.getEncoded());
            return new BigInteger(digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Server ID hash algorithm unavailable.", e);
        }
    }

    /**
     * Joins a server.
     *
     * @param profileId           ID of the profile to join the server with.
     * @param authenticationToken Authentication token to join the server with.
     * @param serverId            ID of the server to join.
     * @throws RequestException If an error occurs while making the request.
     */
    public void joinServer(UUID profileId, String authenticationToken, String serverId) throws RequestException {
        SWSessionService.JoinServerRequest request = new SWSessionService.JoinServerRequest(authenticationToken, profileId, serverId);
        HTTP.makeRequest(this.getProxy(), this.getEndpointUri(JOIN_ENDPOINT), request, null);
    }

    @SuppressWarnings("unused") // Used by GSON
    @AllArgsConstructor
    private static class JoinServerRequest {
        private String accessToken;
        private UUID selectedProfile;
        private String serverId;
    }
}
