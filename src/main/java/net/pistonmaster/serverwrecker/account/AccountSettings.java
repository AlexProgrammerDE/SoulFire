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
package net.pistonmaster.serverwrecker.account;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.pistonmaster.serverwrecker.server.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.server.settings.lib.property.Property;
import net.pistonmaster.serverwrecker.server.settings.lib.property.StringProperty;

@NoArgsConstructor(access = AccessLevel.NONE)
public class AccountSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("account");
    public static final StringProperty NAME_FORMAT = BUILDER.ofString(
            "nameFormat",
            "Name format",
            "The format of the bot names. %d will be replaced with the bot number.",
            new String[]{"--name-format"},
            "Bot_%d"
    );
    public static final BooleanProperty SHUFFLE_ACCOUNTS = BUILDER.ofBoolean(
            "shuffleAccounts",
            "Shuffle accounts",
            "Should the accounts be shuffled?",
            new String[]{"--shuffle-accounts"},
            false
    );
}
