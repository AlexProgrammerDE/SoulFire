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
package com.soulfiremc.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.soulfiremc.brigadier.CommandHelpWrapper;
import com.soulfiremc.brigadier.CommandSource;

public class ClientBrigadierHelper {
  private ClientBrigadierHelper() {}

  public static LiteralArgumentBuilder<CommandSource> literal(String name) {
    return LiteralArgumentBuilder.literal(name);
  }

  public static Command<CommandSource> help(String help, Command<CommandSource> command) {
    return new CommandHelpWrapper<>(command, help, false);
  }
}
