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

import com.soulfiremc.grpc.generated.BotDesiredState;
import com.soulfiremc.server.account.AuthType;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.service.OfflineJavaData;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotConnectionFactory;
import com.soulfiremc.server.bot.BotControlLeaseManager;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.settings.instance.AccountSettings;
import com.soulfiremc.server.settings.instance.BotSettings;
import com.soulfiremc.server.settings.lib.InstanceSettingsDelegate;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.user.SoulFireUser;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.TransactionalRunnable;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class BotStateManagerTest {
  static Stream<Arguments> overlappingIntents() {
    return Stream.of(
      Arguments.of(Intent.START, Intent.STOP),
      Arguments.of(Intent.STOP, Intent.START),
      Arguments.of(Intent.RESTART, Intent.STOP),
      Arguments.of(Intent.STOP, Intent.RESTART),
      Arguments.of(Intent.REMOVE, Intent.START));
  }

  @ParameterizedTest
  @MethodSource("overlappingIntents")
  void overlappingCommandsKeepSavedAndReportedIntentConsistent(
    Intent first, Intent second, @TempDir Path directory) throws Exception {
    var fixture = fixture(directory, first == Intent.STOP || first == Intent.REMOVE);
    var manager = fixture.manager();
    var instance = fixture.instance();
    var intercepted = fixture.dsl();
    var initiator = fixture.user();
    var botId = fixture.botId();
    var firstThread = new AtomicReference<Thread>();
    var committed = new CountDownLatch(1);
    var resume = new CountDownLatch(1);
    doAnswer(invocation -> {
      invocation.callRealMethod();
      if (Thread.currentThread() == firstThread.get()) {
        committed.countDown();
        assertTrue(resume.await(5, TimeUnit.SECONDS));
      }
      return null;
    }).when(intercepted).transaction(any(TransactionalRunnable.class));

    try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
      var earlier = workers.submit(() -> {
        firstThread.set(Thread.currentThread());
        first.apply(manager, initiator, botId);
      });
      try {
        assertTrue(committed.await(5, TimeUnit.SECONDS));
        workers.submit(() -> manager.status(botId)).get(1, TimeUnit.SECONDS);
        var later = workers.submit(() -> second.apply(manager, initiator, botId));
        try {
          later.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException _) {
          // A serialized command can wait until the committed transition resumes.
        } finally {
          resume.countDown();
        }
        earlier.get(5, TimeUnit.SECONDS);
        later.get(5, TimeUnit.SECONDS);
      } finally {
        resume.countDown();
      }
    }

    assertEquals(new BotStateManager(instance).desiredBotIdsSnapshot(), manager.desiredBotIdsSnapshot());
  }

  @ParameterizedTest
  @EnumSource(value = Intent.class, names = {"STOP", "RESTART", "REMOVE"})
  void retiredPreparationNeverRegistersOrConnects(Intent cancellation, @TempDir Path directory) throws Exception {
    var fixture = fixture(directory, false);
    var scheduled = new AtomicReference<Runnable>();
    doAnswer(invocation -> {
      scheduled.set(invocation.getArgument(0));
      return null;
    }).when(fixture.scheduler()).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
    fixture.manager().setDesiredState(fixture.user(), List.of(fixture.botId()), BotDesiredState.BOT_DESIRED_STATE_RUNNING);
    var launch = scheduled.get();
    var prepared = new CountDownLatch(1);
    var resume = new CountDownLatch(1);
    var connection = mock(BotConnection.class);
    when(connection.accountProfileId()).thenReturn(fixture.botId());
    when(connection.connect()).thenAnswer(_ -> CompletableFuture.completedFuture(null));
    when(connection.disconnect(any())).thenReturn(CompletableFuture.completedFuture(null));
    try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
      var pending = workers.submit(() -> {
        try (var factories = mockConstruction(BotConnectionFactory.class, (factory, _) ->
          when(factory.prepareConnection(false)).thenAnswer(_ -> {
            prepared.countDown();
            assertTrue(resume.await(5, TimeUnit.SECONDS));
            return connection;
          }))) {
          launch.run();
        }
      });
      try {
        assertTrue(prepared.await(5, TimeUnit.SECONDS));
        cancellation.apply(fixture.manager(), fixture.user(), fixture.botId());
        if (cancellation == Intent.REMOVE) when(fixture.settings().accounts()).thenReturn(Map.of());
      } finally {
        resume.countDown();
      }
      pending.get(5, TimeUnit.SECONDS);
    }
    assertTrue(fixture.connections().isEmpty());
    verify(connection, never()).connect();
    verify(connection).disconnect(any());
  }

  @Test
  void cancellationCannotRetireARegisteredConnectionBeforeItsStartIsScheduled(@TempDir Path directory) throws Exception {
    var fixture = fixture(directory, false);
    var scheduled = new AtomicReference<Runnable>();
    doAnswer(invocation -> {
      scheduled.set(invocation.getArgument(0));
      return null;
    }).when(fixture.scheduler()).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
    fixture.manager().setDesiredState(fixture.user(), List.of(fixture.botId()), BotDesiredState.BOT_DESIRED_STATE_RUNNING);
    var launch = scheduled.get();
    var registered = new CountDownLatch(1);
    var resume = new CountDownLatch(1);
    var calls = new CopyOnWriteArrayList<ConnectionStep>();
    var connection = mock(BotConnection.class);
    when(connection.accountProfileId()).thenReturn(fixture.botId());
    when(connection.connect()).thenAnswer(_ -> {
      calls.add(ConnectionStep.CONNECT);
      return CompletableFuture.completedFuture(null);
    });
    when(connection.disconnect(any())).thenAnswer(_ -> {
      calls.add(ConnectionStep.DISCONNECT);
      return CompletableFuture.completedFuture(null);
    });
    doAnswer(_ -> {
      fixture.connections().put(fixture.botId(), connection);
      registered.countDown();
      assertTrue(resume.await(5, TimeUnit.SECONDS));
      return true;
    }).when(fixture.instance()).storeNewBot(connection);
    doAnswer(_ -> {
      if (fixture.connections().remove(fixture.botId(), connection)) fixture.manager().connectionRemoved(connection);
      return null;
    }).when(fixture.instance()).removeBot(connection);

    try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
      var pending = workers.submit(() -> {
        try (var factories = mockConstruction(BotConnectionFactory.class, (factory, _) ->
          when(factory.prepareConnection(false)).thenReturn(connection))) {
          launch.run();
        }
      });
      try {
        assertTrue(registered.await(5, TimeUnit.SECONDS));
        var stop = workers.submit(() -> Intent.STOP.apply(fixture.manager(), fixture.user(), fixture.botId()));
        try {
          stop.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException _) {
          // Registration must finish scheduling before cancellation can retire it.
        } finally {
          resume.countDown();
        }
        pending.get(5, TimeUnit.SECONDS);
        stop.get(5, TimeUnit.SECONDS);
      } finally {
        resume.countDown();
      }
    }
    assertEquals(List.of(ConnectionStep.CONNECT, ConnectionStep.DISCONNECT), calls);
    assertTrue(fixture.connections().isEmpty());
  }

  @ParameterizedTest
  @EnumSource(Removal.class)
  void readdingAnAccountCannotReviveItsRetiredPreparation(Removal removal, @TempDir Path directory) throws Exception {
    var fixture = fixture(directory, false);
    var accounts = fixture.settings().accounts();
    var scheduled = new AtomicReference<Runnable>();
    doAnswer(invocation -> {
      scheduled.set(invocation.getArgument(0));
      return null;
    }).when(fixture.scheduler()).schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS));
    fixture.manager().setDesiredState(fixture.user(), List.of(fixture.botId()), BotDesiredState.BOT_DESIRED_STATE_RUNNING);
    var retiredLaunch = scheduled.get();
    var prepared = new CountDownLatch(1);
    var resume = new CountDownLatch(1);
    var retired = mock(BotConnection.class);
    when(retired.accountProfileId()).thenReturn(fixture.botId());
    when(retired.connect()).thenReturn(CompletableFuture.completedFuture(null));
    when(retired.disconnect(any())).thenReturn(CompletableFuture.completedFuture(null));
    try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
      var pending = workers.submit(() -> {
        try (var factories = mockConstruction(BotConnectionFactory.class, (factory, _) ->
          when(factory.prepareConnection(false)).thenAnswer(_ -> {
            prepared.countDown();
            assertTrue(resume.await(5, TimeUnit.SECONDS));
            return retired;
          }))) {
          retiredLaunch.run();
        }
      });
      try {
        assertTrue(prepared.await(5, TimeUnit.SECONDS));
        if (removal == Removal.EXPLICIT) fixture.manager().removeAccounts(List.of(fixture.botId())).join();
        when(fixture.settings().accounts()).thenReturn(Map.of());
        fixture.manager().syncConfiguredAccounts();
        when(fixture.settings().accounts()).thenReturn(accounts);
        fixture.manager().syncConfiguredAccounts();
        fixture.manager().setDesiredState(fixture.user(), List.of(fixture.botId()), BotDesiredState.BOT_DESIRED_STATE_RUNNING);
      } finally {
        resume.countDown();
      }
      pending.get(5, TimeUnit.SECONDS);
    }
    assertTrue(fixture.connections().isEmpty());
    verify(retired, never()).connect();
    verify(retired).disconnect(any());

    var current = mock(BotConnection.class);
    when(current.accountProfileId()).thenReturn(fixture.botId());
    when(current.connect()).thenReturn(CompletableFuture.completedFuture(null));
    assertNotSame(retiredLaunch, scheduled.get());
    try (var factories = mockConstruction(BotConnectionFactory.class, (factory, _) ->
      when(factory.prepareConnection(false)).thenReturn(current))) {
      scheduled.get().run();
    }
    assertSame(current, fixture.connections().get(fixture.botId()));
    verify(current).connect();
  }

  private enum Removal { EXPLICIT, CONFIGURATION }

  private enum ConnectionStep { CONNECT, DISCONNECT }

  private static Fixture fixture(Path directory, boolean initiallyRunning) {
    var source = new SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + directory.resolve("intent.sqlite"));
    Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    var stored = DSL.using(source, SQLDialect.SQLITE);
    var instanceId = UUID.randomUUID();
    var ownerId = UUID.randomUUID().toString();
    stored.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, 'owner', 'owner@example.com', 'ADMIN', CURRENT_TIMESTAMP)", ownerId);
    stored.insertInto(Tables.INSTANCES)
      .set(Tables.INSTANCES.ID, instanceId.toString())
      .set(Tables.INSTANCES.FRIENDLY_NAME, "intent")
      .set(Tables.INSTANCES.ICON, "pickaxe")
      .set(Tables.INSTANCES.OWNER_ID, ownerId)
      .set(Tables.INSTANCES.SETTINGS, "{}")
      .execute();
    var account = new MinecraftAccount(AuthType.OFFLINE, UUID.randomUUID(), "IntentBot", new OfflineJavaData(), Map.of(), Map.of());
    var botId = account.profileId();
    if (initiallyRunning) {
      stored.insertInto(Tables.INSTANCE_DESIRED_BOTS)
        .set(Tables.INSTANCE_DESIRED_BOTS.INSTANCE_ID, instanceId.toString())
        .set(Tables.INSTANCE_DESIRED_BOTS.PROFILE_ID, botId.toString()).execute();
    }
    var settings = mock(InstanceSettingsDelegate.class);
    when(settings.stem()).thenReturn(InstanceSettingsImpl.Stem.EMPTY);
    when(settings.accounts()).thenReturn(Map.of(botId, account));
    when(settings.proxies()).thenReturn(List.of());
    when(settings.get(AccountSettings.SHUFFLE_ACCOUNTS)).thenReturn(false);
    when(settings.get(BotSettings.CONCURRENT_CONNECTS)).thenReturn(1);
    when(settings.get(BotSettings.ADDRESS)).thenReturn("127.0.0.1:25565");
    when(settings.getRandom(BotSettings.JOIN_DELAY)).thenReturn(() -> 0);
    var instance = mock(InstanceManager.class);
    when(instance.id()).thenReturn(instanceId);
    when(instance.settingsSource()).thenReturn(settings);
    var connections = new ConcurrentHashMap<UUID, BotConnection>();
    when(instance.botConnections()).thenReturn(connections);
    var scheduler = mock(SoulFireScheduler.class);
    when(instance.scheduler()).thenReturn(scheduler);
    when(instance.refreshAccount(account)).thenReturn(account);
    when(instance.storeNewBot(any())).thenAnswer(invocation -> connections.putIfAbsent(botId, invocation.getArgument(0)) == null);
    when(instance.botControlLeaseManager()).thenReturn(mock(BotControlLeaseManager.class));
    var intercepted = spy(stored);
    when(instance.dsl()).thenReturn(intercepted);
    var manager = new BotStateManager(instance);
    var initiator = mock(SoulFireUser.class);
    return new Fixture(instance, settings, intercepted, manager, initiator, botId, connections, scheduler);
  }

  private record Fixture(InstanceManager instance, InstanceSettingsDelegate settings, DSLContext dsl,
                         BotStateManager manager, SoulFireUser user, UUID botId,
                         ConcurrentHashMap<UUID, BotConnection> connections, SoulFireScheduler scheduler) {}

  private enum Intent {
    START, STOP, RESTART, REMOVE;

    void apply(BotStateManager manager, SoulFireUser user, UUID botId) {
      if (this == REMOVE) {
        manager.removeAccounts(List.of(botId)).join();
      } else if (this == RESTART) {
        manager.restartBots(user, List.of(botId));
      } else {
        manager.setDesiredState(user, List.of(botId), this == START
          ? BotDesiredState.BOT_DESIRED_STATE_RUNNING : BotDesiredState.BOT_DESIRED_STATE_STOPPED);
      }
    }
  }
}
