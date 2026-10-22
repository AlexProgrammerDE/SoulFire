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
package com.soulfiremc.server.settings.lib;

import com.soulfiremc.server.settings.instance.BotSettings;
import com.soulfiremc.server.settings.server.DevSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsContentionTest {
  @Test
  void instanceReadersDoNotWaitForDatabaseConnections(@TempDir Path directory) throws Exception {
    try (var pool = pool(directory)) {
      var dsl = DSL.using(pool, SQLDialect.SQLITE);
      var server = new ServerSettingsImpl(ServerSettingsImpl.Stem.EMPTY);
      var source = new InstanceSettingsDelegate(() -> {
        dsl.fetchValue("SELECT 1");
        return new InstanceSettingsImpl(InstanceSettingsImpl.Stem.EMPTY, server);
      });
      var expected = source.get(BotSettings.ADDRESS);
      try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var held = pool.getConnection();
        try {
          var read = executor.submit(() -> source.get(BotSettings.ADDRESS));
          assertEquals(expected, read.get(1, TimeUnit.SECONDS));
        } finally {
          held.close();
        }
      }
    }
  }

  @Test
  void serverReadersDoNotWaitForDatabaseConnections(@TempDir Path directory) throws Exception {
    try (var pool = pool(directory)) {
      var dsl = DSL.using(pool, SQLDialect.SQLITE);
      var source = new ServerSettingsDelegate(() -> {
        dsl.fetchValue("SELECT 1");
        return new ServerSettingsImpl(ServerSettingsImpl.Stem.EMPTY);
      });
      var expected = source.get(DevSettings.SOULFIRE_DEBUG);
      try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var held = pool.getConnection();
        try {
          var read = executor.submit(() -> source.get(DevSettings.SOULFIRE_DEBUG));
          assertEquals(expected, read.get(1, TimeUnit.SECONDS));
        } finally {
          held.close();
        }
      }
    }
  }

  @Test
  void readersKeepThePreviousSnapshotWhileAnotherThreadRefreshes() throws Exception {
    var original = new ServerSettingsImpl(ServerSettingsImpl.Stem.EMPTY);
    var replacement = new ServerSettingsImpl(new ServerSettingsImpl.Stem(Map.of()));
    var next = new AtomicReference<ServerSettingsSource>(original);
    var loading = new CountDownLatch(1);
    var proceed = new CountDownLatch(1);
    var block = new AtomicBoolean();
    var source = new ServerSettingsDelegate(() -> {
      if (block.get()) {
        loading.countDown();
        try {
          if (!proceed.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Refresh did not resume");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException(e);
        }
      }
      return next.get();
    });
    next.set(replacement);
    block.set(true);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var refresh = executor.submit(source::invalidate);
      try {
        assertTrue(loading.await(1, TimeUnit.SECONDS));
        assertSame(original.stem(), executor.submit(source::stem).get(1, TimeUnit.SECONDS));
      } finally {
        proceed.countDown();
      }
      refresh.get(1, TimeUnit.SECONDS);
      assertSame(replacement.stem(), source.stem());
    }
  }

  @Test
  void aFailedRefreshKeepsTheLastSettingsUntilASuccessfulRetry() {
    var server = new ServerSettingsImpl(ServerSettingsImpl.Stem.EMPTY);
    var original = new InstanceSettingsImpl(InstanceSettingsImpl.Stem.EMPTY, server);
    var next = new AtomicReference<InstanceSettingsSource>(original);
    var source = new InstanceSettingsDelegate(() -> {
      var value = next.get();
      if (value == null) {
        throw new IllegalStateException("Database unavailable");
      }
      return value;
    });
    next.set(null);
    assertThrows(IllegalStateException.class, source::invalidate);
    assertSame(server, source.serverSettings());
    assertSame(original.stem(), source.stem());
    var replacement = new InstanceSettingsImpl(new InstanceSettingsImpl.Stem(Map.of(), List.of(), List.of(), Map.of()), server);
    next.set(replacement);
    source.invalidate();
    assertSame(replacement.stem(), source.stem());
  }

  private static HikariDataSource pool(Path directory) {
    var config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:" + directory.resolve("settings.sqlite"));
    config.setMaximumPoolSize(1);
    config.setConnectionTimeout(5_000);
    return new HikariDataSource(config);
  }
}
