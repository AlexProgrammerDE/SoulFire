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
import net.pistonmaster.serverwrecker.server.settings.lib.property.Property;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DevSettings implements SettingsObject {
    public static final Property.Builder BUILDER = Property.builder("dev");
    public static final BooleanProperty VIA_DEBUG = BUILDER.ofBoolean(
            "viaDebug",
            "Via Debug",
            "Enable Via debug",
            new String[]{"--via-debug"},
            false
    );
    public static final BooleanProperty NETTY_DEBUG = BUILDER.ofBoolean(
            "nettyDebug",
            "Netty Debug",
            "Enable Netty debug",
            new String[]{"--netty-debug"},
            false
    );
    public static final BooleanProperty GRPC_DEBUG = BUILDER.ofBoolean(
            "grpcDebug",
            "GRPC Debug",
            "Enable GRPC debug",
            new String[]{"--grpc-debug"},
            false
    );
    public static final BooleanProperty CORE_DEBUG = BUILDER.ofBoolean(
            "coreDebug",
            "Core Debug",
            "Enable Core debug",
            new String[]{"--core-debug"},
            false
    );
}
