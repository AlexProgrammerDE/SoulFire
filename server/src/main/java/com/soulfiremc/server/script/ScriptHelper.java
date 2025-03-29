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

import com.google.gson.JsonObject;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class ScriptHelper {
  public static MetaLanguage getMetaLanguage(Context context) {
    var languageId = context.getEngine().getLanguages().keySet().stream().findFirst().orElseThrow(
      () -> new IllegalStateException("No language found in context"));

    return MetaLanguage.fromId(languageId).orElseThrow(
      () -> new IllegalStateException("No language found for id: " + languageId));
  }

  public static Value jsonToValue(Context context, JsonObject value) {
    var metaLanguage = getMetaLanguage(context);
    return context.eval(metaLanguage.languageId(), switch (metaLanguage) {
      case JAVASCRIPT -> "JSON.parse";
      case PYTHON -> "json.loads";
    }).execute(value.toString());
  }
}
