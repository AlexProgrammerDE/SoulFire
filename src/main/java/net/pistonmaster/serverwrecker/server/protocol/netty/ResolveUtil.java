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
package net.pistonmaster.serverwrecker.server.protocol.netty;

import com.google.common.net.HostAndPort;
import io.netty.channel.EventLoopGroup;
import net.pistonmaster.serverwrecker.server.settings.BotSettings;
import net.pistonmaster.serverwrecker.server.settings.lib.SettingsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.IDN;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Optional;

public class ResolveUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResolveUtil.class);
    private static final DirContext DIR_CONTEXT;

    static {
        try {
            String contextFactory = "com.sun.jndi.dns.DnsContextFactory";
            Class.forName(contextFactory);
            Hashtable<String, String> environment = new Hashtable<>();
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

        if (settingsHolder.get(BotSettings.TRY_SRV) && serverAddress.port() == 25565 && !isBedrock) {
            // SRVs can override address on Java, but not Bedrock.
            var resolved = resolveSrv(serverAddress, eventLoopGroup);
            if (resolved.isPresent()) {
                return resolved;
            }
        } else {
            LOGGER.debug("Not resolving SRV record for {}", serverAddress.host());
        }

        return resolveByHost(serverAddress)
                .map(e -> new ResolvedAddress(serverAddress, e));
    }

    private static Optional<ResolvedAddress> resolveSrv(ServerAddress serverAddress, EventLoopGroup eventLoopGroup) {
        var name = "_minecraft._tcp." + serverAddress.host();
        LOGGER.debug("Attempting SRV lookup for \"{}\".", name);

        try {
            Attribute srvAttribute = DIR_CONTEXT.getAttributes(name, new String[]{"SRV"})
                    .get("srv");
            if (srvAttribute != null) {
                String[] attributeSplit = srvAttribute.get().toString().split(" ", 4);
                LOGGER.debug("SRV lookup resolved \"{}\" to \"{}\".", name, srvAttribute.get().toString());

                return resolveByHost(new ServerAddress(attributeSplit[3], parsePort(attributeSplit[2])))
                        .map(e -> new ResolvedAddress(serverAddress, e));
            } else {
                LOGGER.debug("SRV lookup for \"{}\" returned no records.", name);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to resolve SRV record.", e);
        }

        return Optional.empty();
    }

    private static Optional<InetSocketAddress> resolveByHost(ServerAddress serverAddress) {
        try {
            var host = serverAddress.host();
            var resolved = InetAddress.getByName(host);
            LOGGER.debug("Resolved {} -> {}", host, resolved.getHostAddress());
            return Optional.of(new InetSocketAddress(resolved, serverAddress.port()));
        } catch (UnknownHostException e) {
            LOGGER.debug("Failed to resolve host.", e);
            return Optional.empty();
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

    private static int parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return 25565;
        }
    }

    public record ResolvedAddress(ServerAddress originalAddress, InetSocketAddress resolvedAddress) {
    }
}
