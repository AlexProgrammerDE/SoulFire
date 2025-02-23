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

import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProxySettings implements SettingsObject {
  private static final String NAMESPACE = "proxy";
  public static final IntProperty BOTS_PER_PROXY =
    ImmutableIntProperty.builder()
      .namespace(NAMESPACE)
      .key("bots-per-proxy")
      .uiName("Bots per proxy")
      .description("Amount of bots that can be on a single proxy")
      .defaultValue(-1)
      .minValue(-1)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final BooleanProperty SHUFFLE_PROXIES =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("shuffle-proxies")
      .uiName("Shuffle proxies")
      .description("Should the proxy order be random when connecting bots?")
      .defaultValue(false)
      .build();
  public static final ComboProperty PROXY_CHECK_SERVICE =
    ImmutableComboProperty.builder()
      .namespace(NAMESPACE)
      .key("proxy-check-service")
      .uiName("Proxy check service")
      .description("What service to use to check if a proxy is working")
      .defaultValue(ProxyCheckService.IPIFY.name())
      .addOptions(ComboProperty.optionsFromEnum(ProxyCheckService.values(), Enum::name))
      .build();
  public static final IntProperty PROXY_CHECK_CONCURRENCY =
    ImmutableIntProperty.builder()
      .namespace(NAMESPACE)
      .key("proxy-check-concurrency")
      .uiName("Proxy check concurrency")
      .description("Amount of proxies to check at the same time")
      .defaultValue(10)
      .minValue(1)
      .maxValue(Integer.MAX_VALUE)
      .build();

  public enum ProxyCheckService {
    IPIFY,
    AWS
  }
}
