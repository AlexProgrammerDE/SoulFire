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
package com.soulfiremc.server.util.log4j;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.stream.Collectors;

@Plugin(name = "soulfire", category = StrLookup.CATEGORY)
public class SFInfoPlaceholder implements StrLookup {
  @Override
  public @Nullable String lookup(String key) {
    return null;
  }

  @Override
  public @Nullable String lookup(LogEvent event, String key) {
    if (key.equals("context_info")) {
      var builder = new StringBuilder();

      var contextData = event.getContextData();
      var usedData = new HashMap<>();
      if (contextData.containsKey(SFLogAppender.SF_INSTANCE_NAME)) {
        usedData.put("instance", contextData.getValue(SFLogAppender.SF_INSTANCE_NAME));
      }
      if (contextData.containsKey(SFLogAppender.SF_BOT_ACCOUNT_NAME)) {
        usedData.put("bot", contextData.getValue(SFLogAppender.SF_BOT_ACCOUNT_NAME));
      }

      if (!usedData.isEmpty()) {
        builder.append(" {");
        builder.append(usedData
          .entrySet()
          .stream()
          .filter(entry -> entry.getValue() != null)
          .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
          .collect(Collectors.joining(", ")));
        builder.append("}");
      }

      return builder.toString();
    } else if (key.equals("context_info_bot_only")) {
      var builder = new StringBuilder();

      var contextData = event.getContextData();
      var usedData = new HashMap<>();
      if (contextData.containsKey(SFLogAppender.SF_BOT_ACCOUNT_NAME)) {
        usedData.put("bot", contextData.getValue(SFLogAppender.SF_BOT_ACCOUNT_NAME));
      }

      if (!usedData.isEmpty()) {
        builder.append(" {");
        builder.append(usedData
          .entrySet()
          .stream()
          .filter(entry -> entry.getValue() != null)
          .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
          .collect(Collectors.joining(", ")));
        builder.append("}");
      }

      return builder.toString();
    } else {
      return null;
    }
  }
}
