/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.server.protocol;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import net.pistonmaster.soulfire.account.AuthType;
import net.pistonmaster.soulfire.account.HttpHelper;
import net.pistonmaster.soulfire.proxy.SWProxy;
import net.pistonmaster.soulfire.server.util.UUIDHelper;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.UUID;

public class SWSessionService {
    private static final URI MOJANG_JOIN_URI = URI.create("https://sessionserver.mojang.com/session/minecraft/join");
    @SuppressWarnings("HttpUrlsUsage")
    private static final URI THE_ALTENING_JOIN_URI = URI.create("http://sessionserver.thealtening.com/session/minecraft/join");
    private static final URI EASYMC_JOIN_URI = URI.create("https://sessionserver.easymc.io/session/minecraft/join");
    private final URI JOIN_ENDPOINT;
    private final SWProxy proxyData;
    private final Gson gson = new Gson();

    public SWSessionService(AuthType authType, SWProxy proxyData) {
        this.JOIN_ENDPOINT = switch (authType) {
            case MICROSOFT_JAVA -> MOJANG_JOIN_URI;
            case THE_ALTENING -> THE_ALTENING_JOIN_URI;
            case EASYMC -> EASYMC_JOIN_URI;
            default -> throw new IllegalStateException("Unexpected value: " + authType);
        };
        this.proxyData = proxyData;
    }

    public static String getServerId(String base, PublicKey publicKey, SecretKey secretKey) {
        try {
            var digest = MessageDigest.getInstance("SHA-1");
            digest.update(base.getBytes(StandardCharsets.ISO_8859_1));
            digest.update(secretKey.getEncoded());
            digest.update(publicKey.getEncoded());
            return new BigInteger(digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Server ID hash algorithm unavailable.", e);
        }
    }

    public void joinServer(UUID profileId, String authenticationToken, String serverId) throws IOException {
        try (var httpClient = HttpHelper.createMCAuthHttpClient(proxyData)) {
            var request = new SWSessionService.JoinServerRequest(
                    authenticationToken,
                    UUIDHelper.convertToNoDashes(profileId),
                    serverId
            );

            var httpPost = new HttpPost(JOIN_ENDPOINT);
            httpPost.setEntity(new StringEntity(gson.toJson(request), ContentType.APPLICATION_JSON));
            httpClient.execute(httpPost);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @SuppressWarnings("unused") // Used by GSON
    @AllArgsConstructor
    private static class JoinServerRequest {
        private String accessToken;
        private String selectedProfile;
        private String serverId;
    }
}
