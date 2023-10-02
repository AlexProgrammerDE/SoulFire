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

import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;

public record DevSettings(boolean viaDebug, boolean nettyDebug, boolean grpcDebug,
                          boolean coreDebug) implements SettingsObject {
    public static final boolean DEFAULT_VIA_DEBUG = false;
    public static final boolean DEFAULT_NETTY_DEBUG = false;
    public static final boolean DEFAULT_GRPC_DEBUG = false;
    public static final boolean DEFAULT_CORE_DEBUG = false;
    public static DevSettings DEFAULT = new DevSettings(DEFAULT_VIA_DEBUG, DEFAULT_NETTY_DEBUG, DEFAULT_GRPC_DEBUG, DEFAULT_CORE_DEBUG);
}
