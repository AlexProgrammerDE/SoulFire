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
import java.util.Locale;

public final class V3__migrate_legacy_local_timestamps_to_utc extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    var connection = context.getConnection();
    var product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
    if (!product.contains("sqlite")) {
      return;
    }

    // Only legacy/baselined installs need this conversion. Fresh installs have
    // no BASELINE row and already write UTC timestamps.
    if (!hasBaselineRow(connection)) {
      return;
    }

    convertTimestamps(connection);
  }

  private static boolean hasBaselineRow(Connection connection) throws SQLException {
    try (var stmt = connection.createStatement();
         var rs = stmt.executeQuery("""
           SELECT COUNT(*) FROM flyway_schema_history
           WHERE type = 'BASELINE' AND success = 1
           """)) {
      return rs.next() && rs.getInt(1) > 0;
    }
  }

  private static void convertTimestamps(Connection connection) throws SQLException {
    execute(connection, "PRAGMA foreign_keys = OFF");
    try {
      convertColumnIfTableExists(connection, "users", "last_login_at");
      convertColumnIfTableExists(connection, "users", "min_issued_at");
      convertColumnIfTableExists(connection, "users", "created_at");
      convertColumnIfTableExists(connection, "users", "updated_at");

      convertColumnIfTableExists(connection, "instances", "created_at");
      convertColumnIfTableExists(connection, "instances", "updated_at");

      convertColumnIfTableExists(connection, "instance_audit_logs", "created_at");
      convertColumnIfTableExists(connection, "instance_audit_logs", "updated_at");

      convertColumnIfTableExists(connection, "scripts", "created_at");
      convertColumnIfTableExists(connection, "scripts", "updated_at");
    } finally {
      execute(connection, "PRAGMA foreign_keys = ON");
    }
  }

  private static void convertColumnIfTableExists(
    Connection connection, String table, String column) throws SQLException {
    if (!tableExists(connection, table)) {
      return;
    }

    // Treat existing value as local time and rewrite it as UTC.
    // Also normalize legacy formats (T separator, trailing Z/offset, epoch
    // seconds/millis) into a JDBC-compatible timestamp string.
    execute(connection, """
      UPDATE %1$s
      SET %2$s = COALESCE(
        CASE
          WHEN trim(CAST(%2$s AS TEXT)) GLOB '[0-9][0-9][0-9][0-9]*' THEN
            CASE
              WHEN length(trim(CAST(%2$s AS TEXT))) >= 13
                THEN strftime('%%Y-%%m-%%d %%H:%%M:%%f', substr(trim(CAST(%2$s AS TEXT)), 1, 10), 'unixepoch')
              ELSE strftime('%%Y-%%m-%%d %%H:%%M:%%f', trim(CAST(%2$s AS TEXT)), 'unixepoch')
            END
          ELSE strftime('%%Y-%%m-%%d %%H:%%M:%%f', CAST(%2$s AS TEXT), 'utc')
        END,
        trim(
          replace(
            replace(
              CASE
                WHEN instr(CAST(%2$s AS TEXT), '+') > 0
                  THEN substr(CAST(%2$s AS TEXT), 1, instr(CAST(%2$s AS TEXT), '+') - 1)
                WHEN instr(substr(CAST(%2$s AS TEXT), 20), '-') > 0
                  THEN substr(CAST(%2$s AS TEXT), 1, 19 + instr(substr(CAST(%2$s AS TEXT), 20), '-') - 1)
                ELSE CAST(%2$s AS TEXT)
              END,
              'T',
              ' '
            ),
            'Z',
            ''
          )
        )
      )
      WHERE %2$s IS NOT NULL
      """.formatted(table, column));
  }

  private static boolean tableExists(Connection connection, String table) throws SQLException {
    try (var rs = connection.getMetaData().getTables(null, null, table, null)) {
      return rs.next();
    }
  }

  private static void execute(Connection connection, String sql) throws SQLException {
    try (var stmt = connection.createStatement()) {
      stmt.execute(sql);
    }
  }
}
