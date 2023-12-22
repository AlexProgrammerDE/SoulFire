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
package net.pistonmaster.serverwrecker.proxy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.pistonmaster.serverwrecker.server.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.server.settings.lib.property.IntProperty;
import net.pistonmaster.serverwrecker.server.settings.lib.property.Property;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProxySettings implements SettingsObject {
    private static final Property.Builder builder = Property.builder("proxy");
    public static final IntProperty BOTS_PER_PROXY = builder.ofInt(
            "botsPerProxy",
            "Bots Per Proxy",
            new String[]{"--bots-per-proxy"},
            "Amount of bots that can be on a single proxy",
            -1,
            -1,
            Integer.MAX_VALUE,
            1
    );
}
