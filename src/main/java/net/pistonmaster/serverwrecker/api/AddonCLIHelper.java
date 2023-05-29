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
package net.pistonmaster.serverwrecker.api;

import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import picocli.CommandLine;

public class AddonCLIHelper {
    public static <T extends SettingsObject> void registerCommands(CommandLine commandLine, Class<T> clazz, SettingsProvider<T> command) {
        CommandLine.Model.CommandSpec sourceCommandSpec = CommandLine.Model.CommandSpec.forAnnotatedObject(command);
        CommandLine.Model.CommandSpec targetCommandSpec = commandLine.getCommandSpec();

        // Inject the command spec into the main command spec
        for (CommandLine.Model.OptionSpec optionSpec : sourceCommandSpec.options()) {
            targetCommandSpec.addOption(optionSpec);
        }

        ServerWreckerAPI.getServerWrecker().getSettingsManager().registerProvider(clazz, command);
    }
}
