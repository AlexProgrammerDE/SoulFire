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

import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.dns.*;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import net.pistonmaster.serverwrecker.server.settings.BotSettings;
import net.pistonmaster.serverwrecker.server.settings.lib.SettingsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.regex.Pattern;

public class ResolveUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResolveUtil.class);
    private static final Pattern IP_REGEX = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");

    private ResolveUtil() {
    }

    public static InetSocketAddress resolveAddress(boolean isBedrock, SettingsHolder settingsHolder, EventLoopGroup eventLoopGroup) {
        var host = settingsHolder.get(BotSettings.HOST);
        var port = settingsHolder.get(BotSettings.PORT);

        if (!isBedrock && settingsHolder.get(BotSettings.TRY_SRV)) {
            var resolved = resolveSrv(host, eventLoopGroup);
            if (resolved.isPresent()) {
                return resolved.get();
            }
        } else {
            LOGGER.debug("Not resolving SRV record for {}", host);
        }

        return resolveByHost(host, port);
    }

    private static Optional<InetSocketAddress> resolveSrv(String host, EventLoopGroup eventLoopGroup) {
        if (IP_REGEX.matcher(host).matches() || host.equalsIgnoreCase("localhost")) {
            LOGGER.debug("Not a valid domain: {}", host);
            return Optional.empty();
        }

        var name = "_minecraft._tcp." + host;
        LOGGER.debug("Attempting SRV lookup for \"{}\".", name);

        try (var resolver = new DnsNameResolverBuilder(eventLoopGroup.next())
                .channelType(SWNettyHelper.DATAGRAM_CHANNEL_CLASS)
                .build()) {
            var envelope = resolver.query(new DefaultDnsQuestion(name, DnsRecordType.SRV)).get();

            var response = envelope.content();
            if (response.count(DnsSection.ANSWER) == 0) {
                LOGGER.debug("No SRV record found.");
                return Optional.empty();
            }

            DefaultDnsRawRecord record = response.recordAt(DnsSection.ANSWER, 0);
            if (record.type() != DnsRecordType.SRV) {
                LOGGER.debug("Received non-SRV record in response.");
                return Optional.empty();
            }

            var buf = record.content();
            buf.skipBytes(4); // Skip priority and weight.

            var port = buf.readUnsignedShort();
            host = DefaultDnsRecordDecoder.decodeName(buf);
            if (host.endsWith(".")) {
                host = host.substring(0, host.length() - 1);
            }

            LOGGER.debug("Found SRV record containing \"{}:{}}\".", host, port);

            return Optional.of(new InetSocketAddress(host, port));
        } catch (Exception e) {
            LOGGER.debug("Failed to resolve SRV record.", e);
            return Optional.empty();
        }
    }

    private static InetSocketAddress resolveByHost(String host, int port) {
        try {
            var resolved = InetAddress.getByName(host);
            LOGGER.debug("Resolved {} -> {}", host, resolved.getHostAddress());
            return new InetSocketAddress(resolved, port);
        } catch (UnknownHostException e) {
            LOGGER.debug("Failed to resolve host, letting Netty do it instead.", e);
            return InetSocketAddress.createUnresolved(host, port);
        }
    }
}