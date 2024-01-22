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
package net.pistonmaster.soulfire.client.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.proxy.ProxyType;
import net.pistonmaster.soulfire.proxy.SWProxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ProxyRegistry {
    private final List<SWProxy> proxies = new ArrayList<>();
    private final List<Runnable> loadHooks = new ArrayList<>();

    private static <T> T getIndexOrNull(T[] array, int index) {
        if (index < array.length) {
            return array[index];
        } else {
            return null;
        }
    }

    public void loadFromString(String data, ProxyType proxyType) {
        var newProxies = data.lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .distinct()
                .map(line -> fromStringSingle(line, proxyType))
                .toList();

        if (newProxies.isEmpty()) {
            log.warn("No proxies found in the provided data!");
            return;
        }

        this.proxies.addAll(newProxies);
        callLoadHooks();

        log.info("Loaded {} proxies!", newProxies.size());
    }

    private SWProxy fromStringSingle(String data, ProxyType proxyType) {
        data = data.trim();

        var split = data.split(":");

        var host = split[0];
        var port = Integer.parseInt(split[1]);
        var username = getIndexOrNull(split, 2);
        var password = getIndexOrNull(split, 3);

        return new SWProxy(proxyType, host, port, username, password, true);
    }

    public List<SWProxy> getProxies() {
        return Collections.unmodifiableList(proxies);
    }

    public void setProxies(List<SWProxy> proxies) {
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
