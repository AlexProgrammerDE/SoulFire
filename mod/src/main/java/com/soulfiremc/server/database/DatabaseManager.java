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

import com.soulfiremc.server.util.SFPathConstants;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import javax.sql.DataSource;
import java.sql.SQLException;

@Slf4j
public final class DatabaseManager {
  private DatabaseManager() {
  }

  public static DatabaseContext select() {
    return switch (System.getProperty("database", "sqlite")) {
      case "sqlite" -> forSqlite();
      case "mysql" -> forMysql();
      default -> throw new IllegalArgumentException("Invalid database type");
    };
  }

  private static DatabaseContext forSqlite() {
    var dbFile = SFPathConstants.BASE_DIR.resolve("soulfire.sqlite");
    var jdbcUrl = "jdbc:sqlite:%s".formatted(dbFile);

    var hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(jdbcUrl);
    hikariConfig.setMinimumIdle(1);
    hikariConfig.setMaximumPoolSize(1);
    hikariConfig.setIdleTimeout(0);
    hikariConfig.setConnectionTimeout(30_000);

    var dataSource = new HikariDataSource(hikariConfig);

    runMigrations(dataSource);

    var dsl = createDslContext(dataSource, SQLDialect.SQLITE);
    return new DatabaseContext(dsl, dataSource);
  }

  private static DatabaseContext forMysql() {
    var jdbcUrl = "jdbc:mysql://%s:%s/%s".formatted(
      System.getProperty("mysql.host", "localhost"),
      Integer.getInteger("mysql.port", 3306),
      System.getProperty("mysql.database", "soulfire")
    );

    var hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(jdbcUrl);
    hikariConfig.setUsername(System.getProperty("mysql.user"));
    hikariConfig.setPassword(System.getProperty("mysql.password"));
    hikariConfig.setMinimumIdle(Integer.getInteger("mysql.minimumIdle", 1));
    hikariConfig.setMaximumPoolSize(Integer.getInteger("mysql.maximumPoolSize", 10));
    hikariConfig.setIdleTimeout(0);
    hikariConfig.setConnectionTimeout(30_000);

    var dataSource = new HikariDataSource(hikariConfig);

    runMigrations(dataSource);

    var dsl = createDslContext(dataSource, SQLDialect.MARIADB);
    return new DatabaseContext(dsl, dataSource);
  }

  private static void runMigrations(DataSource dataSource) {
    migrateFromHibernate(dataSource);

    var flyway = Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration")
      .baselineOnMigrate(true)
      .baselineVersion("1")
      .load();
    flyway.migrate();
  }

  /// Detects an existing Hibernate-created schema and migrates it to the
  /// Flyway V1 format. Handles two independent issues:
  /// 1. camelCase column names → snake_case
  /// 2. Binary UUID BLOBs → hyphenated text format
  /// Each step is idempotent and can run independently.
  private static void migrateFromHibernate(DataSource dataSource) {
    try (var conn = dataSource.getConnection()) {
      // Check if users table exists at all
      try (var rs = conn.getMetaData().getTables(null, null, "users", null)) {
        if (!rs.next()) {
          return; // Fresh database, nothing to migrate
        }
      }

      // Check for camelCase columns (not yet renamed)
      var needsColumnRename = false;
      try (var rs = conn.getMetaData().getColumns(null, null, "users", "createdAt")) {
        needsColumnRename = rs.next();
      }

      // Check for binary UUID values
      var needsUuidConversion = false;
      try (var stmt = conn.createStatement();
           var rs = stmt.executeQuery("SELECT typeof(id) FROM users LIMIT 1")) {
        if (rs.next()) {
          needsUuidConversion = "blob".equals(rs.getString(1));
        }
      }

      if (!needsColumnRename && !needsUuidConversion) {
        return; // Already fully migrated
      }

      log.info("Detected old Hibernate schema (renameColumns={}, convertUuids={}), migrating...",
        needsColumnRename, needsUuidConversion);
      try (var stmt = conn.createStatement()) {
        if (needsUuidConversion) {
          // Convert binary UUID columns to text format (Hibernate stores UUIDs as BLOBs)
          convertBinaryUuids(stmt, "users", "id");
          convertBinaryUuids(stmt, "instances", "id");
          convertBinaryUuids(stmt, "instances", "owner_id");
          convertBinaryUuids(stmt, "instance_audit_logs", "id");
          convertBinaryUuids(stmt, "instance_audit_logs", "instance_id");
          convertBinaryUuids(stmt, "instance_audit_logs", "user_id");
          convertBinaryUuids(stmt, "scripts", "id");
          convertBinaryUuids(stmt, "scripts", "instance_id");
        }

        if (needsColumnRename) {
          // users
          renameColumn(stmt, "users", "createdAt", "created_at");
          renameColumn(stmt, "users", "updatedAt", "updated_at");
          renameColumn(stmt, "users", "lastLoginAt", "last_login_at");
          renameColumn(stmt, "users", "minIssuedAt", "min_issued_at");

          // instances
          renameColumn(stmt, "instances", "friendlyName", "friendly_name");
          renameColumn(stmt, "instances", "sessionLifecycle", "session_lifecycle");
          renameColumn(stmt, "instances", "createdAt", "created_at");
          renameColumn(stmt, "instances", "updatedAt", "updated_at");

          // instance_audit_logs
          renameColumn(stmt, "instance_audit_logs", "createdAt", "created_at");
          renameColumn(stmt, "instance_audit_logs", "updatedAt", "updated_at");

          // scripts
          renameColumn(stmt, "scripts", "nodesJson", "nodes_json");
          renameColumn(stmt, "scripts", "edgesJson", "edges_json");
          renameColumn(stmt, "scripts", "createdAt", "created_at");
          renameColumn(stmt, "scripts", "updatedAt", "updated_at");
        }
      }

      log.info("Hibernate schema migration complete");
    } catch (SQLException e) {
      log.warn("Could not migrate from Hibernate schema", e);
    }
  }

  /// Converts binary UUID values (stored by Hibernate as BLOBs) to the standard
  /// hyphenated text format expected by JOOQ. Only updates rows where the column
  /// contains BLOB data (typeof returns 'blob'), leaving text UUIDs untouched.
  private static void convertBinaryUuids(
    java.sql.Statement stmt, String table, String column) {
    try {
      // SQLite: convert 16-byte binary UUID to hyphenated text format
      // lower(hex(x)) gives 32 hex chars, then we insert hyphens at positions 8, 12, 16, 20
      stmt.execute("""
        UPDATE %s SET %s =
          substr(lower(hex(%s)), 1, 8) || '-' ||
          substr(lower(hex(%s)), 9, 4) || '-' ||
          substr(lower(hex(%s)), 13, 4) || '-' ||
          substr(lower(hex(%s)), 17, 4) || '-' ||
          substr(lower(hex(%s)), 21, 12)
        WHERE typeof(%s) = 'blob'
        """.formatted(table, column, column, column, column, column, column, column));
    } catch (SQLException e) {
      log.debug("Could not convert binary UUIDs in {}.{}: {}", table, column, e.getMessage());
    }
  }

  private static void renameColumn(
    java.sql.Statement stmt, String table, String oldName, String newName) {
    try {
      stmt.execute("ALTER TABLE %s RENAME COLUMN %s TO %s".formatted(table, oldName, newName));
    } catch (SQLException e) {
      log.debug("Could not rename column {}.{} (may not exist): {}", table, oldName, e.getMessage());
    }
  }

  private static DSLContext createDslContext(DataSource dataSource, SQLDialect dialect) {
    var config = new DefaultConfiguration()
      .set(dataSource)
      .set(dialect)
      .set(new DefaultConfiguration().settings()
        .withExecuteWithOptimisticLocking(true));
    return DSL.using(config);
  }

  public record DatabaseContext(DSLContext dsl, HikariDataSource dataSource) implements AutoCloseable {
    @Override
    public void close() {
      dataSource.close();
    }
  }
}
