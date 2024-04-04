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
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ProxyRegistry {
  private final ObjectSortedSet<EnabledWrapper<SFProxy>> proxies =
    new ObjectLinkedOpenCustomHashSet<>(
      new Hash.Strategy<>() {
        @Override
        public int hashCode(EnabledWrapper<SFProxy> obj) {
          if (obj == null) {
            return 0;
          }

          return obj.value().hashCode();
        }

        @Override
        public boolean equals(EnabledWrapper<SFProxy> obj1, EnabledWrapper<SFProxy> obj2) {
          if (obj1 == null || obj2 == null) {
            return false;
          }

          return obj1.value().equals(obj2.value());
        }
      });
  private final List<Runnable> loadHooks = new ArrayList<>();

  public void loadFromString(String data, ProxyParser proxyParser) {
    try {
      var newProxies =
        data.lines()
          .map(String::strip)
          .filter(Predicate.not(String::isBlank))
          .distinct()
          .map(proxyParser::parse)
          .map(EnabledWrapper::defaultTrue)
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

  public Collection<EnabledWrapper<SFProxy>> proxies() {
    return ObjectSortedSets.unmodifiable(proxies);
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
