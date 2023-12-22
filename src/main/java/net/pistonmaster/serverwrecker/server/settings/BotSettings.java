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
package net.pistonmaster.serverwrecker.server.settings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.pistonmaster.serverwrecker.server.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.server.settings.lib.property.ComboProperty;
import net.pistonmaster.serverwrecker.server.settings.lib.property.IntProperty;
import net.pistonmaster.serverwrecker.server.settings.lib.property.MinMaxPropertyLink;
import net.pistonmaster.serverwrecker.server.settings.lib.property.Property;
import net.pistonmaster.serverwrecker.server.settings.lib.property.StringProperty;
import net.pistonmaster.serverwrecker.server.viaversion.SWVersionConstants;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BotSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("bot");
    public static final StringProperty ADDRESS = BUILDER.ofString(
            "address",
            "Address",
            new String[]{"--address"},
            "Address to connect to",
            "127.0.0.1:25565"
    );
    public static final IntProperty AMOUNT = BUILDER.ofInt(
            "amount",
            "Amount",
            new String[]{"--amount"},
            "Amount of bots to connect",
            1,
            1,
            Integer.MAX_VALUE,
            1
    );
    public static final MinMaxPropertyLink JOIN_DELAY = new MinMaxPropertyLink(
            BUILDER.ofInt(
                    "join-min-delay",
                    "Min Join Delay (ms)",
                    new String[]{"--join-min-delay"},
                    "Minimum delay between joins in milliseconds",
                    1000,
                    0,
                    Integer.MAX_VALUE,
                    1
            ),
            BUILDER.ofInt(
                    "join-max-delay",
                    "Max Join Delay (ms)",
                    new String[]{"--join-max-delay"},
                    "Maximum delay between joins in milliseconds",
                    3000,
                    0,
                    Integer.MAX_VALUE,
                    1
            )
    );
    public static final ComboProperty PROTOCOL_VERSION = BUILDER.ofCombo(
            "protocol-version",
            "Protocol Version",
            new String[]{"--protocol-version"},
            "Minecraft protocol version to use",
            getProtocolVersionOptions(),
            0
    );
    public static final IntProperty READ_TIMEOUT = BUILDER.ofInt(
            "read-timeout",
            "Read Timeout",
            new String[]{"--read-timeout"},
            "Read timeout in seconds",
            30,
            0,
            Integer.MAX_VALUE,
            1
    );
    public static final IntProperty WRITE_TIMEOUT = BUILDER.ofInt(
            "write-timeout",
            "Write Timeout",
            new String[]{"--write-timeout"},
            "Write timeout in seconds",
            0,
            0,
            Integer.MAX_VALUE,
            1
    );
    public static final IntProperty CONNECT_TIMEOUT = BUILDER.ofInt(
            "connect-timeout",
            "Connect Timeout",
            new String[]{"--connect-timeout"},
            "Connect timeout in seconds",
            30,
            0,
            Integer.MAX_VALUE,
            1
    );
    public static final BooleanProperty RESOLVE_SRV = BUILDER.ofBoolean(
            "resolve-srv",
            "Resolve SRV",
            new String[]{"--resolve-srv"},
            "Try to resolve SRV records from the address",
            true
    );
    public static final IntProperty CONCURRENT_CONNECTS = BUILDER.ofInt(
            "concurrent-connects",
            "Concurrent Connects",
            new String[]{"--concurrent-connects"},
            "Max amount of bots attempting to connect at once",
            1,
            1,
            Integer.MAX_VALUE,
            1
    );

    private static ComboProperty.ComboOption[] getProtocolVersionOptions() {
        return SWVersionConstants.getVersionsSorted().stream().map(version -> {
            String displayName;
            if (SWVersionConstants.isBedrock(version)) {
                displayName = String.format("%s (%s)", version.getName(), version.getVersion() - 1_000_000);
            } else if (SWVersionConstants.isLegacy(version)) {
                displayName = String.format("%s (%s)", version.getName(), Math.abs(version.getVersion()) >> 2);
            } else {
                displayName = version.toString();
            }

            return new ComboProperty.ComboOption(version.getName(), displayName);
        }).toArray(ComboProperty.ComboOption[]::new);
    }
}
