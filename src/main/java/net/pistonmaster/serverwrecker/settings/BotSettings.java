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
package net.pistonmaster.serverwrecker.settings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.pistonmaster.serverwrecker.SWConstants;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.property.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BotSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("bot");
    public static final StringProperty HOST = BUILDER.ofString(
            "host",
            "Host",
            "Host to connect to",
            new String[]{"--host"},
            "127.0.0.1"
    );
    public static final IntProperty PORT = BUILDER.ofInt(
            "port",
            "Port",
            "Port to connect to",
            new String[]{"--port"},
            25565
    );
    public static final IntProperty AMOUNT = BUILDER.ofInt(
            "amount",
            "Amount",
            "Amount of bots to connect",
            new String[]{"--amount"},
            1
    );
    public static final IntProperty MIN_JOIN_DELAY_MS = BUILDER.ofInt(
            "minJoinDelayMs",
            "Min Join Delay",
            "Minimum delay between joins in milliseconds",
            new String[]{"--minJoinDelayMs"},
            1000
    );
    public static final IntProperty MAX_JOIN_DELAY_MS = BUILDER.ofInt(
            "maxJoinDelayMs",
            "Max Join Delay",
            "Maximum delay between joins in milliseconds",
            new String[]{"--maxJoinDelayMs"},
            3000
    );
    public static final MinMaxPropertyLink JOIN_DELAY_MS = new MinMaxPropertyLink(MIN_JOIN_DELAY_MS, MAX_JOIN_DELAY_MS);
    public static final ComboProperty PROTOCOL_VERSION = BUILDER.ofCombo(
            "protocolVersion",
            "Protocol Version",
            "Protocol version to use",
            new String[]{"--protocolVersion"},
            getProtocolVersionOptions(),
            0
    );
    public static final IntProperty READ_TIMEOUT = BUILDER.ofInt(
            "readTimeout",
            "Read Timeout",
            "Read timeout in seconds",
            new String[]{"--readTimeout"},
            30
    );
    public static final IntProperty WRITE_TIMEOUT = BUILDER.ofInt(
            "writeTimeout",
            "Write Timeout",
            "Write timeout in seconds",
            new String[]{"--writeTimeout"},
            0
    );
    public static final IntProperty CONNECT_TIMEOUT = BUILDER.ofInt(
            "connectTimeout",
            "Connect Timeout",
            "Connect timeout in seconds",
            new String[]{"--connectTimeout"},
            30
    );
    public static final BooleanProperty TRY_SRV = BUILDER.ofBoolean(
            "trySrv",
            "Try SRV",
            "Try to use SRV records",
            new String[]{"--trySrv"},
            true
    );
    public static final IntProperty CONCURRENT_CONNECTS = BUILDER.ofInt(
            "concurrentConnects",
            "Concurrent Connects",
            "Amount of concurrent connects",
            new String[]{"--concurrentConnects"},
            1
    );

    private static ComboProperty.ComboOption[] getProtocolVersionOptions() {
        return SWConstants.getVersionsSorted().stream().map(version -> {
            String displayName;
            if (SWConstants.isBedrock(version)) {
                displayName = String.format("%s (%s)", version.getName(), version.getVersion() - 1_000_000);
            } else if (SWConstants.isLegacy(version)) {
                displayName = String.format("%s (%s)", version.getName(), Math.abs(version.getVersion()) >> 2);
            } else {
                displayName = version.toString();
            }

            return new ComboProperty.ComboOption(version.getName(), displayName);
        }).toArray(ComboProperty.ComboOption[]::new);
    }
}
