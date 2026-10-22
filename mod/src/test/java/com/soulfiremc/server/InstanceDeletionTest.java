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
package com.soulfiremc.server;

import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.SFPathConstants;
import net.lenni0451.reflect.Fields;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

final class InstanceDeletionTest {
  private final UUID id = UUID.randomUUID();
  private final InstanceManager instance = mock();
  private final SoulFireServer server = mock(CALLS_REAL_METHODS);
  private DSLContext dsl;

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var source = new SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + directory.resolve("deletion.sqlite"));
    Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    dsl = DSL.using(source, SQLDialect.SQLITE);
    var owner = UUID.randomUUID().toString();
    dsl.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      owner, "owner", "owner@example.com", "ADMIN");
    dsl.insertInto(Tables.INSTANCES)
      .set(Tables.INSTANCES.ID, id.toString())
      .set(Tables.INSTANCES.FRIENDLY_NAME, "deletion")
      .set(Tables.INSTANCES.OWNER_ID, owner)
      .set(Tables.INSTANCES.ICON, "pickaxe")
      .set(Tables.INSTANCES.SETTINGS, "{}")
      .execute();
    doReturn(dsl).when(server).dsl();
    var instances = new ConcurrentHashMap<UUID, InstanceManager>();
    instances.put(id, instance);
    Fields.setObject(server, Fields.getDeclaredField(SoulFireServer.class, "instances"), instances);
    Fields.setObject(server, Fields.getDeclaredField(SoulFireServer.class, "instanceDeletions"), new ConcurrentHashMap<>());
    var scheduler = mock(SoulFireScheduler.class);
    doAnswer(invocation -> {
      invocation.<Runnable>getArgument(0).run();
      return null;
    }).when(scheduler).execute(any(Runnable.class));
    Fields.setObject(server, Fields.getDeclaredField(SoulFireServer.class, "scheduler"), scheduler);
  }

  @Test
  void cleanupCanReloadSettingsUntilItCompletesAndConcurrentDeletesShareTheWork() {
    var disconnected = new CompletableFuture<Void>();
    var cleaned = disconnected.thenRun(() ->
      assertEquals("{}", dsl.select(Tables.INSTANCES.SETTINGS).from(Tables.INSTANCES)
        .where(Tables.INSTANCES.ID.eq(id.toString())).fetchSingle().value1()));
    doReturn(cleaned).when(instance).deleteInstance();

    var first = server.deleteInstance(id).orElseThrow();
    var second = server.deleteInstance(id).orElseThrow();
    assertSame(first, second);
    assertFalse(first.isDone());
    assertTrue(stored());
    assertSame(instance, server.getInstance(id).orElseThrow());
    verify(instance).deleteInstance();

    disconnected.complete(null);
    first.join();
    assertFalse(stored());
    assertTrue(server.getInstance(id).isEmpty());
    assertTrue(server.deleteInstance(id).isEmpty());
  }

  @Test
  void failedShutdownKeepsTheProfileAvailableForRetry() {
    var disconnected = new CompletableFuture<Void>();
    doReturn(disconnected, CompletableFuture.completedFuture(null)).when(instance).deleteInstance();
    var deletion = server.deleteInstance(id).orElseThrow();
    var cause = new IllegalStateException("Disconnect failed");
    disconnected.completeExceptionally(cause);

    assertSame(cause, assertThrows(CompletionException.class, deletion::join).getCause());
    assertTrue(stored());
    assertSame(instance, server.getInstance(id).orElseThrow());
    server.deleteInstance(id).orElseThrow().join();
    assertFalse(stored());
    verify(instance, times(2)).deleteInstance();
  }

  @Test
  void failedDatabaseDeleteKeepsRuntimeRegistrationUntilTheRetrySucceeds() {
    doReturn(CompletableFuture.completedFuture(null)).when(instance).deleteInstance();
    dsl.execute("CREATE TRIGGER reject_delete BEFORE DELETE ON instances BEGIN SELECT RAISE(ABORT, 'Delete rejected'); END");
    try {
      var deletion = server.deleteInstance(id).orElseThrow();
      assertTrue(assertThrows(CompletionException.class, deletion::join).getCause() instanceof DataAccessException);
      assertTrue(stored());
      assertSame(instance, server.getInstance(id).orElseThrow());
    } finally {
      dsl.execute("DROP TRIGGER reject_delete");
    }
    server.deleteInstance(id).orElseThrow().join();
    assertFalse(stored());
    assertTrue(server.getInstance(id).isEmpty());
  }

  @Test
  void aProfileWhoseRuntimeDidNotLoadStillDeletesItsFilesAndDatabaseRow(@TempDir Path directory) throws IOException {
    server.instances().clear();
    var clientData = directory.resolve("client-data").resolve("instance-" + id);
    var objectData = directory.resolve("object-storage").resolve("instance-" + id);
    Files.createDirectories(clientData);
    Files.createDirectories(objectData);
    Files.write(clientData.resolve("state"), new byte[]{1});
    Files.write(objectData.resolve("state"), new byte[]{2});
    try (var paths = mockStatic(SFPathConstants.class)) {
      paths.when(SFPathConstants::baseDirectory).thenReturn(directory);
      server.deleteInstance(id).orElseThrow().join();
    }
    assertFalse(Files.exists(clientData));
    assertFalse(Files.exists(objectData));
    assertFalse(stored());
    assertTrue(server.deleteInstance(id).isEmpty());
  }

  @Test
  void storageFailureLeavesTheProfileAvailableForRetry(@TempDir Path directory) {
    server.instances().clear();
    var cause = new IOException("Storage is unavailable");
    try (var paths = mockStatic(SFPathConstants.class)) {
      paths.when(SFPathConstants::baseDirectory).thenReturn(directory);
      try (var storage = mockStatic(SFHelpers.class)) {
        storage.when(() -> SFHelpers.deleteDirectory(any(Path.class))).thenThrow(cause);
        var deletion = server.deleteInstance(id).orElseThrow();
        var failure = assertThrows(CompletionException.class, deletion::join).getCause();
        assertTrue(failure instanceof UncheckedIOException);
        assertSame(cause, failure.getCause());
        assertTrue(stored());
      }
      server.deleteInstance(id).orElseThrow().join();
      assertFalse(stored());
    }
  }

  private boolean stored() {
    return dsl.fetchExists(Tables.INSTANCES, Tables.INSTANCES.ID.eq(id.toString()));
  }
}
