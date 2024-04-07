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
package com.soulfiremc.server.util;

import com.google.gson.JsonObject;
import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.util.GsonInstance;
import com.soulfiremc.util.ReactorHttpHelper;
import io.netty.handler.codec.http.HttpStatusClass;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class SFUpdateChecker {
  private static final URI UPDATE_URL =
    URI.create("https://api.github.com/repos/AlexProgrammerDE/SoulFire/releases/latest");
  private static SFUpdateChecker instance;
  private final String updateVersion;

  public SFUpdateChecker() {
    this.updateVersion = checkForUpdates();
  }

  public static CompletableFuture<SFUpdateChecker> getInstance() {
    return CompletableFuture.supplyAsync(
      () -> {
        if (instance == null) {
          instance = new SFUpdateChecker();
        }

        return instance;
      });
  }

  private static String checkForUpdates() {
    if (Boolean.getBoolean("soulfire.disable-updates")) {
      log.info("Skipping update check because of system property");
      return null;
    }

    try {
      return ReactorHttpHelper.createReactorClient(null, false)
        .get()
        .uri(UPDATE_URL)
        .responseSingle(
          (res, content) -> {
            if (res.status().codeClass() != HttpStatusClass.SUCCESS) {
              log.warn("Failed to check for updates: {}", res.status().code());
              return Mono.empty();
            }

            return content
              .asString()
              .mapNotNull(
                s -> {
                  var responseObject = GsonInstance.GSON.fromJson(s, JsonObject.class);

                  var latestVersion = responseObject.get("tag_name").getAsString();
                  if (VersionComparator.isNewer(BuildData.VERSION, latestVersion)) {
                    return latestVersion;
                  } else {
                    return null;
                  }
                });
          })
        .block();
    } catch (Exception e) {
      log.warn("Failed to check for updates", e);
      return null;
    }
  }

  public Optional<String> getUpdateVersion() {
    return Optional.ofNullable(updateVersion);
  }
}
