/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.settings.instance;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.ClientOptions;
import com.soulfiremc.grpc.generated.StringSetting;
import com.soulfiremc.server.settings.lib.BotSettingsSource;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.lib.SettingsSource;
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
public final class AISettings implements SettingsObject {
  private static final String NAMESPACE = "ai";
  public static final StringProperty<SettingsSource.Bot> API_BASE_URL =
    ImmutableStringProperty.<SettingsSource.Bot>builder()
      .sourceType(SettingsSource.Bot.INSTANCE)
      .namespace(NAMESPACE)
      .key("api-base-url")
      .uiName("API Base URL")
      .description("API server base URL, can also be changed to other providers")
      .defaultValue(ClientOptions.PRODUCTION_URL)
      .build();
  public static final StringProperty<SettingsSource.Bot> API_KEY =
    ImmutableStringProperty.<SettingsSource.Bot>builder()
      .sourceType(SettingsSource.Bot.INSTANCE)
      .namespace(NAMESPACE)
      .key("api-key")
      .uiName("API Key")
      .description("API key or none if using a custom provider")
      .defaultValue("")
      .type(StringSetting.InputType.PASSWORD)
      .build();
  public static final IntProperty<SettingsSource.Bot> REQUEST_TIMEOUT =
    ImmutableIntProperty.<SettingsSource.Bot>builder()
      .sourceType(SettingsSource.Bot.INSTANCE)
      .namespace(NAMESPACE)
      .key("api-request-timeout")
      .uiName("API Request Timeout")
      .description("API request timeout (seconds)")
      .defaultValue(60)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final IntProperty<SettingsSource.Bot> MAX_RETRIES =
    ImmutableIntProperty.<SettingsSource.Bot>builder()
      .sourceType(SettingsSource.Bot.INSTANCE)
      .namespace(NAMESPACE)
      .key("api-max-retries")
      .uiName("API Max Retries")
      .description("API request max retries")
      .defaultValue(5)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();

  public static OpenAIClient create(BotSettingsSource source) {
    return OpenAIOkHttpClient.builder()
      .baseUrl(source.get(AISettings.API_BASE_URL))
      .apiKey(source.get(AISettings.API_KEY))
      .timeout(Duration.ofSeconds(source.get(AISettings.REQUEST_TIMEOUT)))
      .maxRetries(source.get(AISettings.MAX_RETRIES))
      .build();
  }
}
