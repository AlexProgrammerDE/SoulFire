/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.util.log4j;

import com.soulfiremc.server.util.structs.ShutdownManager;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

final class GenericTerminalConsoleTest {
  private final ShutdownManager shutdownManager = mock();
  private final GenericTerminalConsole.CommandExecutor executor = mock();
  private final GenericTerminalConsole console = new GenericTerminalConsole(shutdownManager, executor, (_, _) -> List.of(), Path.of("."));
  private final LineReader reader = mock();

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void endOfInputDetachesWithoutShuttingDown(boolean throwsEndOfFile) {
    var command = UUID.randomUUID().toString();
    var input = when(reader.readLine(anyString())).thenReturn(" ", " " + command + " ");
    if (throwsEndOfFile) {
      input.thenThrow(new EndOfFileException()).thenThrow(new AssertionError());
    } else {
      input.thenReturn(null).thenThrow(new AssertionError());
    }

    console.readCommands(reader);

    verify(reader, times(3)).readLine(anyString());
    verify(executor).execute(command);
    verify(shutdownManager, never()).shutdownSoftware(anyBoolean());
  }

  @Test
  void userInterruptStillShutsDownTheServer() {
    when(reader.readLine(anyString())).thenThrow(new UserInterruptException(null));

    console.readCommands(reader);

    verify(shutdownManager).shutdownSoftware(true);
    verifyNoInteractions(executor);
  }
}
