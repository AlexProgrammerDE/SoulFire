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
package net.pistonmaster.soulfire.server.protocol.netty;

import com.google.common.net.HostAndPort;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.server.settings.BotSettings;
import net.pistonmaster.soulfire.server.settings.lib.SettingsHolder;

import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.IDN;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Optional;

@Slf4j
public class ResolveUtil {
    private static final DirContext DIR_CONTEXT;

    static {
        try {
            var contextFactory = "com.sun.jndi.dns.DnsContextFactory";
            Class.forName(contextFactory);
            var environment = new Hashtable<String, String>();
            environment.put("java.naming.factory.initial", contextFactory);
            environment.put("java.naming.provider.url", "dns:");
            environment.put("com.sun.jndi.dns.timeout.retries", "1");
            DIR_CONTEXT = new InitialDirContext(environment);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ResolveUtil() {
    }

    public static Optional<ResolvedAddress> resolveAddress(boolean isBedrock, SettingsHolder settingsHolder, EventLoopGroup eventLoopGroup) {
        var serverAddress = new ServerAddress(settingsHolder.get(BotSettings.ADDRESS));

        if (settingsHolder.get(BotSettings.RESOLVE_SRV) && serverAddress.port() == 25565 && !isBedrock) {
            // SRVs can override address on Java, but not Bedrock.
            var resolved = resolveSrv(serverAddress, eventLoopGroup);
            if (resolved.isPresent()) {
                return resolved;
            }
        } else {
            log.debug("Not resolving SRV record for {}", serverAddress.host());
        }

        return resolveByHost(serverAddress)
                .map(e -> new ResolvedAddress(serverAddress, e));
    }

    private static Optional<ResolvedAddress> resolveSrv(ServerAddress serverAddress, EventLoopGroup eventLoopGroup) {
        var name = "_minecraft._tcp." + serverAddress.host();
        log.debug("Attempting SRV lookup for \"{}\".", name);

        try {
            var srvAttribute = DIR_CONTEXT.getAttributes(name, new String[]{"SRV"})
                    .get("srv");
            if (srvAttribute != null) {
                var attributeSplit = srvAttribute.get().toString().split(" ", 4);
                log.debug("SRV lookup resolved \"{}\" to \"{}\".", name, srvAttribute.get().toString());

                return resolveByHost(new ServerAddress(attributeSplit[3], parsePort(attributeSplit[2])))
                        .map(e -> new ResolvedAddress(serverAddress, e));
            } else {
                log.debug("SRV lookup for \"{}\" returned no records.", name);
            }
        } catch (Exception e) {
            log.debug("Failed to resolve SRV record.", e);
        }

        return Optional.empty();
    }

    private static Optional<InetSocketAddress> resolveByHost(ServerAddress serverAddress) {
        try {
            var host = serverAddress.host();
            var resolved = InetAddress.getByName(host);
            log.debug("Resolved {} -> {}", host, resolved.getHostAddress());
            return Optional.of(new InetSocketAddress(resolved, serverAddress.port()));
        } catch (UnknownHostException e) {
            log.debug("Failed to resolve host.", e);
            return Optional.empty();
        }
    }

    private static int parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return 25565;
        }
    }

    public record ServerAddress(HostAndPort hostAndPort) {
        public ServerAddress(String address) {
            this(HostAndPort.fromString(address).withDefaultPort(25565));
        }

        public ServerAddress(String host, int port) {
            this(HostAndPort.fromParts(host, port));
        }

        public String host() {
            try {
                return IDN.toASCII(hostAndPort.getHost());
            } catch (IllegalArgumentException e) {
                return "";
            }
        }

        public int port() {
            return hostAndPort.getPort();
        }
    }

    public record ResolvedAddress(ServerAddress originalAddress, InetSocketAddress resolvedAddress) {
    }
}
