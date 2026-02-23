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

import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/// Handles migration of script node/edge JSON between schema versions.
/// When the node/edge format changes, add a new migration step here
/// and increment CURRENT_VERSION.
@Slf4j
public final class ScriptSchemaMigration {
  /// The current schema version. Increment when making breaking changes
  /// to the node/edge JSON format.
  public static final int CURRENT_VERSION = 1;

  /// Migrates nodes and edges from an older schema version to the current one.
  /// Returns the migrated data. If already at current version, returns unchanged.
  ///
  /// @param fromVersion the schema version of the data
  /// @param nodes       the raw node data
  /// @param edges       the raw edge data
  /// @return migrated node and edge data
  public static MigrationResult migrate(
    int fromVersion,
    List<Map<String, JsonElement>> nodes,
    List<Map<String, JsonElement>> edges
  ) {
    if (fromVersion >= CURRENT_VERSION) {
      return new MigrationResult(nodes, edges, false);
    }

    log.info("Migrating script schema from version {} to {}", fromVersion, CURRENT_VERSION);

    var currentNodes = nodes;
    var currentEdges = edges;

    // Add migration steps here as needed:
    // if (fromVersion < 2) { currentNodes = migrateV1toV2(currentNodes); }

    return new MigrationResult(currentNodes, currentEdges, true);
  }

  /// Result of a schema migration.
  ///
  /// @param nodes    the migrated nodes
  /// @param edges    the migrated edges
  /// @param migrated whether any migration was actually performed
  public record MigrationResult(
    List<Map<String, JsonElement>> nodes,
    List<Map<String, JsonElement>> edges,
    boolean migrated
  ) {}

  private ScriptSchemaMigration() {}
}
