/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.proxy;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class ProxyRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRegistry.class);
    private final List<SWProxy> proxies = new ArrayList<>();
    private final List<Runnable> loadHooks = new ArrayList<>();

    public void loadFromString(String file, ProxyType proxyType) {
        var newProxies = new ArrayList<SWProxy>();

        file.lines()
                .filter(line -> !line.isBlank())
                .distinct()
                .map(line -> fromString(line, proxyType))
                .forEach(newProxies::add);

        if (newProxies.isEmpty()) {
            LOGGER.warn("No accounts found in the provided file!");
            return;
        }

        this.proxies.addAll(newProxies);
        LOGGER.info("Loaded {} proxies!", newProxies.size());

        callLoadHooks();
    }

    private SWProxy fromString(String proxy, ProxyType proxyType) {
        proxy = proxy.trim();

        var split = proxy.split(":");

        var host = split[0];
        var port = Integer.parseInt(split[1]);
        var username = getIndexOrNull(split, 2);
        var password = getIndexOrNull(split, 3);

        return new SWProxy(proxyType, host, port, username, password, true);
    }

    private <T> T getIndexOrNull(T[] array, int index) {
        if (index < array.length) {
            return array[index];
        } else {
            return null;
        }
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
