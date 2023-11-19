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
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.Property;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DevSettings implements SettingsObject {
    public static final Property.Builder BUILDER = Property.builder("dev");
    public static final BooleanProperty VIA_DEBUG = BUILDER.of(
            "viaDebug",
            "Via Debug",
            "Enable Via debug",
            "Enable Via debug",
            new String[]{"--viaDebug"},
            false
    );
    public static final BooleanProperty NETTY_DEBUG = BUILDER.of(
            "nettyDebug",
            "Netty Debug",
            "Enable Netty debug",
            "Enable Netty debug",
            new String[]{"--nettyDebug"},
            false
    );
    public static final BooleanProperty GRPC_DEBUG = BUILDER.of(
            "grpcDebug",
            "GRPC Debug",
            "Enable GRPC debug",
            "Enable GRPC debug",
            new String[]{"--grpcDebug"},
            false
    );
    public static final BooleanProperty CORE_DEBUG = BUILDER.of(
            "coreDebug",
            "Core Debug",
            "Enable Core debug",
            "Enable Core debug",
            new String[]{"--coreDebug"},
            false
    );
}
