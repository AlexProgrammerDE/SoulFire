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
package com.soulfiremc.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandHistoryManager {
  private final Path historyFile;
  private final List<Map.Entry<Instant, String>> commandHistory = new ArrayList<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  public CommandHistoryManager(Path baseDirectory) {
    this.historyFile = baseDirectory.resolve(".command_history");

    loadCommandHistory();
  }

  public void loadCommandHistory() {
    lock.writeLock().lock();
    try {
      commandHistory.clear();

      if (!Files.exists(historyFile)) {
        return;
      }

      var lines = Files.readAllLines(historyFile);
      for (var line : lines) {
        var firstColon = line.indexOf(':');
        if (firstColon == -1) {
          continue;
        }

        var seconds = Long.parseLong(line.substring(0, firstColon));
        var command = line.substring(firstColon + 1);

        commandHistory.add(Map.entry(Instant.ofEpochSecond(seconds), command));
      }
    } catch (IOException e) {
      log.error("Failed to create command history file!", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void newCommandHistoryEntry(String command) {
    lock.writeLock().lock();
    try {
      Files.createDirectories(historyFile.getParent());
      var newLine = Instant.now().getEpochSecond() + ":" + command + System.lineSeparator();
      Files.writeString(
        historyFile, newLine, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException e) {
      log.error("Failed to create command history file!", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void clearCommandHistory() {
    lock.writeLock().lock();
    try {
      Files.deleteIfExists(historyFile);
      commandHistory.clear();
    } catch (IOException e) {
      log.error("Failed to delete command history file!", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public List<Map.Entry<Instant, String>> getCommandHistory() {
    lock.readLock().lock();
    try {
      return new ArrayList<>(commandHistory);
    } finally {
      lock.readLock().unlock();
    }
  }
}
