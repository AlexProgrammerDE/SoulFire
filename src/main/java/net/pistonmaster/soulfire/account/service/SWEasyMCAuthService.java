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
package net.pistonmaster.soulfire.account.service;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.account.AuthType;
import net.pistonmaster.soulfire.account.HttpHelper;
import net.pistonmaster.soulfire.account.MinecraftAccount;
import net.pistonmaster.soulfire.proxy.SWProxy;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Slf4j
public final class SWEasyMCAuthService implements MCAuthService<SWEasyMCAuthService.EasyMCAuthData> {
    private static final URI AUTHENTICATE_ENDPOINT = URI.create("https://api.easymc.io/v1/token/redeem");
    private final Gson gson = new Gson();

    @Override
    public MinecraftAccount login(EasyMCAuthData data, SWProxy proxyData) throws IOException {
        try (var httpClient = HttpHelper.createMCAuthHttpClient(proxyData)) {
            var request = new AuthenticationRequest(data.altToken);
            var httpPost = new HttpPost(AUTHENTICATE_ENDPOINT);
            httpPost.setEntity(new StringEntity(gson.toJson(request), ContentType.APPLICATION_JSON));
            var response = gson.fromJson(EntityUtils.toString(httpClient.execute(httpPost).getEntity()),
                    TokenRedeemResponse.class);

            if (response.error() != null) {
                log.error("EasyMC has returned a error: {}", response.error());
                throw new IOException(response.error());
            }

            if (response.message() != null) {
                log.info("EasyMC has a message for you (This is not a error): {}", response.message());
            }

            return new MinecraftAccount(
                    AuthType.EASYMC,
                    response.mcName(),
                    new OnlineJavaData(
                            UUID.fromString(response.uuid()),
                            response.session(),
                            -1
                    ),
                    true
            );
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public EasyMCAuthData createData(String data) {
        var split = data.split(":");

        if (split.length != 1) {
            throw new IllegalArgumentException("Invalid data!");
        }

        return new EasyMCAuthData(split[0].trim());
    }

    public record EasyMCAuthData(String altToken) {
    }

    private record AuthenticationRequest(String token) {
    }

    @SuppressWarnings("unused") // Used by GSON
    @Getter
    private static class TokenRedeemResponse {
        private String mcName;
        private String uuid;
        private String session;
        private String message;
        private String error;
    }
}
