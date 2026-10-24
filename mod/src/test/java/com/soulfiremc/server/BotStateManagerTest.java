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
import com.soulfiremc.server.bot.BotControlLeaseManager;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.settings.instance.AccountSettings;
import com.soulfiremc.server.settings.instance.BotSettings;
import com.soulfiremc.server.settings.lib.InstanceSettingsDelegate;
import com.soulfiremc.server.user.SoulFireUser;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.TransactionalRunnable;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
    if (first == Intent.STOP || first == Intent.REMOVE) {
      stored.insertInto(Tables.INSTANCE_DESIRED_BOTS)
        .set(Tables.INSTANCE_DESIRED_BOTS.INSTANCE_ID, instanceId.toString())
        .set(Tables.INSTANCE_DESIRED_BOTS.PROFILE_ID, botId.toString()).execute();
    }
    var settings = mock(InstanceSettingsDelegate.class);
    when(settings.accounts()).thenReturn(Map.of(botId, account));
    when(settings.proxies()).thenReturn(List.of());
    when(settings.get(AccountSettings.SHUFFLE_ACCOUNTS)).thenReturn(false);
    when(settings.get(BotSettings.CONCURRENT_CONNECTS)).thenReturn(1);
    when(settings.getRandom(BotSettings.JOIN_DELAY)).thenReturn(() -> 0);
    var instance = mock(InstanceManager.class);
    when(instance.id()).thenReturn(instanceId);
    when(instance.settingsSource()).thenReturn(settings);
    when(instance.botConnections()).thenReturn(new ConcurrentHashMap<>());
    when(instance.scheduler()).thenReturn(mock(SoulFireScheduler.class));
    when(instance.botControlLeaseManager()).thenReturn(mock(BotControlLeaseManager.class));
    var intercepted = spy(stored);
    when(instance.dsl()).thenReturn(intercepted);
    var manager = new BotStateManager(instance);
    var initiator = mock(SoulFireUser.class);
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
