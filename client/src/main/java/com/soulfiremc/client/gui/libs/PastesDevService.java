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
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;

@Slf4j
public class PastesDevService {
  private static final URI PASTES_DEV_URI = URI.create("https://api.pastes.dev/post");

  private PastesDevService() {}

  public static String upload(String text) {
    return ReactorHttpHelper.createReactorClient(null, true)
        .post()
        .uri(PASTES_DEV_URI)
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
                        var response =
                            GsonInstance.GSON.fromJson(responseText, BytebinResponse.class);
                        return response.key();
                      });
            })
        .block();
  }

  private record BytebinResponse(String key) {}
}
