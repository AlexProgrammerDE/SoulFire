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

import com.google.gson.JsonPrimitive;
import com.soulfiremc.grpc.generated.GlobalPermission;
import com.soulfiremc.grpc.generated.InstanceDuplicateRequest;
import com.soulfiremc.grpc.generated.InstanceDuplicateResponse;
import com.soulfiremc.grpc.generated.InstancePermission;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.account.AuthType;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.service.OfflineJavaData;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.proxy.ProxyType;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.user.PermissionContext;
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

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class InstanceDuplicationTest {
  private final SoulFireServer server = mock();
  private final SoulFireUser user = mock();
  private final UUID ownerId = UUID.randomUUID();
  private final UUID sourceOwnerId = UUID.randomUUID();
  private DSLContext dsl;
  private InstanceServiceImpl service;
  private Context context;
  private UUID sourceId;

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var source = new org.sqlite.SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + directory.resolve("instances.sqlite"));
    Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    dsl = DSL.using(source, SQLDialect.SQLITE);
    when(server.dsl()).thenReturn(dsl);
    when(user.getUniqueId()).thenReturn(ownerId);
    when(server.createInstance(anyString(), same(user), any(InstanceSettingsImpl.Stem.class), anyString()))
      .thenAnswer(invocation -> insert(invocation.getArgument(0), ownerId, invocation.getArgument(2), invocation.getArgument(3)));
    for (var id : List.of(ownerId, sourceOwnerId)) {
      dsl.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
        id.toString(), id.toString(), id + "@example.com", "USER");
    }
    var account = new MinecraftAccount(AuthType.OFFLINE, UUID.randomUUID(), "CopyBot", new OfflineJavaData(),
      Map.of("bot", Map.of("enabled", new JsonPrimitive(false))),
      Map.of("plugin", Map.of("counter", new JsonPrimitive(3))));
    var settings = new InstanceSettingsImpl.Stem(
      Map.of("bot", Map.of("server", new JsonPrimitive("localhost:25566"))),
      List.of(account), List.of(new SFProxy(ProxyType.SOCKS5, new InetSocketAddress("127.0.0.1", 1080), "proxy-user", "proxy-pass")),
      Map.of("plugin", Map.of("counter", new JsonPrimitive(7))));
    sourceId = insert("Source", sourceOwnerId, settings, "apple");
    service = new InstanceServiceImpl(server);
    context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, user);
  }

  @Test
  void copyPreservesTheCompleteProfileAndBelongsToTheCaller() throws Exception {
    var source = dsl.selectFrom(Tables.INSTANCES).fetchSingle();
    var id = duplicate(sourceId.toString(), " Copied profile ").getId();
    var copy = dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(id)).fetchSingle();
    assertNotEquals(sourceId.toString(), id);
    assertEquals("Copied profile", copy.getFriendlyName());
    assertEquals(ownerId.toString(), copy.getOwnerId());
    assertEquals(source.getSettings(), copy.getSettings());
    assertEquals(source.getIcon(), copy.getIcon());
    assertEquals(source, dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(sourceId.toString())).fetchSingle());
    dsl.update(Tables.INSTANCES).set(Tables.INSTANCES.SETTINGS, "{}").where(Tables.INSTANCES.ID.eq(id)).execute();
    assertEquals(source.getSettings(), dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(sourceId.toString())).fetchSingle().getSettings());
  }

  @Test
  void failedInsertLeavesNoEmptyInstanceAndRetryCreatesOneCompleteCopy() throws Exception {
    var before = dsl.selectFrom(Tables.INSTANCES).fetch().intoMaps();
    var sourceSettings = dsl.selectFrom(Tables.INSTANCES).fetchSingle().getSettings();
    dsl.execute("CREATE TRIGGER reject_copy BEFORE INSERT ON instances WHEN NEW.friendly_name = 'Copy' BEGIN SELECT RAISE(ABORT, 'Write rejected'); END");
    try {
      assertCode(Status.Code.INTERNAL, () -> duplicate(sourceId.toString(), "Copy"));
      assertEquals(before, dsl.selectFrom(Tables.INSTANCES).fetch().intoMaps());
    } finally {
      dsl.execute("DROP TRIGGER reject_copy");
    }
    duplicate(sourceId.toString(), "Copy");
    assertEquals(2, dsl.fetchCount(Tables.INSTANCES));
    var copy = dsl.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.FRIENDLY_NAME.eq("Copy")).fetchSingle();
    assertEquals(sourceSettings, copy.getSettings());
    assertEquals("apple", copy.getIcon());
  }

  @Test
  void invalidInputAndMissingSourcesCannotCreateAnInstance() {
    assertCode(Status.Code.INVALID_ARGUMENT, () -> duplicate("invalid", "Copy"));
    for (var name : List.of("", "   ", "x".repeat(33), "first\nsecond")) {
      assertCode(Status.Code.INVALID_ARGUMENT, () -> duplicate(sourceId.toString(), name));
    }
    assertCode(Status.Code.NOT_FOUND, () -> duplicate(UUID.randomUUID().toString(), "Copy"));
    verify(server, never()).createInstance(anyString(), same(user), any(InstanceSettingsImpl.Stem.class), anyString());
    assertEquals(1, dsl.fetchCount(Tables.INSTANCES));
  }

  @Test
  void creationPermissionIsRequired() {
    requirePermission(PermissionContext.global(GlobalPermission.CREATE_INSTANCE));
  }

  @Test
  void sourceReadPermissionIsRequired() {
    requirePermission(PermissionContext.instance(InstancePermission.READ_INSTANCE, sourceId));
  }

  private void requirePermission(PermissionContext permission) {
    doThrow(Status.PERMISSION_DENIED.asRuntimeException()).when(user).hasPermissionOrThrow(permission);
    assertCode(Status.Code.PERMISSION_DENIED, () -> duplicate(sourceId.toString(), "Copy"));
    verify(server, never()).createInstance(anyString(), same(user), any(InstanceSettingsImpl.Stem.class), anyString());
    assertEquals(1, dsl.fetchCount(Tables.INSTANCES));
  }

  private UUID insert(String name, UUID owner, InstanceSettingsImpl.Stem settings, String icon) {
    var id = UUID.randomUUID();
    var now = LocalDateTime.now(ZoneOffset.UTC);
    dsl.insertInto(Tables.INSTANCES)
      .set(Tables.INSTANCES.ID, id.toString()).set(Tables.INSTANCES.FRIENDLY_NAME, name)
      .set(Tables.INSTANCES.OWNER_ID, owner.toString()).set(Tables.INSTANCES.ICON, icon)
      .set(Tables.INSTANCES.SETTINGS, GsonInstance.CONFIG_GSON.toJson(settings.serializeToTree()))
      .set(Tables.INSTANCES.CREATED_AT, now).set(Tables.INSTANCES.UPDATED_AT, now).set(Tables.INSTANCES.VERSION, 0L).execute();
    return id;
  }

  private InstanceDuplicateResponse duplicate(String id, String name) throws Exception {
    var result = new CompletableFuture<InstanceDuplicateResponse>();
    context.run(() -> service.duplicateInstance(InstanceDuplicateRequest.newBuilder().setId(id).setFriendlyName(name).build(), new StreamObserver<>() {
      @Override
      public void onNext(InstanceDuplicateResponse value) { result.complete(value); }
      @Override
      public void onError(Throwable error) { result.completeExceptionally(error); }
      @Override
      public void onCompleted() { }
    }));
    return result.get();
  }

  private static void assertCode(Status.Code code, ThrowingAction action) {
    assertEquals(code, assertThrows(StatusRuntimeException.class, action::run).getStatus().getCode());
  }

  @FunctionalInterface
  private interface ThrowingAction {
    void run() throws Exception;
  }
}
