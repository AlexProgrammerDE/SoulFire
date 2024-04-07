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
package com.soulfiremc.client.gui.libs;

import com.soulfiremc.util.GsonInstance;
import com.soulfiremc.util.ReactorHttpHelper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpStatusClass;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogsUploadService {
  private static final URI UPLOAD_URI = URI.create("https://api.mclo.gs/1/log");

  private LogsUploadService() {}

  public static McLogsResponse upload(String text) {
    return ReactorHttpHelper.createReactorClient(null, true)
      .headers(h -> h.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded"))
      .post()
      .uri(UPLOAD_URI)
      .sendForm((request, form) -> form.attr("content", text))
      .responseSingle(
        (res, content) -> {
          if (res.status().codeClass() != HttpStatusClass.SUCCESS) {
            log.warn("Failed to upload: {}", res.status().code());
            throw new RuntimeException("Failed to upload");
          }

          return content
            .asString()
            .map(
              responseText -> {
                var response = GsonInstance.GSON.fromJson(responseText, McLogsResponse.class);
                if (!response.success()) {
                  log.warn("Failed to upload: {}", response.error());
                  throw new RuntimeException("Failed to upload");
                }

                return response;
              });
        })
      .block();
  }

  public record McLogsResponse(boolean success, String id, String url, String raw, String error) {}
}
