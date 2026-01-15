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
package com.soulfiremc.server.script;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;

@Getter
@RequiredArgsConstructor
public enum ScriptLanguage {
  TYPESCRIPT(MetaLanguage.JAVASCRIPT, "main.ts", "main.js"),
  JAVASCRIPT(MetaLanguage.JAVASCRIPT, "main.js", "main.js"),
  PYTHON(MetaLanguage.PYTHON, "main.py", "main.py");

  public static final ScriptLanguage DEFAULT = JAVASCRIPT;
  public static final ScriptLanguage[] VALUES = values();

  private final MetaLanguage metaLanguage;
  private final String detectFile;
  private final String entryFile;

  public static ScriptLanguage determineLanguage(Path codePath) {
    for (var language : VALUES) {
      var detectFile = codePath.resolve(language.detectFile);
      if (Files.exists(detectFile) && Files.isRegularFile(detectFile)) {
        return language;
      }
    }

    return DEFAULT;
  }
}
