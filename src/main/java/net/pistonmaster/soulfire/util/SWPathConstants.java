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
package net.pistonmaster.soulfire.util;

import net.harawata.appdirs.AppDirsFactory;

import java.nio.file.Path;

public class SWPathConstants {
    public static final Path WORKING_DIRECTORY = Path.of(System.getProperty("user.dir"));
    public static final Path DATA_FOLDER = getApplicationDataFolder();
    public static final Path PLUGINS_FOLDER = DATA_FOLDER.resolve("plugins");
    public static final Path CONFIG_FOLDER = DATA_FOLDER.resolve("config");
    public static final Path PROFILES_FOLDER = DATA_FOLDER.resolve("profiles");

    private static Path getApplicationDataFolder() {
        return Path.of(AppDirsFactory.getInstance().getUserDataDir("SoulFire", null, null));
    }
}
