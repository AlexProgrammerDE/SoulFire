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

import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.util.SFPathConstants;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

final class DatabaseManagerTest {
  @Test
  void deletingAnInstanceCascadesAfterAConnectionIsReplaced(@TempDir Path directory) throws Exception {
    try (var paths = mockStatic(SFPathConstants.class)) {
      paths.when(SFPathConstants::baseDirectory).thenReturn(directory);
      try (var database = DatabaseManager.select()) {
        var deleted = seed(database);
        var retained = seed(database);
        try (var connection = database.dataSource().getConnection()) {
          database.dataSource().evictConnection(connection);
        }
        database.dsl().deleteFrom(Tables.INSTANCES)
          .where(Tables.INSTANCES.ID.eq(deleted.instance())).execute();
        assertChildrenRemoved(database, deleted, retained);
        assertEquals(2, database.dsl().fetchCount(Tables.USERS));
        assertThrows(DataAccessException.class, () -> database.dsl().execute(
          "INSERT INTO instance_desired_bots (instance_id, profile_id) VALUES (?, ?)",
          deleted.instance(), UUID.randomUUID().toString()));
      }
    }
  }

  @Test
  void deletingAnOwnerCascadesThroughInstancesAndPermissionGrants(@TempDir Path directory) {
    try (var paths = mockStatic(SFPathConstants.class)) {
      paths.when(SFPathConstants::baseDirectory).thenReturn(directory);
      try (var database = DatabaseManager.select()) {
        var deleted = seed(database);
        var retained = seed(database);
        database.dsl().deleteFrom(Tables.USERS)
          .where(Tables.USERS.ID.eq(deleted.owner())).execute();
        assertChildrenRemoved(database, deleted, retained);
        assertEquals(1, database.dsl().fetchCount(Tables.USERS));
        assertEquals(1, database.dsl().fetchCount(Tables.PLUGIN_PERMISSION_GRANTS));
        assertTrue(database.dsl().fetchExists(Tables.PLUGIN_PERMISSION_GRANTS,
          Tables.PLUGIN_PERMISSION_GRANTS.USER_ID.eq(retained.owner())));
      }
    }
  }

  @Test
  void upgradingABaselinedDatabasePreservesRecordsAndEnablesCascades(@TempDir Path directory) throws Exception {
    var url = "jdbc:sqlite:" + directory.resolve("soulfire.sqlite");
    Flyway.configure().dataSource(url, null, null).locations("classpath:db/migration")
      .target("1").load().migrate();
    var owner = UUID.randomUUID().toString();
    var instance = UUID.randomUUID().toString();
    var script = UUID.randomUUID().toString();
    try (var connection = DriverManager.getConnection(url)) {
      var legacy = DSL.using(connection, SQLDialect.SQLITE);
      legacy.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, 'legacy-user', 'legacy@example.com', 'ADMIN', CURRENT_TIMESTAMP)", owner);
      legacy.execute("INSERT INTO instances (id, friendly_name, icon, owner_id, session_lifecycle, settings) VALUES (?, 'Legacy', 'pickaxe', ?, 'STOPPED', '{}')", instance, owner);
      legacy.execute("INSERT INTO scripts (id, name, instance_id, nodes_json, edges_json, paused) VALUES (?, 'Legacy script', ?, '[]', '[]', TRUE)", script, instance);
      legacy.execute("INSERT INTO instance_audit_logs (id, type, instance_id, user_id) VALUES (?, 'EXECUTE_COMMAND', ?, ?)", UUID.randomUUID().toString(), instance, owner);
      // A schema without Flyway history must go through the legacy baseline path.
      legacy.execute("DROP TABLE flyway_schema_history");
    }
    try (var paths = mockStatic(SFPathConstants.class)) {
      paths.when(SFPathConstants::baseDirectory).thenReturn(directory);
      try (var database = DatabaseManager.select()) {
        var dsl = database.dsl();
        assertEquals(owner, dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instance)).fetchSingle().getOwnerId());
        assertTrue(dsl.selectFrom(Tables.SCRIPTS).where(Tables.SCRIPTS.ID.eq(script)).fetchSingle().getPaused());
        assertEquals(1, dsl.fetchCount(Tables.INSTANCE_AUDIT_LOGS));
        dsl.deleteFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instance)).execute();
        assertEquals(0, dsl.fetchCount(Tables.SCRIPTS));
        assertEquals(0, dsl.fetchCount(Tables.INSTANCE_AUDIT_LOGS));
        assertEquals(1, dsl.fetchCount(Tables.USERS));
      }
    }
  }

  private static Fixture seed(DatabaseManager.DatabaseContext database) {
    var fixture = new Fixture(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    var dsl = database.dsl();
    dsl.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, 'ADMIN', CURRENT_TIMESTAMP)",
      fixture.owner(), fixture.owner().substring(0, 12), fixture.owner() + "@example.com");
    dsl.execute("INSERT INTO instances (id, friendly_name, icon, owner_id, settings) VALUES (?, 'Instance', 'pickaxe', ?, '{}')",
      fixture.instance(), fixture.owner());
    dsl.execute("INSERT INTO scripts (id, name, instance_id, nodes_json, edges_json, paused) VALUES (?, 'Script', ?, '[]', '[]', TRUE)",
      UUID.randomUUID().toString(), fixture.instance());
    dsl.execute("INSERT INTO instance_audit_logs (id, type, instance_id, user_id) VALUES (?, 'EXECUTE_COMMAND', ?, ?)",
      UUID.randomUUID().toString(), fixture.instance(), fixture.owner());
    dsl.execute("INSERT INTO instance_desired_bots (instance_id, profile_id) VALUES (?, ?)",
      fixture.instance(), UUID.randomUUID().toString());
    dsl.execute("INSERT INTO plugin_permission_grants (user_id, permission_id, scope, granted) VALUES (?, 'qa.permission', 'GLOBAL', TRUE)",
      fixture.owner());
    return fixture;
  }

  private static void assertChildrenRemoved(DatabaseManager.DatabaseContext database, Fixture deleted, Fixture retained) {
    var dsl = database.dsl();
    assertFalse(dsl.fetchExists(Tables.INSTANCES, Tables.INSTANCES.ID.eq(deleted.instance())));
    assertTrue(dsl.fetchExists(Tables.INSTANCES, Tables.INSTANCES.ID.eq(retained.instance())));
    assertEquals(1, dsl.fetchCount(Tables.SCRIPTS));
    assertEquals(1, dsl.fetchCount(Tables.INSTANCE_AUDIT_LOGS));
    assertEquals(1, dsl.fetchCount(Tables.INSTANCE_DESIRED_BOTS));
    assertTrue(dsl.fetchExists(Tables.SCRIPTS, Tables.SCRIPTS.INSTANCE_ID.eq(retained.instance())));
    assertTrue(dsl.fetchExists(Tables.INSTANCE_AUDIT_LOGS, Tables.INSTANCE_AUDIT_LOGS.INSTANCE_ID.eq(retained.instance())));
    assertTrue(dsl.fetchExists(Tables.INSTANCE_DESIRED_BOTS, Tables.INSTANCE_DESIRED_BOTS.INSTANCE_ID.eq(retained.instance())));
  }

  private record Fixture(String owner, String instance) {}
}
