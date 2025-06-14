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
package com.soulfiremc.dedicated;

import com.soulfiremc.launcher.SoulFireAbstractBootstrap;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.command.CommandSourceStack;
import com.soulfiremc.server.command.ConsoleCommandSource;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.util.PortHelper;
import com.soulfiremc.server.util.SFPathConstants;
import com.soulfiremc.server.util.log4j.GenericTerminalConsole;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public final class SoulFireDedicatedBootstrap extends SoulFireAbstractBootstrap {
  private SoulFireDedicatedBootstrap() {
    super();
  }

  @SuppressWarnings("unused")
  public static void bootstrap(String[] args) {
    new SoulFireDedicatedBootstrap().internalBootstrap(args);
  }

  @Override
  protected void postMixinMain(String[] args) {
    var host = SoulFireAbstractBootstrap.getRPCHost("0.0.0.0");
    var port = SoulFireAbstractBootstrap.getRPCPort(PortHelper.SF_DEFAULT_PORT);

    GenericTerminalConsole.setupStreams();
    var soulFire =
      new SoulFireServer(host, port, SoulFireAbstractBootstrap.START_TIME, getBaseDirectory());

    if (soulFire.authSystem().rootUserData().email().equals(AuthSystem.ROOT_DEFAULT_EMAIL)) {
      log.info("The root users email is '{}', please change it using the command 'set-email <email>', you can login with the client using that email", AuthSystem.ROOT_DEFAULT_EMAIL);
    }

    var commandManager = soulFire.serverCommandManager();
    var commandSource = new ConsoleCommandSource(soulFire.authSystem());
    new GenericTerminalConsole(
      soulFire.shutdownManager(),
      command -> commandManager.execute(command, CommandSourceStack.ofUnrestricted(soulFire, commandSource)),
      (command, cursor) -> commandManager.complete(command, cursor, CommandSourceStack.ofUnrestricted(soulFire, commandSource)),
      SFPathConstants.WORKING_DIRECTORY
    ).start();

    soulFire.shutdownManager().awaitShutdown();
  }

  @Override
  protected Path getBaseDirectory() {
    return SFPathConstants.WORKING_DIRECTORY;
  }
}
