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

import com.soulfiremc.grpc.generated.ActivateScriptRequest;
import com.soulfiremc.grpc.generated.CreateScriptRequest;
import com.soulfiremc.grpc.generated.CreateScriptResponse;
import com.soulfiremc.grpc.generated.ScriptEvent;
import com.soulfiremc.grpc.generated.UpdateScriptRequest;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.grpc.RPCServer;
import com.soulfiremc.server.grpc.ScriptServiceImpl;
import com.soulfiremc.server.grpc.ServerRPCConstants;
import com.soulfiremc.server.script.ScriptTriggerService;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.SFPathConstants;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import net.lenni0451.reflect.Fields;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

final class InstanceDeletionTest {
  private final UUID id = UUID.randomUUID();
  private final InstanceManager instance = mock();
  private final SoulFireServer server = mock(CALLS_REAL_METHODS);
  private final Context scriptContext = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, mock(SoulFireUser.class));
  private DSLContext dsl;

  @BeforeEach
  void setUp(@TempDir Path directory) {
    doCallRealMethod().when(instance).markDeleting();
    doCallRealMethod().when(instance).deleting();
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
    var rpcServer = mock(RPCServer.class);
    doReturn(mock(ScriptServiceImpl.class)).when(rpcServer).scriptService();
    Fields.setObject(server, Fields.getDeclaredField(SoulFireServer.class, "rpcServer"), rpcServer);
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

  @ParameterizedTest
  @EnumSource(ScriptMutation.class)
  void pendingDeletionRejectsScriptMutations(ScriptMutation mutation) {
    var scripts = scriptService();
    var scriptId = pausedScript(scripts);
    ServerCallStreamObserver<ScriptEvent> activation = mock();
    var cleanup = new CompletableFuture<Void>();
    doReturn(cleanup).when(instance).deleteInstance();
    var deletion = server.deleteInstance(id).orElseThrow();
    try {
      assertFalse(deletion.isDone());
      var failure = assertThrows(StatusRuntimeException.class, () -> scriptContext.run(() -> {
        switch (mutation) {
          case CREATE, CREATE_PAUSED -> scripts.createScript(CreateScriptRequest.newBuilder().setInstanceId(id.toString())
            .setName("during deletion").setPaused(mutation == ScriptMutation.CREATE_PAUSED).build(), mock());
          case UPDATE -> scripts.updateScript(UpdateScriptRequest.newBuilder()
            .setInstanceId(id.toString()).setScriptId(scriptId).setPaused(false).build(), mock());
          case ACTIVATE -> scripts.activateScript(ActivateScriptRequest.newBuilder()
            .setInstanceId(id.toString()).setScriptId(scriptId).build(), activation);
        }
      }));
      assertEquals(Status.Code.FAILED_PRECONDITION, failure.getStatus().getCode());
      assertTrue(dsl.selectFrom(Tables.SCRIPTS).where(Tables.SCRIPTS.ID.eq(scriptId)).fetchSingle().getPaused());
      assertEquals(1, dsl.fetchCount(Tables.SCRIPTS));
    } finally {
      cleanup.complete(null);
      deletion.join();
      scripts.stopAllScripts(id);
    }
  }

  @Test
  void deletionWaitsForInFlightRegistrationAndStopsTheResultingActivation() throws Exception {
    var scripts = scriptService();
    var scriptId = pausedScript(scripts);
    var registering = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var deleting = new CountDownLatch(1);
    var triggers = spy(new ScriptTriggerService());
    doAnswer(call -> {
      registering.countDown();
      assertTrue(release.await(5, TimeUnit.SECONDS));
      return call.callRealMethod();
    }).when(triggers).registerTriggers(any(), any(), any(), any());
    Fields.setObject(scripts, Fields.getDeclaredField(ScriptServiceImpl.class, "triggerService"), triggers);
    var cleanup = new CompletableFuture<Void>();
    doReturn(cleanup).when(instance).deleteInstance();
    ServerCallStreamObserver<ScriptEvent> observer = mock();
    try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
      var activation = workers.submit(() -> scriptContext.run(() -> scripts.activateScript(ActivateScriptRequest.newBuilder()
        .setInstanceId(id.toString()).setScriptId(scriptId).build(), observer)));
      try {
        assertTrue(registering.await(5, TimeUnit.SECONDS));
        var deletion = workers.submit(() -> {
          deleting.countDown();
          return server.deleteInstance(id).orElseThrow();
        });
        assertTrue(deleting.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> deletion.get(100, TimeUnit.MILLISECONDS));
        release.countDown();
        activation.get(5, TimeUnit.SECONDS);
        var completion = deletion.get(5, TimeUnit.SECONDS);
        assertFalse(completion.isDone());
        verify(observer, times(1)).onCompleted();
        cleanup.complete(null);
        completion.join();
        assertTrue(server.getInstance(id).isEmpty());
      } finally {
        release.countDown();
        cleanup.complete(null);
      }
    } finally {
      scripts.stopAllScripts(id);
    }
  }

  @Test
  void deletionIsVisibleBeforeResourceCancellationWithoutARuntime() {
    server.instances().clear();
    doNothing().when(server).deleteInstanceStorage(id);
    var scripts = server.rpcServer().scriptService();
    doAnswer(_ -> { assertTrue(server.isInstanceDeleting(id)); return null; }).when(scripts).stopAllScripts(id);
    server.deleteInstance(id).orElseThrow().join();
    assertFalse(stored());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void unloadedProfileRejectsNewScriptsWhileStorageCleanupIsPending(boolean cancelCaller) {
    var scripts = scriptService();
    var scheduled = new ArrayDeque<Runnable>();
    var scheduler = server.scheduler();
    doAnswer(call -> { scheduled.add(call.getArgument(0)); return null; }).when(scheduler).execute(any(Runnable.class));
    doNothing().when(server).deleteInstanceStorage(id);
    server.instances().clear();
    var deletion = server.deleteInstance(id).orElseThrow();
    try {
      assertFalse(deletion.isDone());
      if (cancelCaller) deletion.cancel(false);
      var failure = assertThrows(StatusRuntimeException.class, () -> scriptContext.run(() ->
        scripts.createScript(CreateScriptRequest.newBuilder().setInstanceId(id.toString())
          .setName("during storage cleanup").setPaused(true).build(), mock())));
      assertEquals(Status.Code.FAILED_PRECONDITION, failure.getStatus().getCode());
      assertEquals(0, dsl.fetchCount(Tables.SCRIPTS));
    } finally {
      Runnable task;
      while ((task = scheduled.poll()) != null) task.run();
      if (!cancelCaller) deletion.join();
      assertFalse(stored());
    }
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

  private ScriptServiceImpl scriptService() {
    var scripts = new ScriptServiceImpl(server);
    var rpc = server.rpcServer();
    doReturn(scripts).when(rpc).scriptService();
    return scripts;
  }

  private String pausedScript(ScriptServiceImpl scripts) {
    var created = new AtomicReference<CreateScriptResponse>();
    StreamObserver<CreateScriptResponse> observer = mock();
    doAnswer(call -> { created.set(call.getArgument(0)); return null; }).when(observer).onNext(any());
    scriptContext.run(() -> scripts.createScript(CreateScriptRequest.newBuilder()
      .setInstanceId(id.toString()).setName("before deletion").setPaused(true).build(), observer));
    return created.get().getScript().getId();
  }

  private enum ScriptMutation { CREATE, CREATE_PAUSED, UPDATE, ACTIVATE }

  private boolean stored() {
    return dsl.fetchExists(Tables.INSTANCES, Tables.INSTANCES.ID.eq(id.toString()));
  }
}
