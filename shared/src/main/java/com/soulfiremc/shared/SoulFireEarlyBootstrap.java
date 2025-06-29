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
package com.soulfiremc.shared;

import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.AnsiConsole;

@SuppressWarnings("unused")
@Slf4j
public class SoulFireEarlyBootstrap {
  public static void earlyBootstrap() {
    SFInfoPlaceholder.register();

    // Install the Log4J JUL bridge
    org.apache.logging.log4j.jul.LogManager.getLogManager().reset();

    SFLogAppender.INSTANCE.start();

    AnsiConsole.systemInstall();
  }
}
