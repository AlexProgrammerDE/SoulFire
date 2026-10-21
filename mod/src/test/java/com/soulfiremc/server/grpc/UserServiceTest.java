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

import com.soulfiremc.grpc.generated.UpdateUserRequest;
import com.soulfiremc.grpc.generated.UserCreateRequest;
import com.soulfiremc.grpc.generated.UserCreateResponse;
import com.soulfiremc.grpc.generated.UserInfoRequest;
import com.soulfiremc.grpc.generated.UserInfoResponse;
import com.soulfiremc.grpc.generated.UserRole;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.SoulFireUser;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class UserServiceTest {
  @Test
  void createdUserCanBeReadAndUpdatedUsingTheResponseId(@TempDir Path directory) throws Exception {
    var source = new SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + directory.resolve("users.sqlite"));
    Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    var dsl = DSL.using(source, SQLDialect.SQLITE);
    var server = mock(SoulFireServer.class);
    when(server.dsl()).thenReturn(dsl);
    var root = mock(SoulFireUser.class);
    when(root.getUniqueId()).thenReturn(AuthSystem.ROOT_USER_ID);
    var context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, root);
    var service = new UserServiceImpl(server);

    var created = context.call(() -> UserServiceTest.<UserCreateResponse>call(observer -> service.createUser(UserCreateRequest.newBuilder()
      .setUsername("created-user").setEmail("created@example.com").setRole(UserRole.USER).build(), observer)));
    var id = UUID.fromString(created.getId());
    var stored = dsl.selectFrom(Tables.USERS).where(Tables.USERS.ID.eq(id.toString())).fetchSingle();
    assertEquals("created-user", stored.getUsername());

    var info = context.call(() -> UserServiceTest.<UserInfoResponse>call(observer -> service.getUserInfo(UserInfoRequest.newBuilder()
      .setId(created.getId()).build(), observer)));
    assertEquals(UserRole.USER, info.getRole());
    assertEquals(stored.getEmail(), info.getEmail());
    context.run(() -> service.updateUser(UpdateUserRequest.newBuilder().setId(created.getId())
      .setUsername("updated-user").setEmail("updated@example.com").setRole(UserRole.ADMIN).build(), mock()));
    var updated = dsl.selectFrom(Tables.USERS).where(Tables.USERS.ID.eq(id.toString())).fetchSingle();
    assertEquals("updated-user", updated.getUsername());
    assertEquals("updated@example.com", updated.getEmail());
    assertEquals(UserRole.ADMIN.name(), updated.getRole());
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
