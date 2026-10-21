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

import com.soulfiremc.grpc.generated.UpdateSelfEmailRequest;
import com.soulfiremc.grpc.generated.UpdateSelfUsernameRequest;
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
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class UserServiceTest {
  private final UUID existingId = UUID.randomUUID();
  private final SoulFireServer server = mock();
  private final UserServiceImpl service = new UserServiceImpl(server);
  private final ClientServiceImpl clientService = new ClientServiceImpl(server);
  private DSLContext dsl;
  private Context context;
  private Context selfContext;

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var source = new SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + directory.resolve("users.sqlite"));
    Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    dsl = DSL.using(source, SQLDialect.SQLITE);
    when(server.dsl()).thenReturn(dsl);
    var root = mock(SoulFireUser.class);
    when(root.getUniqueId()).thenReturn(AuthSystem.ROOT_USER_ID);
    context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, root);
    var self = mock(SoulFireUser.class);
    when(self.getUniqueId()).thenReturn(existingId);
    selfContext = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, self);
    dsl.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      existingId.toString(), "existing-user", "existing@example.com", "USER");
  }

  @Test
  void createdUserCanBeReadAndUpdatedUsingTheResponseId() throws Exception {
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

  @ParameterizedTest
  @MethodSource("invalidUsernames")
  void invalidUsernamesNeverChangeStoredUsers(String username) {
    var before = dsl.selectFrom(Tables.USERS).orderBy(Tables.USERS.ID).fetch().intoMaps();
    assertAll(
      () -> assertInvalid(context, () -> service.createUser(UserCreateRequest.newBuilder()
        .setUsername(username).setEmail("new@example.com").setRole(UserRole.USER).build(), mock())),
      () -> assertInvalid(context, () -> service.updateUser(UpdateUserRequest.newBuilder().setId(existingId.toString())
        .setUsername(username).setEmail("updated@example.com").setRole(UserRole.ADMIN).build(), mock())),
      () -> assertInvalid(selfContext, () -> clientService.updateSelfUsername(UpdateSelfUsernameRequest.newBuilder()
        .setUsername(username).build(), mock())),
      () -> assertEquals(before, dsl.selectFrom(Tables.USERS).orderBy(Tables.USERS.ID).fetch().intoMaps()));
  }

  @ParameterizedTest
  @MethodSource("invalidEmails")
  void invalidEmailsNeverChangeStoredUsers(String email) {
    var before = dsl.selectFrom(Tables.USERS).orderBy(Tables.USERS.ID).fetch().intoMaps();
    assertAll(
      () -> assertInvalid(context, () -> service.createUser(UserCreateRequest.newBuilder()
        .setUsername("new-user").setEmail(email).setRole(UserRole.USER).build(), mock())),
      () -> assertInvalid(context, () -> service.updateUser(UpdateUserRequest.newBuilder().setId(existingId.toString())
        .setUsername("updated-user").setEmail(email).setRole(UserRole.ADMIN).build(), mock())),
      () -> assertInvalid(selfContext, () -> clientService.updateSelfEmail(UpdateSelfEmailRequest.newBuilder()
        .setEmail(email).build(), mock())),
      () -> assertEquals(before, dsl.selectFrom(Tables.USERS).orderBy(Tables.USERS.ID).fetch().intoMaps()));
  }

  @Test
  void validBoundaryUsernamesAndEmailAliasesCanBeSaved() {
    for (var username : List.of("a-b", "a".repeat(32))) {
      context.run(() -> service.createUser(UserCreateRequest.newBuilder().setUsername(username)
        .setEmail(username + "+tag@sub.example.com").setRole(UserRole.USER).build(), mock()));
    }
    selfContext.run(() -> clientService.updateSelfUsername(UpdateSelfUsernameRequest.newBuilder().setUsername("self-user").build(), mock()));
    selfContext.run(() -> clientService.updateSelfEmail(UpdateSelfEmailRequest.newBuilder().setEmail("self+tag@sub.example.com").build(), mock()));
    var stored = dsl.selectFrom(Tables.USERS).where(Tables.USERS.ID.eq(existingId.toString())).fetchSingle();
    assertEquals("self-user", stored.getUsername());
    assertEquals("self+tag@sub.example.com", stored.getEmail());
    assertEquals(3, dsl.fetchCount(Tables.USERS));
  }

  private static Stream<String> invalidUsernames() {
    return Stream.of("", "ab", "-user", "user-", "Upper", "white space", "under_score", "müller", "a".repeat(33));
  }

  private static Stream<String> invalidEmails() {
    return Stream.of("", "invalid", "user@", "@example.com", "user @example.com", "user@example.com trailing", "a".repeat(256) + "@example.com");
  }

  private static void assertInvalid(Context context, Runnable action) {
    var error = assertThrows(StatusRuntimeException.class, () -> context.run(action));
    assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
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
