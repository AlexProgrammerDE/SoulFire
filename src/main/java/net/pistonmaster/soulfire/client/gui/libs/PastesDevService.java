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
package net.pistonmaster.soulfire.client.gui.libs;

import com.google.gson.Gson;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.account.HttpHelper;
import net.pistonmaster.soulfire.builddata.BuildData;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;

@Slf4j
public class PastesDevService {
  private static final Gson gson = new Gson();

  private PastesDevService() {}

  private static CloseableHttpClient createHttpClient() {
    var headers = new ArrayList<Header>();
    headers.add(new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()));
    headers.add(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en"));
    headers.add(new BasicHeader(HttpHeaders.USER_AGENT, "SoulFire/" + BuildData.VERSION));

    return HttpHelper.createApacheHttpClient(headers, null);
  }

  public static String upload(String text) {
    return HttpHelper.createReactorClient(null)
        .headers(
            h -> {
              h.add("Accept", "application/json");
              h.add("Content-Type", "application/json");
              h.add("User-Agent", "SoulFire/" + BuildData.VERSION);
            })
        .post()
        .uri("https://api.pastes.dev/post")
        .send(ByteBufFlux.fromString(Flux.just(text)))
        .responseSingle(
            (res, content) -> {
              if (res.status().code() != 201) {
                log.warn("Failed to upload: {}", res.status().code());
                throw new RuntimeException("Failed to upload");
              }

              return content
                  .asString()
                  .map(
                      responseText -> {
                        var response = gson.fromJson(responseText, BytebinResponse.class);
                        return response.key();
                      });
            })
        .block();
  }

  private record BytebinResponse(String key) {}
}
