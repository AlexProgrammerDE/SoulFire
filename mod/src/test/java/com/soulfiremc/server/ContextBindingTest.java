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

import com.soulfiremc.mod.util.SFConstants;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.script.SoulFireReactorScheduler;
import com.soulfiremc.server.util.netty.NettyHelper;
import com.soulfiremc.shared.SFLogAppender;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalIoHandler;
import net.lenni0451.reflect.Fields;
import net.lenni0451.reflect.Objects;
import net.minecraft.client.Minecraft;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContextBindingTest {
  @Test
  void restoresNestedBotContextsAndLoggingAfterCheckedExceptions() throws Exception {
    var outer = TestContext.create();
    var inner = TestContext.create();
    var failure = new IOException();

    assertUnbound();
    assertSame(failure, assertThrows(IOException.class, () -> outer.wrapper().runWrappedWithIOException(() -> {
      outer.assertBound();
      assertSame(failure, assertThrows(IOException.class, () -> inner.wrapper().runWrappedWithIOException(() -> {
        inner.assertBound();
        throw failure;
      })));
      outer.assertBound();
      throw failure;
    })));
    assertUnbound();
  }

  @Test
  void isolatesConcurrentSchedulerTasksAndRebindsDelayedReactorCallbacks() throws Exception {
    var first = TestContext.create();
    var second = TestContext.create();
    var firstScheduler = new SoulFireScheduler(first.wrapper());
    var secondScheduler = new SoulFireScheduler(second.wrapper());
    var ready = new CountDownLatch(2);

    try {
      var firstTask = firstScheduler.runAsync(() -> assertConcurrentContext(first, ready));
      var secondTask = secondScheduler.runAsync(() -> assertConcurrentContext(second, ready));
      firstTask.get(5, TimeUnit.SECONDS);
      secondTask.get(5, TimeUnit.SECONDS);

      var reactorScheduler = new SoulFireReactorScheduler(firstScheduler).withAdditionalWrapper(second.wrapper());
      var result = Mono.delay(Duration.ofMillis(1), reactorScheduler)
        .map(_ -> {
          second.assertBound();
          return BotConnection.current();
        })
        .block(Duration.ofSeconds(5));
      assertSame(second.bot(), result);
      assertSame(first.bot(), firstScheduler.supplyAsync(BotConnection::current).get(5, TimeUnit.SECONDS));
      assertUnbound();
    } finally {
      firstScheduler.shutdown();
      secondScheduler.shutdown();
    }
  }

  @Test
  void databaseContentionDoesNotDelayNetworkCallbacks(@TempDir Path directory) throws Exception {
    var config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:" + directory.resolve("names.sqlite"));
    config.setMaximumPoolSize(1);
    config.setConnectionTimeout(5_000);
    var context = TestContext.create();
    var updatedName = UUID.randomUUID().toString();
    try (var source = new HikariDataSource(config)) {
      var dsl = DSL.using(source, SQLDialect.SQLITE);
      var name = context.instance().id().toString();
      dsl.execute("CREATE TABLE instances (id TEXT PRIMARY KEY, friendly_name TEXT NOT NULL)");
      dsl.insertInto(Tables.INSTANCES).set(Tables.INSTANCES.ID, name).set(Tables.INSTANCES.FRIENDLY_NAME, name).execute();
      Fields.setObject(context.instance(), Fields.getDeclaredField(InstanceManager.class, "dsl"), dsl);
      context.instance().refreshFriendlyName();
      var loop = new MultiThreadIoEventLoopGroup(1, LocalIoHandler.newFactory());
      var channel = new LocalChannel();
      var received = new CompletableFuture<String>();
      NettyHelper.addRunnableWrapper("test-", context.wrapper(), channel);
      channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
        @Override
        public void channelRead(ChannelHandlerContext ignored, Object message) {
          try {
            context.assertBound();
            received.complete(MDC.get(SFLogAppender.SF_INSTANCE_NAME));
          } catch (Throwable error) {
            received.completeExceptionally(error);
          }
        }
      });
      try {
        loop.register(channel).syncUninterruptibly();
        // Keep the only SQL connection busy while the shared network thread reads a packet.
        try (var _ = source.getConnection()) {
          assertEquals(1, source.getHikariPoolMXBean().getActiveConnections());
          loop.submit(() -> channel.pipeline().fireChannelRead(new Object())).get(1, TimeUnit.SECONDS);
          assertEquals(name, received.get(1, TimeUnit.SECONDS));
        }
      } finally {
        channel.close().syncUninterruptibly();
        loop.shutdownGracefully(0, 1, TimeUnit.SECONDS).syncUninterruptibly();
      }
      dsl.update(Tables.INSTANCES).set(Tables.INSTANCES.FRIENDLY_NAME, updatedName)
        .where(Tables.INSTANCES.ID.eq(name)).execute();
      context.instance().refreshFriendlyName();
      assertEquals(updatedName, context.instance().friendlyName());
      context.wrapper().runWrapped(context::assertBound);
    }
    assertThrows(DataAccessException.class, context.instance()::refreshFriendlyName);
    assertEquals(updatedName, context.instance().friendlyName());
    context.wrapper().runWrapped(context::assertBound);
    assertUnbound();
  }

  private static void assertConcurrentContext(TestContext context, CountDownLatch ready) {
    context.assertBound();
    ready.countDown();
    try {
      assertTrue(ready.await(5, TimeUnit.SECONDS));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }
    context.assertBound();
  }

  private static void assertUnbound() {
    assertTrue(SoulFireServer.currentOptional().isEmpty());
    assertTrue(InstanceManager.currentOptional().isEmpty());
    assertTrue(BotConnection.currentOptional().isEmpty());
    assertNull(SFConstants.MINECRAFT_INSTANCE.get());
    assertNull(MDC.get(SFLogAppender.SF_INSTANCE_ID));
    assertNull(MDC.get(SFLogAppender.SF_INSTANCE_NAME));
    assertNull(MDC.get(SFLogAppender.SF_BOT_ACCOUNT_ID));
    assertNull(MDC.get(SFLogAppender.SF_BOT_ACCOUNT_NAME));
  }

  private record TestContext(
    SoulFireServer server,
    InstanceManager instance,
    BotConnection bot,
    SoulFireScheduler.RunnableWrapper wrapper
  ) {
    private static TestContext create() throws ReflectiveOperationException {
      var server = Objects.allocate(SoulFireServer.class);
      var instance = Objects.allocate(InstanceManager.class);
      var bot = Objects.allocate(BotConnection.class);
      var instanceId = UUID.randomUUID();
      var accountId = UUID.randomUUID();
      Fields.setObject(instance, Fields.getDeclaredField(InstanceManager.class, "id"), instanceId);
      Fields.setObject(instance, Fields.getDeclaredField(InstanceManager.class, "friendlyName"), instanceId.toString());
      Fields.setObject(bot, Fields.getDeclaredField(BotConnection.class, "accountProfileId"), accountId);
      Fields.setObject(bot, Fields.getDeclaredField(BotConnection.class, "accountName"), accountId.toString());
      Fields.setObject(bot, Fields.getDeclaredField(BotConnection.class, "minecraft"), Objects.allocate(Minecraft.class));
      return new TestContext(server, instance, bot, wrapperFor(server).with(wrapperFor(instance)).with(wrapperFor(bot)));
    }

    private static SoulFireScheduler.RunnableWrapper wrapperFor(Object owner) throws ReflectiveOperationException {
      var wrapperClass = Arrays.stream(owner.getClass().getDeclaredClasses())
        .filter(SoulFireScheduler.RunnableWrapper.class::isAssignableFrom)
        .findFirst().orElseThrow();
      var constructor = wrapperClass.getDeclaredConstructor(owner.getClass());
      constructor.setAccessible(true);
      return (SoulFireScheduler.RunnableWrapper) constructor.newInstance(owner);
    }

    private void assertBound() {
      assertSame(server, SoulFireServer.current());
      assertSame(instance, InstanceManager.current());
      assertSame(bot, BotConnection.current());
      assertSame(bot.minecraft(), Minecraft.getInstance());
      assertEquals(instance.id().toString(), MDC.get(SFLogAppender.SF_INSTANCE_ID));
      assertEquals(instance.friendlyName(), MDC.get(SFLogAppender.SF_INSTANCE_NAME));
      assertEquals(bot.accountProfileId().toString(), MDC.get(SFLogAppender.SF_BOT_ACCOUNT_ID));
      assertEquals(bot.accountName(), MDC.get(SFLogAppender.SF_BOT_ACCOUNT_NAME));
    }
  }
}
