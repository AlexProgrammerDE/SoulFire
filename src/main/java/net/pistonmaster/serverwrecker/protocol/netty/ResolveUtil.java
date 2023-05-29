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
package net.pistonmaster.serverwrecker.protocol.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.dns.*;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import net.pistonmaster.serverwrecker.common.SWProxy;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import net.pistonmaster.serverwrecker.settings.DevSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsHolder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class ResolveUtil {
    private static final String IP_REGEX = "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b";

    public static InetSocketAddress resolveAddress(SettingsHolder settingsHolder, EventLoopGroup eventLoopGroup, SWProxy proxy) { // TODO: Add proxy support
        BotSettings settings = settingsHolder.get(BotSettings.class);
        String host = settings.host();
        int port = settings.port();

        DevSettings devSettings = settingsHolder.get(DevSettings.class);
        boolean debug = devSettings.debug();

        String name = "_minecraft._tcp." + settings.host();
        if (debug) {
            System.out.println("[PacketLib] Attempting SRV lookup for \"" + name + "\".");
        }

        if (settings.trySrv() && (!host.matches(IP_REGEX) && !host.equalsIgnoreCase("localhost"))) {
            AddressedEnvelope<DnsResponse, InetSocketAddress> envelope = null;
            try (DnsNameResolver resolver = new DnsNameResolverBuilder(eventLoopGroup.next())
                    .channelType(SWNettyHelper.DATAGRAM_CHANNEL_CLASS)
                    .build()) {
                envelope = resolver.query(new DefaultDnsQuestion(name, DnsRecordType.SRV)).get();

                DnsResponse response = envelope.content();
                if (response.count(DnsSection.ANSWER) > 0) {
                    DefaultDnsRawRecord record = response.recordAt(DnsSection.ANSWER, 0);
                    if (record.type() == DnsRecordType.SRV) {
                        ByteBuf buf = record.content();
                        buf.skipBytes(4); // Skip priority and weight.

                        port = buf.readUnsignedShort();
                        host = DefaultDnsRecordDecoder.decodeName(buf);
                        if (host.endsWith(".")) {
                            host = host.substring(0, host.length() - 1);
                        }

                        if (devSettings.debug()) {
                            System.out.println("[PacketLib] Found SRV record containing \"" + host + ":" + port + "\".");
                        }

                    } else if (debug) {
                        System.out.println("[PacketLib] Received non-SRV record in response.");
                    }
                } else if (debug) {
                    System.out.println("[PacketLib] No SRV record found.");
                }
            } catch (Exception e) {
                if (debug) {
                    System.out.println("[PacketLib] Failed to resolve SRV record.");
                    e.printStackTrace();
                }
            } finally {
                if (envelope != null) {
                    envelope.release();
                }

            }
        } else if (debug) {
            System.out.println("[PacketLib] Not resolving SRV record for " + host);
        }

        // Resolve host here
        try {
            InetAddress resolved = InetAddress.getByName(host);
            if (debug) {
                System.out.printf("[PacketLib] Resolved %s -> %s%n", host, resolved.getHostAddress());
            }
            return new InetSocketAddress(resolved, port);
        } catch (UnknownHostException e) {
            if (debug) {
                System.out.println("[PacketLib] Failed to resolve host, letting Netty do it instead.");
                e.printStackTrace();
            }
            return InetSocketAddress.createUnresolved(host, port);
        }
    }
}
