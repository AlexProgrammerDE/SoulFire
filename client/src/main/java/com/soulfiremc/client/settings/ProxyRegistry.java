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
package com.soulfiremc.client.settings;

import com.soulfiremc.settings.proxy.SFProxy;
import com.soulfiremc.util.EnabledWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ProxyRegistry {
  private final List<EnabledWrapper<SFProxy>> proxies = new ArrayList<>();
  private final List<Runnable> loadHooks = new ArrayList<>();

  public void loadFromString(String data, ProxyParser proxyParser) {
    try {
      var newProxies =
          data.lines()
              .map(String::strip)
              .filter(line -> !line.isBlank())
              .distinct()
              .map(line -> fromStringSingle(line, proxyParser))
              .toList();

      if (newProxies.isEmpty()) {
        log.warn("No proxies found in the provided data!");
        return;
      }

      this.proxies.addAll(newProxies);
      callLoadHooks();

      log.info("Loaded {} proxies!", newProxies.size());
    } catch (Exception e) {
      log.error("Failed to load proxies from string!", e);
    }
  }

  private EnabledWrapper<SFProxy> fromStringSingle(String data, ProxyParser proxyParser) {
    return new EnabledWrapper<>(true, proxyParser.parse(data));
  }

  public List<EnabledWrapper<SFProxy>> getProxies() {
    return Collections.unmodifiableList(proxies);
  }

  public void setProxies(List<EnabledWrapper<SFProxy>> proxies) {
    this.proxies.clear();
    this.proxies.addAll(proxies);
  }

  public void callLoadHooks() {
    loadHooks.forEach(Runnable::run);
  }

  public void addLoadHook(Runnable runnable) {
    loadHooks.add(runnable);
  }
}
