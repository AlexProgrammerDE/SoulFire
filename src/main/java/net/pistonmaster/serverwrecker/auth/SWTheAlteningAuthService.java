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
package net.pistonmaster.serverwrecker.auth;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.auth.service.MojangAuthenticationService;
import com.github.steveice10.mc.auth.util.HTTP;
import com.google.gson.Gson;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import net.pistonmaster.serverwrecker.common.SWProxy;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SWTheAlteningAuthService implements MCAuthService {
    private final Gson gson = new Gson();
    @SuppressWarnings("HttpUrlsUsage") // The Altening doesn't support encrypted HTTPS
    private static final URI AUTHENTICATE_ENDPOINT = URI.create("http://authserver.thealtening.com/authenticate");

    private static CloseableHttpClient createHttpClient(SWProxy proxyData) {
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en"));
        headers.add(new BasicHeader(HttpHeaders.USER_AGENT, "ServerWrecker/" + BuildData.VERSION));

        return HttpHelper.createHttpClient(headers, proxyData);
    }

    public JavaAccount login(String email, String password, SWProxy proxyData) throws IOException {
        try (CloseableHttpClient httpClient = createHttpClient(proxyData)) {
            AuthenticationRequest request = new AuthenticationRequest(email, password, UUID.randomUUID().toString());
            HttpPost httpPost = new HttpPost(AUTHENTICATE_ENDPOINT);
            httpPost.setEntity(new StringEntity(gson.toJson(request), ContentType.APPLICATION_JSON));
            AuthenticateRefreshResponse response = gson.fromJson(EntityUtils.toString(httpClient.execute(httpPost).getEntity()),
                    AuthenticateRefreshResponse.class);

            return new JavaAccount(AuthType.THE_ALTENING, response.selectedProfile.getName(), response.selectedProfile.getId(), response.accessToken, -1);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static class Agent {
        private String name;
        private int version;

        protected Agent(String name, int version) {
            this.name = name;
            this.version = version;
        }
    }

    private static class User {
        public String id;
        public List<GameProfile.Property> properties;
    }

    private static class AuthenticationRequest {
        private Agent agent;
        private String username;
        private String password;
        private String clientToken;
        private boolean requestUser;

        protected AuthenticationRequest(String username, String password, String clientToken) {
            this.agent = new Agent("Minecraft", 1);
            this.username = username;
            this.password = password;
            this.clientToken = clientToken;
            this.requestUser = true;
        }
    }

    private static class AuthenticateRefreshResponse {
        public String accessToken;
        public String clientToken;
        public GameProfile selectedProfile;
        public GameProfile[] availableProfiles;
        public User user;
    }
}
