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
package com.soulfiremc.console;

import com.soulfiremc.server.util.structs.ShutdownManager;
import lombok.RequiredArgsConstructor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.jetbrains.annotations.Nullable;
import org.jline.reader.*;

import java.nio.file.Path;
import java.util.List;

@RequiredArgsConstructor
public class GenericTerminalConsole extends SimpleTerminalConsole {
  private static final Logger logger = LogManager.getLogger("SoulFireConsole");
  private final ShutdownManager shutdownManager;
  private final CommandExecutor commandExecutor;
  private final CommandCompleter commandCompleter;
  private final Path historyDirectory;

  /**
   * Sets up {@code System.out} and {@code System.err} to redirect to log4j.
   */
  public static void setupStreams() {
    System.setOut(IoBuilder.forLogger(logger).setLevel(Level.INFO).buildPrintStream());
    System.setErr(IoBuilder.forLogger(logger).setLevel(Level.ERROR).buildPrintStream());
  }

  private static Candidate toCandidate(Completion completion) {
    var suggestionText = completion.suggestion();
    return new Candidate(
      suggestionText,
      suggestionText,
      null,
      completion.tooltip(),
      null,
      null,
      false
    );
  }

  @Override
  protected boolean isRunning() {
    return !shutdownManager.shutdown();
  }

  @Override
  protected void shutdown() {
    shutdownManager.shutdownSoftware(true);
  }

  @Override
  protected void runCommand(String command) {
    commandExecutor.execute(command);
  }

  @Override
  protected LineReader buildReader(LineReaderBuilder builder) {
    return super.buildReader(
      builder
        .appName("SoulFire")
        .variable(LineReader.HISTORY_FILE, historyDirectory.resolve(".console_history"))
        .completer(new TerminalCompleter(commandCompleter)));
  }

  public interface CommandExecutor {
    int execute(String command);
  }

  public interface CommandCompleter {
    Iterable<Completion> complete(String line, int cursor);
  }

  public record Completion(String suggestion, @Nullable String tooltip) {
  }

  private record TerminalCompleter(CommandCompleter commandCompleter) implements Completer {
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
      for (var suggestion : commandCompleter.complete(line.line(), line.cursor())) {
        if (suggestion.suggestion().isEmpty()) {
          continue;
        }

        candidates.add(toCandidate(suggestion));
      }
    }
  }
}
