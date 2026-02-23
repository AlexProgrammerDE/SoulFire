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
package com.soulfiremc.server.script.nodes.variable;

import com.soulfiremc.server.api.metadata.MetadataKey;
import com.soulfiremc.server.script.NodeValue;

import java.util.concurrent.ConcurrentHashMap;

/// Shared MetadataKey constants for session variable nodes.
public final class SessionVariableKeys {
  /// Key for bot-scoped session variables (stored in bot metadata, lost on disconnect).
  public static final MetadataKey<ConcurrentHashMap<String, NodeValue>> SESSION_BOT_VARS_KEY =
    MetadataKey.of("script_session", "variables", ConcurrentHashMap.class);

  /// Key for instance-scoped session variables (stored in instance metadata, lost on restart).
  public static final MetadataKey<ConcurrentHashMap<String, NodeValue>> SESSION_INSTANCE_VARS_KEY =
    MetadataKey.of("script_session", "instance_variables", ConcurrentHashMap.class);

  private SessionVariableKeys() {}
}
