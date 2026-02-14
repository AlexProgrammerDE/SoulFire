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
package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class V2__repair_baselined_schema extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    var connection = context.getConnection();
    var product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
    if (product.contains("sqlite")) {
      repairSqliteSchema(connection);
      return;
    }

    ensureScriptsTable(connection);
  }

  private static void repairSqliteSchema(Connection connection) throws SQLException {
    execute(connection, "PRAGMA foreign_keys = OFF");
    try {
      repairUsers(connection);
      repairInstances(connection);
      repairInstanceAuditLogs(connection);
      repairServerConfig(connection);
      repairScripts(connection);
    } finally {
      execute(connection, "PRAGMA foreign_keys = ON");
    }
  }

  private static void repairUsers(Connection connection) throws SQLException {
    var oldTable = "users_sf_v2_old";
    var cols = swapTable(connection, "users", oldTable, """
      CREATE TABLE users (
        id VARCHAR(36) NOT NULL PRIMARY KEY,
        username VARCHAR(32) NOT NULL UNIQUE,
        email VARCHAR(255) NOT NULL UNIQUE,
        role VARCHAR(20) NOT NULL,
        last_login_at TIMESTAMP,
        min_issued_at TIMESTAMP NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        version BIGINT NOT NULL DEFAULT 0
      )
      """);
    if (cols == null) {
      return;
    }

    execute(connection, """
      INSERT INTO users (id, username, email, role, last_login_at, min_issued_at, created_at, updated_at, version)
      SELECT
        %s AS id,
        %s AS username,
        %s AS email,
        %s AS role,
        %s AS last_login_at,
        %s AS min_issued_at,
        %s AS created_at,
        %s AS updated_at,
        %s AS version
      FROM %s
      """.formatted(
      uuidExpr(cols, "id"),
      chooseExpr(cols, "''", "username"),
      chooseExpr(cols, "''", "email"),
      chooseExpr(cols, "'USER'", "role"),
      timestampExpr(cols, "NULL", "last_login_at", "lastLoginAt"),
      timestampExpr(cols, "CURRENT_TIMESTAMP", "min_issued_at", "minIssuedAt"),
      timestampExpr(cols, "CURRENT_TIMESTAMP", "created_at", "createdAt"),
      timestampExpr(cols, "CURRENT_TIMESTAMP", "updated_at", "updatedAt"),
      chooseExpr(cols, "0", "version"),
      oldTable
    ));
    dropTable(connection, oldTable);
  }

  private static void repairInstances(Connection connection) throws SQLException {
    var oldTable = "instances_sf_v2_old";
    var cols = swapTable(connection, "instances", oldTable, """
      CREATE TABLE instances (
        id VARCHAR(36) NOT NULL PRIMARY KEY,
        friendly_name VARCHAR(32) NOT NULL,
        icon VARCHAR(64) NOT NULL,
        owner_id VARCHAR(36) NOT NULL,
        session_lifecycle VARCHAR(20) NOT NULL,
        settings TEXT NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        version BIGINT NOT NULL DEFAULT 0,
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
      )
      """);
    if (cols == null) {
      return;
    }

    execute(connection, """
      INSERT INTO instances (id, friendly_name, icon, owner_id, session_lifecycle, settings, created_at, updated_at, version)
      SELECT
        %s AS id,
        %s AS friendly_name,
        %s AS icon,
        %s AS owner_id,
        %s AS session_lifecycle,
        %s AS settings,
        %s AS created_at,
        %s AS updated_at,
        %s AS version
      FROM %s
      """.formatted(
      uuidExpr(cols, "id"),
      chooseExpr(cols, "'Instance'", "friendly_name", "friendlyName"),
      chooseExpr(cols, "'pickaxe'", "icon"),
      uuidExpr(cols, "owner_id", "ownerId"),
      chooseExpr(cols, "'STOPPED'", "session_lifecycle", "sessionLifecycle", "attackLifecycle"),
      chooseExpr(cols, "'{}'", "settings"),
      timestampExpr(cols, "CURRENT_TIMESTAMP", "created_at", "createdAt"),
      timestampExpr(cols, "CURRENT_TIMESTAMP", "updated_at", "updatedAt"),
      chooseExpr(cols, "0", "version"),
      oldTable
    ));
    dropTable(connection, oldTable);
  }

  private static void repairInstanceAuditLogs(Connection connection) throws SQLException {
    var oldTable = "instance_audit_logs_sf_v2_old";
    var cols = swapTable(connection, "instance_audit_logs", oldTable, """
      CREATE TABLE instance_audit_logs (
        id VARCHAR(36) NOT NULL PRIMARY KEY,
        type VARCHAR(30) NOT NULL,
        data VARCHAR(4000),
        instance_id VARCHAR(36) NOT NULL,
        user_id VARCHAR(36) NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        version BIGINT NOT NULL DEFAULT 0,
        FOREIGN KEY (instance_id) REFERENCES instances(id) ON DELETE CASCADE,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
      )
      """);
    if (cols == null) {
      return;
    }

    execute(connection, """
      INSERT INTO instance_audit_logs (id, type, data, instance_id, user_id, created_at, updated_at, version)
      SELECT
        %s AS id,
        %s AS type,
        %s AS data,
        %s AS instance_id,
        %s AS user_id,
        %s AS created_at,
        %s AS updated_at,
        %s AS version
      FROM %s
      """.formatted(
      uuidExpr(cols, "id"),
      chooseExpr(cols, "'UNKNOWN'", "type"),
      chooseExpr(cols, "NULL", "data"),
      uuidExpr(cols, "instance_id", "instanceId"),
      uuidExpr(cols, "user_id", "userId"),
      timestampExpr(cols, "CURRENT_TIMESTAMP", "created_at", "createdAt"),
      timestampExpr(cols, "CURRENT_TIMESTAMP", "updated_at", "updatedAt"),
      chooseExpr(cols, "0", "version"),
      oldTable
    ));

    execute(connection, """
      UPDATE instance_audit_logs SET type = 'START_SESSION' WHERE type = 'START_ATTACK'
      """);
    execute(connection, """
      UPDATE instance_audit_logs SET type = 'PAUSE_SESSION' WHERE type = 'PAUSE_ATTACK'
      """);
    execute(connection, """
      UPDATE instance_audit_logs SET type = 'RESUME_SESSION' WHERE type = 'RESUME_ATTACK'
      """);
    execute(connection, """
      UPDATE instance_audit_logs SET type = 'STOP_SESSION' WHERE type = 'STOP_ATTACK'
      """);

    dropTable(connection, oldTable);
  }

  private static void repairServerConfig(Connection connection) throws SQLException {
    var oldTable = "server_config_sf_v2_old";
    var cols = swapTable(connection, "server_config", oldTable, """
      CREATE TABLE server_config (
        id BIGINT NOT NULL PRIMARY KEY,
        settings TEXT NOT NULL,
        version BIGINT NOT NULL DEFAULT 0
      )
      """);
    if (cols == null) {
      return;
    }

    execute(connection, """
      INSERT INTO server_config (id, settings, version)
      SELECT
        %s AS id,
        %s AS settings,
        %s AS version
      FROM %s
      """.formatted(
      chooseExpr(cols, "1", "id"),
      chooseExpr(cols, "'{}'", "settings"),
      chooseExpr(cols, "0", "version"),
      oldTable
    ));
    dropTable(connection, oldTable);
  }

  private static void repairScripts(Connection connection) throws SQLException {
    var oldTable = "scripts_sf_v2_old";
    var cols = swapTable(connection, "scripts", oldTable, """
      CREATE TABLE scripts (
        id VARCHAR(36) NOT NULL PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        description VARCHAR(1000),
        instance_id VARCHAR(36) NOT NULL,
        nodes_json TEXT NOT NULL,
        edges_json TEXT NOT NULL,
        paused BOOLEAN NOT NULL DEFAULT FALSE,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        version BIGINT NOT NULL DEFAULT 0,
        FOREIGN KEY (instance_id) REFERENCES instances(id) ON DELETE CASCADE
      )
      """);
    if (cols == null) {
      return;
    }

    // Old script schema is incompatible with the current graph-based model.
    if (hasColumn(cols, "scriptName")) {
      dropTable(connection, oldTable);
      return;
    }

    execute(connection, """
      INSERT INTO scripts (id, name, description, instance_id, nodes_json, edges_json, paused, created_at, updated_at, version)
      SELECT
        %s AS id,
        %s AS name,
        %s AS description,
        %s AS instance_id,
        %s AS nodes_json,
        %s AS edges_json,
        %s AS paused,
        %s AS created_at,
        %s AS updated_at,
        %s AS version
      FROM %s
      """.formatted(
      uuidExpr(cols, "id"),
      chooseExpr(cols, "'Recovered Script'", "name"),
      chooseExpr(cols, "NULL", "description"),
      uuidExpr(cols, "instance_id", "instanceId"),
      chooseExpr(cols, "'[]'", "nodes_json", "nodesJson"),
      chooseExpr(cols, "'[]'", "edges_json", "edgesJson"),
      chooseExpr(cols, "0", "paused"),
      timestampExpr(cols, "CURRENT_TIMESTAMP", "created_at", "createdAt"),
      timestampExpr(cols, "CURRENT_TIMESTAMP", "updated_at", "updatedAt"),
      chooseExpr(cols, "0", "version"),
      oldTable
    ));
    dropTable(connection, oldTable);
  }

  private static void ensureScriptsTable(Connection connection) throws SQLException {
    execute(connection, """
      CREATE TABLE IF NOT EXISTS scripts (
        id VARCHAR(36) NOT NULL PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        description VARCHAR(1000),
        instance_id VARCHAR(36) NOT NULL,
        nodes_json TEXT NOT NULL,
        edges_json TEXT NOT NULL,
        paused BOOLEAN NOT NULL DEFAULT FALSE,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        version BIGINT NOT NULL DEFAULT 0
      )
      """);
  }

  private static Set<String> swapTable(
    Connection connection,
    String table,
    String oldTable,
    String createSql
  ) throws SQLException {
    dropTable(connection, oldTable);
    if (!tableExists(connection, table)) {
      execute(connection, createSql);
      return null;
    }

    var cols = columnsFor(connection, table);
    execute(connection, "ALTER TABLE %s RENAME TO %s".formatted(table, oldTable));
    execute(connection, createSql);
    return cols;
  }

  private static boolean tableExists(Connection connection, String table) throws SQLException {
    try (var rs = connection.getMetaData().getTables(null, null, table, null)) {
      return rs.next();
    }
  }

  private static Set<String> columnsFor(Connection connection, String table) throws SQLException {
    var columns = new HashSet<String>();
    try (var rs = connection.getMetaData().getColumns(null, null, table, null)) {
      while (rs.next()) {
        columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
      }
    }
    return columns;
  }

  private static boolean hasColumn(Set<String> columns, String column) {
    return columns.contains(column.toLowerCase(Locale.ROOT));
  }

  private static String uuidExpr(Set<String> columns, String... candidates) {
    var source = firstExisting(columns, candidates);
    if (source == null) {
      return "NULL";
    }

    return """
      CASE
        WHEN typeof(%1$s) = 'blob' THEN
          substr(lower(hex(%1$s)), 1, 8) || '-' ||
          substr(lower(hex(%1$s)), 9, 4) || '-' ||
          substr(lower(hex(%1$s)), 13, 4) || '-' ||
          substr(lower(hex(%1$s)), 17, 4) || '-' ||
          substr(lower(hex(%1$s)), 21, 12)
        ELSE CAST(%1$s AS TEXT)
      END
      """.formatted(source);
  }

  private static String chooseExpr(Set<String> columns, String fallback, String... candidates) {
    var source = firstExisting(columns, candidates);
    return source == null ? fallback : source;
  }

  private static String timestampExpr(Set<String> columns, String fallback, String... candidates) {
    var source = firstExisting(columns, candidates);
    if (source == null) {
      return fallback;
    }

    // Normalize common legacy timestamp formats into JDBC Timestamp.valueOf format:
    // - Replace 'T' separator with space
    // - Drop trailing 'Z'
    // - Drop trailing timezone offset (+hh:mm / -hh:mm)
    return """
      trim(
        replace(
          replace(
            CASE
              WHEN instr(CAST(%1$s AS TEXT), '+') > 0
                THEN substr(CAST(%1$s AS TEXT), 1, instr(CAST(%1$s AS TEXT), '+') - 1)
              WHEN instr(substr(CAST(%1$s AS TEXT), 20), '-') > 0
                THEN substr(CAST(%1$s AS TEXT), 1, 19 + instr(substr(CAST(%1$s AS TEXT), 20), '-') - 1)
              ELSE CAST(%1$s AS TEXT)
            END,
            'T',
            ' '
          ),
          'Z',
          ''
        )
      )
      """.formatted(source);
  }

  private static String firstExisting(Set<String> columns, String... candidates) {
    for (var candidate : candidates) {
      if (hasColumn(columns, candidate)) {
        return candidate;
      }
    }
    return null;
  }

  private static void dropTable(Connection connection, String table) throws SQLException {
    execute(connection, "DROP TABLE IF EXISTS %s".formatted(table));
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    try (var stmt = connection.createStatement()) {
      stmt.execute(sql);
    }
  }
}
