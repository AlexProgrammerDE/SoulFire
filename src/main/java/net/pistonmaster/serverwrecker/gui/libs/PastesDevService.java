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
package net.pistonmaster.serverwrecker.gui.libs;

import com.google.gson.Gson;
import net.pistonmaster.serverwrecker.auth.HttpHelper;
import net.pistonmaster.serverwrecker.builddata.BuildData;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PastesDevService {
    private static final Gson gson = new Gson();

    private PastesDevService() {
    }

    private static CloseableHttpClient createHttpClient() {
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en"));
        headers.add(new BasicHeader(HttpHeaders.USER_AGENT, "ServerWrecker/" + BuildData.VERSION));

        return HttpHelper.createHttpClient(headers, null);
    }

    public static String upload(String text) throws IOException {
        try (var httpClient = createHttpClient()) {
            var httpPost = new HttpPost("https://api.pastes.dev/post");
            httpPost.setEntity(new StringEntity(text, ContentType.APPLICATION_JSON));
            try (var response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() != 201) {
                    throw new IOException("Failed to upload paste: " + response.getStatusLine().getStatusCode());
                }

                var responseText = EntityUtils.toString(response.getEntity());

                return gson.fromJson(responseText, BytebinResponse.class).key();
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private record BytebinResponse(String key) {
    }
}
