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
package com.soulfiremc.server.script;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;

@Getter
@RequiredArgsConstructor
public enum ScriptLanguage {
  JAVASCRIPT("js", "main.js"),
  PYTHON("python", "main.py");

  public static final ScriptLanguage DEFAULT = JAVASCRIPT;
  public static final ScriptLanguage[] VALUES = values();

  private final String languageId;
  private final String entryFile;

  public static ScriptLanguage determineLanguage(Path codePath) {
    for (var language : VALUES) {
      if (Files.exists(codePath.resolve(language.entryFile))
        && Files.isRegularFile(codePath.resolve(language.entryFile))) {
        return language;
      }
    }

    return DEFAULT;
  }
}
