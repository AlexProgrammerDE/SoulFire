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
package net.pistonmaster.serverwrecker.auth.service;

import com.google.gson.Gson;
import lombok.Getter;
import net.pistonmaster.serverwrecker.auth.AuthType;
import net.pistonmaster.serverwrecker.auth.HttpHelper;
import net.pistonmaster.serverwrecker.auth.MinecraftAccount;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.proxy.SWProxy;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;

public class SWEasyMCAuthService implements MCAuthService {
    private static final URI AUTHENTICATE_ENDPOINT = URI.create("https://api.easymc.io/v1/token/redeem");
    private static final Logger LOGGER = LoggerFactory.getLogger(SWEasyMCAuthService.class);
    private final Gson gson = new Gson();

    private static CloseableHttpClient createHttpClient(SWProxy proxyData) {
        var headers = new ArrayList<Header>();
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en"));
        headers.add(new BasicHeader(HttpHeaders.USER_AGENT, "ServerWrecker/" + BuildData.VERSION));

        return HttpHelper.createHttpClient(headers, proxyData);
    }

    public MinecraftAccount login(String altToken, SWProxy proxyData) throws IOException {
        try (var httpClient = createHttpClient(proxyData)) {
            var request = new AuthenticationRequest(altToken);
            var httpPost = new HttpPost(AUTHENTICATE_ENDPOINT);
            httpPost.setEntity(new StringEntity(gson.toJson(request), ContentType.APPLICATION_JSON));
            var response = gson.fromJson(EntityUtils.toString(httpClient.execute(httpPost).getEntity()),
                    TokenRedeemResponse.class);

            if (response.getMessage() != null) {
                LOGGER.info("EasyMC has a message for you (This is not a error): {}", response.getMessage());
            }

            return new MinecraftAccount(
                    AuthType.EASYMC,
                    response.getMcName(),
                    new JavaData(
                            UUID.fromString(response.getUuid()),
                            response.getSession(),
                            -1
                    ),
                    true
            );
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private record AuthenticationRequest(String token) {
    }

    @Getter
    private static class TokenRedeemResponse {
        private String mcName;
        private String uuid;
        private String session;
        private String message;
    }
}
