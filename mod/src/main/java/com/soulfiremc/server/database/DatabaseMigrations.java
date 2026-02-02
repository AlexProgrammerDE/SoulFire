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
package com.soulfiremc.server.database;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Handles database migrations that need to run before Hibernate initializes.
 * These migrations handle schema changes that Hibernate's hbm2ddl.auto=update cannot handle,
 * such as column renames and enum value updates.
 */
@Slf4j
public final class DatabaseMigrations {
  private DatabaseMigrations() {
  }

  /**
   * Runs all necessary migrations on the given database connection.
   * Migrations are idempotent - they check if changes are needed before applying.
   *
   * @param connection the database connection
   * @param isSqlite   true if using SQLite, false for MySQL/MariaDB
   */
  public static void runMigrations(Connection connection, boolean isSqlite) {
    try {
      migrateAttackToSession(connection, isSqlite);
      dropScriptsTable(connection, isSqlite);
      dropScriptsScopeColumn(connection, isSqlite);
    } catch (SQLException e) {
      log.error("Failed to run database migrations", e);
      throw new RuntimeException("Database migration failed", e);
    }
  }

  /**
   * Migration: Drop the old scripts table if it has the old schema.
   * The new visual script editor uses a different table structure.
   * The old table had columns like (id, name, description, instance_id, code, enabled, createdAt, updatedAt, version)
   * while the new table has (id, name, description, instance_id, scope, nodesJson, edgesJson, createdAt, updatedAt, version).
   */
  private static void dropScriptsTable(Connection connection, boolean isSqlite) throws SQLException {
    if (tableExists(connection, "scripts", isSqlite)) {
      // Check if this is the old schema by looking for the 'code' column (old) vs 'nodesJson' column (new)
      boolean hasOldSchema = columnExists(connection, "scripts", "code", isSqlite);
      boolean hasNewSchema = columnExists(connection, "scripts", "nodesJson", isSqlite);

      if (hasOldSchema && !hasNewSchema) {
        log.info("Migrating database: dropping old scripts table (old scripting system)");
        try (var stmt = connection.createStatement()) {
          stmt.execute("DROP TABLE scripts");
        }
        log.info("Successfully dropped old scripts table - new table will be created by Hibernate");
      }
    }
  }

  /**
   * Migration: Drop the scope column from scripts table.
   * Scripts no longer have a scope (BOT/INSTANCE) - they all run at instance level
   * and bot becomes an explicit input to nodes.
   */
  private static void dropScriptsScopeColumn(Connection connection, boolean isSqlite) throws SQLException {
    if (tableExists(connection, "scripts", isSqlite)
      && columnExists(connection, "scripts", "scope", isSqlite)) {
      log.info("Migrating database: dropping scope column from scripts table");

      if (isSqlite) {
        // SQLite 3.35.0+ supports DROP COLUMN, but for broader compatibility
        // we'll use the table recreation approach
        try (var stmt = connection.createStatement()) {
          stmt.execute("PRAGMA foreign_keys=OFF");

          // Create new table without scope column
          stmt.execute("""
            CREATE TABLE scripts_new (
              id BLOB NOT NULL,
              name VARCHAR(100) NOT NULL,
              description VARCHAR(1000),
              instance_id BLOB NOT NULL,
              nodesJson TEXT NOT NULL,
              edgesJson TEXT NOT NULL,
              autoStart INTEGER NOT NULL DEFAULT 0,
              createdAt TIMESTAMP NOT NULL,
              updatedAt TIMESTAMP NOT NULL,
              version BIGINT NOT NULL,
              PRIMARY KEY (id),
              FOREIGN KEY (instance_id) REFERENCES instances(id)
            )
            """);

          // Copy data (excluding scope column)
          stmt.execute("""
            INSERT INTO scripts_new (id, name, description, instance_id, nodesJson, edgesJson, autoStart, createdAt, updatedAt, version)
            SELECT id, name, description, instance_id, nodesJson, edgesJson, autoStart, createdAt, updatedAt, version
            FROM scripts
            """);

          // Drop old table
          stmt.execute("DROP TABLE scripts");

          // Rename new table
          stmt.execute("ALTER TABLE scripts_new RENAME TO scripts");

          stmt.execute("PRAGMA foreign_keys=ON");
        }
      } else {
        // MySQL/MariaDB supports DROP COLUMN directly
        try (var stmt = connection.createStatement()) {
          stmt.execute("ALTER TABLE scripts DROP COLUMN scope");
        }
      }

      log.info("Successfully dropped scope column from scripts table");
    }
  }

  /**
   * Migration: Rename 'attack' terminology to 'session' terminology.
   * - Renames attackLifecycle column to sessionLifecycle in instances table
   * - Updates enum values from *_ATTACK to *_SESSION in instance_audit_logs table
   */
  private static void migrateAttackToSession(Connection connection, boolean isSqlite) throws SQLException {
    // Check if the old column exists in instances table
    if (columnExists(connection, "instances", "attackLifecycle", isSqlite)) {
      log.info("Migrating database: renaming attackLifecycle to sessionLifecycle");

      if (isSqlite) {
        // SQLite doesn't support RENAME COLUMN in older versions, but modern SQLite does
        // Using ALTER TABLE ... RENAME COLUMN which works in SQLite 3.25.0+
        try (var stmt = connection.createStatement()) {
          stmt.execute("ALTER TABLE instances RENAME COLUMN attackLifecycle TO sessionLifecycle");
        }
      } else {
        // MySQL/MariaDB syntax
        try (var stmt = connection.createStatement()) {
          stmt.execute("ALTER TABLE instances CHANGE COLUMN attackLifecycle sessionLifecycle VARCHAR(255)");
        }
      }

      log.info("Successfully renamed attackLifecycle column to sessionLifecycle");
    }

    // Update enum values in instance_audit_logs table
    // SQLite has CHECK constraints on enum columns, so we need to handle this carefully
    if (tableExists(connection, "instance_audit_logs", isSqlite)) {
      updateAuditLogEnumValues(connection, isSqlite);
    }
  }

  /**
   * Updates the audit log type enum values from *_ATTACK to *_SESSION.
   * For SQLite, we need to recreate the table because CHECK constraints cannot be modified.
   */
  private static void updateAuditLogEnumValues(Connection connection, boolean isSqlite) throws SQLException {
    // Check if migration is needed by looking for old values
    boolean needsMigration = false;
    try (var stmt = connection.createStatement();
         var rs = stmt.executeQuery(
           "SELECT COUNT(*) FROM instance_audit_logs WHERE type IN ('START_ATTACK', 'PAUSE_ATTACK', 'RESUME_ATTACK', 'STOP_ATTACK')")) {
      if (rs.next() && rs.getInt(1) > 0) {
        needsMigration = true;
      }
    }

    if (!needsMigration) {
      return;
    }

    log.info("Migrating audit log entries from ATTACK to SESSION terminology");

    if (isSqlite) {
      // SQLite: We need to recreate the table to change the CHECK constraint
      // 1. Create new table with updated CHECK constraint
      // 2. Copy data with updated values
      // 3. Drop old table
      // 4. Rename new table

      try (var stmt = connection.createStatement()) {
        // Disable foreign keys temporarily
        stmt.execute("PRAGMA foreign_keys=OFF");

        // Create new table with updated CHECK constraint
        stmt.execute("""
          CREATE TABLE instance_audit_logs_new (
            id BLOB NOT NULL,
            type VARCHAR(255) NOT NULL CHECK (type IN ('EXECUTE_COMMAND','START_SESSION','PAUSE_SESSION','RESUME_SESSION','STOP_SESSION')),
            data VARCHAR(4000),
            instance_id BLOB NOT NULL,
            user_id BLOB NOT NULL,
            createdAt TIMESTAMP NOT NULL,
            updatedAt TIMESTAMP NOT NULL,
            version BIGINT NOT NULL,
            PRIMARY KEY (id),
            FOREIGN KEY (instance_id) REFERENCES instances(id),
            FOREIGN KEY (user_id) REFERENCES users(id)
          )
          """);

        // Copy data with updated enum values
        stmt.execute("""
          INSERT INTO instance_audit_logs_new (id, type, data, instance_id, user_id, createdAt, updatedAt, version)
          SELECT id,
            CASE type
              WHEN 'START_ATTACK' THEN 'START_SESSION'
              WHEN 'PAUSE_ATTACK' THEN 'PAUSE_SESSION'
              WHEN 'RESUME_ATTACK' THEN 'RESUME_SESSION'
              WHEN 'STOP_ATTACK' THEN 'STOP_SESSION'
              ELSE type
            END,
            data, instance_id, user_id, createdAt, updatedAt, version
          FROM instance_audit_logs
          """);

        // Drop old table
        stmt.execute("DROP TABLE instance_audit_logs");

        // Rename new table
        stmt.execute("ALTER TABLE instance_audit_logs_new RENAME TO instance_audit_logs");

        // Re-enable foreign keys
        stmt.execute("PRAGMA foreign_keys=ON");

        log.info("Successfully migrated audit log entries to SESSION terminology");
      }
    } else {
      // MySQL/MariaDB: Can update directly, then alter the column to change the enum
      var updates = new String[][]{
        {"START_ATTACK", "START_SESSION"},
        {"PAUSE_ATTACK", "PAUSE_SESSION"},
        {"RESUME_ATTACK", "RESUME_SESSION"},
        {"STOP_ATTACK", "STOP_SESSION"}
      };

      for (var update : updates) {
        var oldValue = update[0];
        var newValue = update[1];

        try (var pstmt = connection.prepareStatement(
          "UPDATE instance_audit_logs SET type = ? WHERE type = ?")) {
          pstmt.setString(1, newValue);
          pstmt.setString(2, oldValue);
          var affected = pstmt.executeUpdate();
          if (affected > 0) {
            log.info("Updated {} audit log entries from {} to {}", affected, oldValue, newValue);
          }
        }
      }
    }
  }

  /**
   * Checks if a table exists.
   */
  private static boolean tableExists(Connection connection, String table, boolean isSqlite) throws SQLException {
    if (isSqlite) {
      try (var stmt = connection.createStatement();
           var rs = stmt.executeQuery(
             "SELECT name FROM sqlite_master WHERE type='table' AND name='" + table + "'")) {
        return rs.next();
      }
    } else {
      try (var stmt = connection.prepareStatement(
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?")) {
        stmt.setString(1, table);
        try (var rs = stmt.executeQuery()) {
          return rs.next() && rs.getInt(1) > 0;
        }
      }
    }
  }

  /**
   * Checks if a column exists in a table.
   */
  private static boolean columnExists(Connection connection, String table, String column, boolean isSqlite) throws SQLException {
    if (isSqlite) {
      // SQLite: use PRAGMA table_info
      try (var stmt = connection.createStatement();
           var rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
        while (rs.next()) {
          if (column.equalsIgnoreCase(rs.getString("name"))) {
            return true;
          }
        }
      }
    } else {
      // MySQL/MariaDB: use information_schema
      try (var stmt = connection.prepareStatement(
        "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?")) {
        stmt.setString(1, table);
        stmt.setString(2, column);
        try (var rs = stmt.executeQuery()) {
          if (rs.next()) {
            return rs.getInt(1) > 0;
          }
        }
      }
    }
    return false;
  }
}
