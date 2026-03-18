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
package com.soulfiremc.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.util.SocketAddressHelper;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

final class StickyProxyAllocator {
  static final String STICKY_PROXY_NAMESPACE = "proxy";
  static final String STICKY_PROXY_KEY = "sticky-last-proxy";

  private StickyProxyAllocator() {
  }

  static List<AssignedProxy> assign(List<MinecraftAccount> accounts, List<ProxyAllocation> proxies, boolean stickyProxiesEnabled) {
    if (accounts.isEmpty()) {
      return List.of();
    }

    if (proxies.isEmpty()) {
      return accounts.stream().map(account -> new AssignedProxy(account, null)).toList();
    }

    var assignedProxies = new LinkedHashMap<UUID, SFProxy>();

    if (stickyProxiesEnabled) {
      for (var account : accounts) {
        findStickyProxy(proxies, account).ifPresent(proxyAllocation -> {
          proxyAllocation.reserve();
          assignedProxies.put(account.profileId(), proxyAllocation.proxy());
        });
      }
    }

    for (var account : accounts) {
      if (assignedProxies.containsKey(account.profileId())) {
        continue;
      }

      var selectedProxy =
        proxies.stream()
          .filter(ProxyAllocation::isAvailable)
          .min(Comparator.comparingInt(ProxyAllocation::usedBots))
          .orElseThrow(() -> new IllegalStateException("No proxies available!"));

      selectedProxy.reserve();
      assignedProxies.put(account.profileId(), selectedProxy.proxy());
    }

    return accounts.stream().map(account -> new AssignedProxy(account, assignedProxies.get(account.profileId()))).toList();
  }

  static Optional<String> stickyProxyFingerprint(MinecraftAccount account) {
    return stickyProxyFingerprint(account.persistentMetadata());
  }

  static Optional<String> stickyProxyFingerprint(Map<String, Map<String, JsonElement>> persistentMetadata) {
    return Optional.ofNullable(persistentMetadata.get(STICKY_PROXY_NAMESPACE))
      .map(namespace -> namespace.get(STICKY_PROXY_KEY))
      .filter(JsonElement::isJsonPrimitive)
      .map(JsonElement::getAsJsonPrimitive)
      .filter(JsonPrimitive::isString)
      .map(JsonPrimitive::getAsString)
      .filter(fingerprint -> !fingerprint.isBlank());
  }

  static Map<String, Map<String, JsonElement>> withSelectedProxy(
    Map<String, Map<String, JsonElement>> persistentMetadata,
    @Nullable SFProxy proxy) {
    if (proxy == null) {
      return persistentMetadata;
    }

    return SettingsSource.Stem.withUpdatedEntry(
      persistentMetadata,
      STICKY_PROXY_NAMESPACE,
      STICKY_PROXY_KEY,
      new JsonPrimitive(proxyFingerprint(proxy)));
  }

  static String proxyFingerprint(SFProxy proxy) {
    var fingerprint = new JsonObject();
    fingerprint.addProperty("type", proxy.type().name());
    fingerprint.addProperty("address", SocketAddressHelper.serialize(proxy.address()));

    if (proxy.username() == null) {
      fingerprint.add("username", JsonNull.INSTANCE);
    } else {
      fingerprint.addProperty("username", proxy.username());
    }

    if (proxy.password() == null) {
      fingerprint.add("password", JsonNull.INSTANCE);
    } else {
      fingerprint.addProperty("password", proxy.password());
    }

    return fingerprint.toString();
  }

  private static Optional<ProxyAllocation> findStickyProxy(List<ProxyAllocation> proxies, MinecraftAccount account) {
    return stickyProxyFingerprint(account)
      .flatMap(fingerprint ->
        proxies.stream()
          .filter(ProxyAllocation::isAvailable)
          .filter(proxyAllocation -> proxyFingerprint(proxyAllocation.proxy()).equals(fingerprint))
          .findFirst());
  }

  record AssignedProxy(MinecraftAccount account, @Nullable SFProxy proxy) {
  }

  static final class ProxyAllocation {
    private final SFProxy proxy;
    private final int maxBots;
    private final AtomicInteger useCount = new AtomicInteger(0);

    ProxyAllocation(SFProxy proxy, int maxBots) {
      this.proxy = proxy;
      this.maxBots = maxBots;
    }

    SFProxy proxy() {
      return proxy;
    }

    boolean unlimited() {
      return maxBots == -1;
    }

    int availableBots() {
      return unlimited() ? Integer.MAX_VALUE : maxBots - useCount.get();
    }

    boolean isAvailable() {
      return availableBots() > 0;
    }

    int usedBots() {
      return useCount.get();
    }

    boolean hasBots() {
      return usedBots() > 0;
    }

    void reserve() {
      useCount.incrementAndGet();
    }
  }
}
