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
package com.soulfiremc.server.grpc;

import com.soulfiremc.grpc.generated.BotAuthentication;
import com.soulfiremc.grpc.generated.InstanceGetOrCreateBotRequest;
import com.soulfiremc.grpc.generated.InstanceGetOrCreateBotResponse;
import com.soulfiremc.grpc.generated.InstanceGetOrCreateRequest;
import com.soulfiremc.grpc.generated.InstanceGetOrCreateResponse;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class InstanceProvisioningTest {
  @Test
  void concurrentProvisioningReusesRecordsAndRejectsConflicts(@TempDir Path directory) throws Exception {
    var dataSource = new SQLiteDataSource();
    dataSource.setUrl("jdbc:sqlite:" + directory.resolve("instances.sqlite"));
    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    var dsl = DSL.using(dataSource, SQLDialect.SQLITE);
    var ownerId = UUID.randomUUID();
    dsl.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      ownerId.toString(), "owner", "owner@example.com", "ADMIN");
    var user = mock(SoulFireUser.class);
    when(user.getUniqueId()).thenReturn(ownerId);
    when(user.hasPermission(any())).thenReturn(true);
    var server = mock(SoulFireServer.class);
    when(server.dsl()).thenReturn(dsl);
    when(server.getInstance(any())).thenReturn(Optional.empty());
    when(server.createInstance(anyString(), same(user), any(InstanceSettingsImpl.Stem.class))).thenAnswer(invocation -> {
      var id = UUID.randomUUID();
      InstanceSettingsImpl.Stem settings = invocation.getArgument(2);
      dsl.insertInto(Tables.INSTANCES)
        .set(Tables.INSTANCES.ID, id.toString())
        .set(Tables.INSTANCES.FRIENDLY_NAME, invocation.<String>getArgument(0))
        .set(Tables.INSTANCES.OWNER_ID, ownerId.toString())
        .set(Tables.INSTANCES.ICON, "pickaxe")
        .set(Tables.INSTANCES.SETTINGS, GsonInstance.CONFIG_GSON.toJson(settings.serializeToTree()))
        .set(Tables.INSTANCES.CREATED_AT, LocalDateTime.now())
        .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now())
        .set(Tables.INSTANCES.VERSION, 0L).execute();
      return id;
    });
    var service = new InstanceServiceImpl(server);
    var context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, user);
    var request = InstanceGetOrCreateRequest.newBuilder().setName("farm").setServer("localhost:25565").build();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first = executor.submit(() -> context.call(() -> InstanceProvisioningTest.<InstanceGetOrCreateResponse>call(observer -> service.getOrCreateInstance(request, observer))));
      var second = executor.submit(() -> context.call(() -> InstanceProvisioningTest.<InstanceGetOrCreateResponse>call(observer -> service.getOrCreateInstance(request, observer))));
      var one = first.get();
      var two = second.get();
      assertEquals(one.getId(), two.getId());
      assertNotEquals(one.getCreated(), two.getCreated());
      verify(server, times(1)).createInstance(anyString(), same(user), any(InstanceSettingsImpl.Stem.class));
      var botRequest = InstanceGetOrCreateBotRequest.newBuilder().setId(one.getId()).setName("Builder")
        .setAuth(BotAuthentication.BOT_AUTHENTICATION_OFFLINE).build();
      var botOne = executor.submit(() -> context.call(() -> InstanceProvisioningTest.<InstanceGetOrCreateBotResponse>call(observer -> service.getOrCreateBot(botRequest, observer))));
      var botTwo = executor.submit(() -> context.call(() -> InstanceProvisioningTest.<InstanceGetOrCreateBotResponse>call(observer -> service.getOrCreateBot(botRequest, observer))));
      var botFirst = botOne.get();
      var botSecond = botTwo.get();
      assertEquals(botFirst.getBotId(), botSecond.getBotId());
      assertNotEquals(botFirst.getCreated(), botSecond.getCreated());
      var conflict = request.toBuilder().setServer("localhost:25566").build();
      var error = assertThrows(StatusRuntimeException.class,
        () -> context.call(() -> InstanceProvisioningTest.<InstanceGetOrCreateResponse>call(observer -> service.getOrCreateInstance(conflict, observer))));
      assertEquals(Status.Code.FAILED_PRECONDITION, error.getStatus().getCode());
    }
  }

  private static <T> T call(Consumer<StreamObserver<T>> action) {
    var result = new CompletableFuture<T>();
    action.accept(new StreamObserver<>() {
      @Override
      public void onNext(T value) {
        result.complete(value);
      }

      @Override
      public void onError(Throwable error) {
        result.completeExceptionally(error);
      }

      @Override
      public void onCompleted() {
      }
    });
    return result.join();
  }
}
