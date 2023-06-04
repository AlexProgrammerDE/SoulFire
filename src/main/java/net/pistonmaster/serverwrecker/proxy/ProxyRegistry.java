
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
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.common.ProxyType;
import net.pistonmaster.serverwrecker.common.SWProxy;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class ProxyRegistry implements SettingsDuplex<ProxyList> {
    private final List<SWProxy> proxies = new ArrayList<>();
    private final ServerWrecker serverWrecker;

    public void loadFromFile(Path file) throws IOException {
        loadFromString(Files.readString(file));
    }

    public void loadFromString(String file) {
        List<SWProxy> newProxies = new ArrayList<>();

        String[] proxyLines = file.split("\n");

        Arrays.stream(proxyLines)
                .filter(line -> !line.isBlank())
                .distinct()
                .map(this::fromString)
                .forEach(newProxies::add);

        this.proxies.addAll(newProxies);
        serverWrecker.getLogger().info("Loaded {} proxies!", newProxies.size());
    }

    private SWProxy fromString(String proxy) {
        proxy = proxy.trim();

        String[] split = proxy.split(":");

        String host = split[0];
        int port = Integer.parseInt(split[1]);

        // TODO: Make this more dynamic
        if (split.length > 3) {
            return new SWProxy(ProxyType.SOCKS5, new InetSocketAddress(host, port), split[2], split[3]);
        } else {
            return new SWProxy(ProxyType.SOCKS5, new InetSocketAddress(host, port), null, null);
        }
    }

    public List<SWProxy> getProxies() {
        return Collections.unmodifiableList(proxies);
    }

    @Override
    public void onSettingsChange(ProxyList settings) {
        proxies.clear();
        proxies.addAll(settings.proxies());
    }

    @Override
    public ProxyList collectSettings() {
        return new ProxyList(List.copyOf(proxies));
    }
}
