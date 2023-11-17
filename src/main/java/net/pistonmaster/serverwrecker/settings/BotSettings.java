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

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.pistonmaster.serverwrecker.SWConstants;
import net.pistonmaster.serverwrecker.settings.lib.property.IntProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.Property;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.property.StringProperty;

public record BotSettings(String host, int port,
                          int amount, int minJoinDelayMs, int maxJoinDelayMs,
                          ProtocolVersion protocolVersion,
                          int readTimeout, int writeTimeout, int connectTimeout,
                          boolean trySrv, int concurrentConnects) implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("bot");
    public static final StringProperty HOST = BUILDER.of("host", "Host", "Host to connect to", "Host to connect to", new String[]{"--host"}, "127.0.0.1");
    public static final IntProperty PORT = BUILDER.of("port", "Port", "Port to connect to", "Port to connect to", new String[]{"--port"}, 25565);
    public static final int DEFAULT_PORT = 25565;
    public static final int DEFAULT_AMOUNT = 1;
    public static final int DEFAULT_MIN_JOIN_DELAY_MS = 1000;
    public static final int DEFAULT_MAX_JOIN_DELAY_MS = 3000;
    public static final ProtocolVersion DEFAULT_PROTOCOL_VERSION = SWConstants.CURRENT_PROTOCOL_VERSION;
    public static final int DEFAULT_READ_TIMEOUT = 30;
    public static final int DEFAULT_WRITE_TIMEOUT = 0;
    public static final int DEFAULT_CONNECT_TIMEOUT = 30;
    public static final boolean DEFAULT_TRY_SRV = true;
    public static final int DEFAULT_CONCURRENT_CONNECTS = 1;
}
