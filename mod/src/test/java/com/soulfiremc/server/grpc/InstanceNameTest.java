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

import com.soulfiremc.grpc.generated.InstanceCreateRequest;
import com.soulfiremc.grpc.generated.InstanceCreateResponse;
import com.soulfiremc.grpc.generated.InstanceGetOrCreateRequest;
import com.soulfiremc.grpc.generated.InstanceGetOrCreateResponse;
import com.soulfiremc.grpc.generated.InstanceUpdateMetaRequest;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class InstanceNameTest {
  private final SoulFireServer server = mock();
  private final SoulFireUser user = mock();
  private final UUID ownerId = UUID.randomUUID();
  private DSLContext dsl;
  private InstanceServiceImpl service;
  private Context context;
  private UUID existingId;

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var source = new SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + directory.resolve("instances.sqlite"));
    Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    dsl = DSL.using(source, SQLDialect.SQLITE);
    when(server.dsl()).thenReturn(dsl);
    when(user.getUniqueId()).thenReturn(ownerId);
    when(server.getInstance(any())).thenReturn(Optional.empty());
    when(server.createInstance(anyString(), same(user))).thenAnswer(invocation -> insert(invocation.getArgument(0)));
    when(server.createInstance(anyString(), same(user), any(InstanceSettingsImpl.Stem.class))).thenAnswer(invocation -> insert(invocation.getArgument(0)));
    dsl.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      ownerId.toString(), "owner", "owner@example.com", "ADMIN");
    existingId = insert("Existing QA");
    service = new InstanceServiceImpl(server);
    context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, user);
  }

  @ParameterizedTest
  @MethodSource("invalidNames")
  void invalidNamesNeverChangeStoredInstances(String name) {
    var before = dsl.selectFrom(Tables.INSTANCES).orderBy(Tables.INSTANCES.ID).fetch().intoMaps();
    assertAll(
      () -> assertInvalid(() -> service.createInstance(InstanceCreateRequest.newBuilder().setFriendlyName(name).build(), mock())),
      () -> assertInvalid(() -> service.updateInstanceMeta(InstanceUpdateMetaRequest.newBuilder()
        .setId(existingId.toString()).setFriendlyName(name).build(), mock())),
      () -> assertInvalid(() -> service.getOrCreateInstance(InstanceGetOrCreateRequest.newBuilder().setName(name).build(), mock())),
      () -> assertEquals(before, dsl.selectFrom(Tables.INSTANCES).orderBy(Tables.INSTANCES.ID).fetch().intoMaps()));
  }

  @ParameterizedTest
  @MethodSource("validNames")
  void creationAndProvisioningUseTheSameNormalizedName(String name) throws Exception {
    var created = context.call(() -> InstanceNameTest.<InstanceCreateResponse>call(observer -> service.createInstance(
      InstanceCreateRequest.newBuilder().setFriendlyName(name).build(), observer)));
    var reused = context.call(() -> InstanceNameTest.<InstanceGetOrCreateResponse>call(observer -> service.getOrCreateInstance(
      InstanceGetOrCreateRequest.newBuilder().setName(" \u00a0" + name + "\ufeff ").build(), observer)));
    assertEquals(created.getId(), reused.getId());
    assertFalse(reused.getCreated());
    context.run(() -> service.updateInstanceMeta(InstanceUpdateMetaRequest.newBuilder().setId(existingId.toString())
      .setFriendlyName(" \u00a0" + name + "\ufeff ").build(), mock()));
    assertEquals(name, dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(existingId.toString())).fetchSingle().getFriendlyName());
  }

  @Test
  void missingMetadataFieldIsInvalidWithoutChangingTheInstance() {
    var before = dsl.selectFrom(Tables.INSTANCES).fetch().intoMaps();
    assertInvalid(() -> service.updateInstanceMeta(InstanceUpdateMetaRequest.newBuilder().setId(existingId.toString()).build(), mock()));
    assertEquals(before, dsl.selectFrom(Tables.INSTANCES).fetch().intoMaps());
  }

  private UUID insert(String name) {
    var id = UUID.randomUUID();
    var now = LocalDateTime.now(ZoneOffset.UTC);
    dsl.insertInto(Tables.INSTANCES)
      .set(Tables.INSTANCES.ID, id.toString()).set(Tables.INSTANCES.FRIENDLY_NAME, name)
      .set(Tables.INSTANCES.OWNER_ID, ownerId.toString()).set(Tables.INSTANCES.ICON, "pickaxe")
      .set(Tables.INSTANCES.SETTINGS, GsonInstance.CONFIG_GSON.toJson(InstanceSettingsImpl.Stem.EMPTY.serializeToTree()))
      .set(Tables.INSTANCES.CREATED_AT, now).set(Tables.INSTANCES.UPDATED_AT, now).set(Tables.INSTANCES.VERSION, 0L).execute();
    return id;
  }

  private static Stream<String> invalidNames() {
    return Stream.of("", "   ", "\u00a0\ufeff", "x".repeat(33), "first\nsecond", "first\rsecond", "first\tsecond", "first\u0000second", "first\u0085second", "first\u2028second");
  }

  private static Stream<String> validNames() {
    return Stream.of("x", "x".repeat(32), "Farm - A_1", "日本語 Farm");
  }

  private void assertInvalid(Runnable action) {
    var error = assertThrows(StatusRuntimeException.class, () -> context.run(action));
    assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
  }

  private static <T> T call(Consumer<StreamObserver<T>> action) {
    var result = new CompletableFuture<T>();
    action.accept(new StreamObserver<>() {
      @Override
      public void onNext(T value) { result.complete(value); }
      @Override
      public void onError(Throwable error) { result.completeExceptionally(error); }
      @Override
      public void onCompleted() { }
    });
    return result.join();
  }
}
