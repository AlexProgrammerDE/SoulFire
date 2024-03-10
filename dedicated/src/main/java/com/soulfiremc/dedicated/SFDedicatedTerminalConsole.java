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

import com.soulfiremc.server.ServerCommandManager;
import com.soulfiremc.util.ShutdownManager;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;

@RequiredArgsConstructor
public class SFDedicatedTerminalConsole extends SimpleTerminalConsole {
  private static final Logger logger = LogManager.getLogger("SoulFire");
  private final ShutdownManager shutdownManager;
  private final ServerCommandManager serverCommandManager;

  /** Sets up {@code System.out} and {@code System.err} to redirect to log4j. */
  public static void setupStreams() {
    System.setOut(IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream());
    System.setErr(IoBuilder.forLogger(logger).setLevel(Level.ERROR).buildPrintStream());
  }

  public static void setupTerminalConsole(
      ExecutorService threadPool,
      ShutdownManager shutdownManager,
      ServerCommandManager serverCommandManager) {
    SFDedicatedTerminalConsole.setupStreams();

    if (System.console() == null) {
      logger.debug("No console available, not starting a terminal console.");
      return;
    }

    threadPool.execute(new SFDedicatedTerminalConsole(shutdownManager, serverCommandManager)::start);
  }

  @Override
  protected boolean isRunning() {
    return !shutdownManager.shutdown();
  }

  @Override
  protected void runCommand(String command) {
    serverCommandManager.execute(command);
  }

  @Override
  protected void shutdown() {
    shutdownManager.shutdownSoftware(true);
  }

  @Override
  protected LineReader buildReader(LineReaderBuilder builder) {
    var history = new DefaultHistory();
    for (var command : serverCommandManager.getCommandHistory()) {
      history.add(command.getKey(), command.getValue());
    }

    return super.buildReader(
        builder
            .appName("SoulFire")
            .completer(
                (reader, parsedLine, list) -> {
                  for (var suggestion :
                      serverCommandManager.getCompletionSuggestions(parsedLine.line())) {
                    list.add(new Candidate(suggestion));
                  }
                })
            .history(history));
  }
}
