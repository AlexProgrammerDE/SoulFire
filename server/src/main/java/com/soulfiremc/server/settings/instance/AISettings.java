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
package com.soulfiremc.server.settings.instance;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.ClientOptions;
import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.ImmutableIntProperty;
import com.soulfiremc.server.settings.property.ImmutableStringProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.StringProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AISettings implements SettingsObject {
  private static final String NAMESPACE = "ai";
  public static final StringProperty API_BASE_URL =
    ImmutableStringProperty.builder()
      .namespace(NAMESPACE)
      .key("api-base-url")
      .uiName("API Base URL")
      .description("API server base URL, can also be changed to other providers")
      .defaultValue(ClientOptions.PRODUCTION_URL)
      .build();
  public static final StringProperty API_KEY =
    ImmutableStringProperty.builder()
      .namespace(NAMESPACE)
      .key("api-key")
      .uiName("API Key")
      .description("API key or none if using a custom provider")
      .defaultValue("")
      .secret(true)
      .build();
  public static final IntProperty REQUEST_TIMEOUT =
    ImmutableIntProperty.builder()
      .namespace(NAMESPACE)
      .key("api-request-timeout")
      .uiName("API Request Timeout")
      .description("API request timeout (seconds)")
      .defaultValue(60)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final IntProperty MAX_RETRIES =
    ImmutableIntProperty.builder()
      .namespace(NAMESPACE)
      .key("api-max-retries")
      .uiName("API Max Retries")
      .description("API request max retries")
      .defaultValue(5)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();

  public static OpenAIClient create(InstanceSettingsSource source) {
    return OpenAIOkHttpClient.builder()
      .baseUrl(source.get(AISettings.API_BASE_URL))
      .apiKey(source.get(AISettings.API_KEY))
      .timeout(Duration.ofSeconds(source.get(AISettings.REQUEST_TIMEOUT)))
      .maxRetries(source.get(AISettings.MAX_RETRIES))
      .build();
  }
}
