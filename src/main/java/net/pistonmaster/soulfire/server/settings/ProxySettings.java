/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.server.settings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.pistonmaster.soulfire.server.settings.lib.SettingsObject;
import net.pistonmaster.soulfire.server.settings.lib.property.IntProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.Property;
import net.pistonmaster.soulfire.util.BuiltinSettingsConstants;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProxySettings implements SettingsObject {
    private static final Property.Builder builder = Property.builder(BuiltinSettingsConstants.PROXY_SETTINGS_ID);
    public static final IntProperty BOTS_PER_PROXY = builder.ofInt(
            "bots-per-proxy",
            "Bots per proxy",
            new String[]{"--bots-per-proxy"},
            "Amount of bots that can be on a single proxy",
            -1,
            -1,
            Integer.MAX_VALUE,
            1
    );
}
