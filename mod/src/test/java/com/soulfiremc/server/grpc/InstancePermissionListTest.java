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

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.soulfiremc.grpc.generated.BotFleetSummary;
import com.soulfiremc.grpc.generated.InstanceListRequest;
import com.soulfiremc.grpc.generated.InstanceListResponse;
import com.soulfiremc.grpc.generated.InstancePermission;
import com.soulfiremc.server.BotStateManager;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.settings.lib.ServerSettingsDelegate;
import com.soulfiremc.server.settings.lib.ServerSettingsImpl;
import com.soulfiremc.server.settings.lib.ServerSettingsSource;
import com.soulfiremc.server.settings.server.ServerSettings;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Jwts;
import org.flywaydb.core.Flyway;
import org.jooq.ExecuteListener;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class InstancePermissionListTest {
  private final SoulFireServer server = mock();
  private final UUID ownerId = UUID.randomUUID();
  private final List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
  private final AtomicInteger queries = new AtomicInteger();
  private final AtomicReference<ServerSettingsSource> source = new AtomicReference<>(new ServerSettingsImpl(ServerSettingsImpl.Stem.EMPTY));
  private final ServerSettingsDelegate settings = new ServerSettingsDelegate(source::get);
  private AuthSystem auth;
  private SoulFireUser owner;

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var database = new SQLiteDataSource();
    database.setUrl("jdbc:sqlite:" + directory.resolve("permissions.sqlite"));
    Flyway.configure().dataSource(database).locations("classpath:db/migration").load().migrate();
    var dsl = DSL.using(database, SQLDialect.SQLITE);
    dsl.configuration().set(ExecuteListener.onExecuteStart(_ -> queries.incrementAndGet()));
    when(server.dsl()).thenReturn(dsl);
    when(server.settingsSource()).thenReturn(settings);
    when(server.jwtSecretKey()).thenReturn(Jwts.SIG.HS256.key().build());
    auth = new AuthSystem(server, dsl);
    dsl.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      ownerId.toString(), "owner", "owner@example.com", "USER");
    for (var index = 0; index < ids.size(); index++) {
      var id = ids.get(index);
      dsl.insertInto(Tables.INSTANCES).set(Tables.INSTANCES.ID, id.toString())
        .set(Tables.INSTANCES.OWNER_ID, (index < 2 ? ownerId : AuthSystem.ROOT_USER_ID).toString())
        .set(Tables.INSTANCES.FRIENDLY_NAME, id.toString().substring(0, 8)).set(Tables.INSTANCES.ICON, "pickaxe")
        .set(Tables.INSTANCES.SETTINGS, GsonInstance.CONFIG_GSON.toJson(InstanceSettingsImpl.Stem.EMPTY.serializeToTree())).execute();
      var runtime = mock(InstanceManager.class);
      var bots = mock(BotStateManager.class);
      when(server.getInstance(id)).thenReturn(Optional.of(runtime));
      when(runtime.botStateManager()).thenReturn(bots);
      when(bots.summary()).thenReturn(BotFleetSummary.getDefaultInstance());
    }
    owner = auth.authenticateBySubject(ownerId, Instant.now()).orElseThrow();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void listsVisibleProfilesWithoutQueryingOwnershipForEveryPermission(boolean admin) {
    var user = admin ? auth.authenticateBySubject(AuthSystem.ROOT_USER_ID, Instant.now()).orElseThrow() : owner;
    queries.set(0);
    var response = list(user);
    assertEquals(admin ? 1 : ids.size() + 1, queries.get());
    assertEquals(admin ? Set.copyOf(ids) : Set.copyOf(ids.subList(0, 2)), response.getInstancesList().stream()
      .map(instance -> UUID.fromString(instance.getId())).collect(Collectors.toSet()));
    for (var instance : response.getInstancesList()) {
      var id = UUID.fromString(instance.getId());
      assertEquals(InstancePermission.values().length - 1, instance.getInstancePermissionsCount());
      if (admin) assertTrue(instance.getInstancePermissionsList().stream().allMatch(permission -> permission.getGranted()));
      for (var permission : instance.getInstancePermissionsList()) {
        assertEquals(user.hasPermission(PermissionContext.instance(permission.getInstancePermission(), id)), permission.getGranted());
      }
    }
  }

  @Test
  void customPermissionOverridesStillControlVisibilityAndActions() {
    var user = mock(SoulFireUser.class, CALLS_REAL_METHODS);
    doAnswer(call -> {
      PermissionContext.InstanceContext permission = call.getArgument(0);
      return permission.instanceId().equals(ids.getFirst())
        && (permission.instancePermission() == InstancePermission.READ_INSTANCE
          || permission.instancePermission() == InstancePermission.CONTROL_BOTS);
    }).when(user).hasPermission(any());
    var response = list(user);
    assertEquals(List.of(ids.getFirst().toString()), response.getInstancesList().stream().map(InstanceListResponse.Instance::getId).toList());
    var grants = response.getInstances(0).getInstancePermissionsList().stream().collect(Collectors.toMap(
      permission -> permission.getInstancePermission(), permission -> permission.getGranted()));
    assertTrue(grants.get(InstancePermission.CONTROL_BOTS));
    assertFalse(grants.get(InstancePermission.DELETE_INSTANCE));
    for (var id : ids.subList(1, ids.size())) {
      verify(user, times(1)).hasPermission(argThat(permission -> permission instanceof PermissionContext.InstanceContext instance
        && instance.instanceId().equals(id)));
    }
  }

  @Test
  void laterRequestsObserveOwnershipAndPolicyChanges() {
    assertEquals(2, list(owner).getInstancesCount());
    server.dsl().update(Tables.INSTANCES).set(Tables.INSTANCES.OWNER_ID, AuthSystem.ROOT_USER_ID.toString())
      .where(Tables.INSTANCES.ID.eq(ids.getFirst().toString())).execute();
    policy(false);
    var restricted = list(owner).getInstancesList();
    assertEquals(List.of(ids.get(1).toString()), restricted.stream().map(InstanceListResponse.Instance::getId).toList());
    var grants = restricted.getFirst().getInstancePermissionsList().stream().collect(Collectors.toMap(
      permission -> permission.getInstancePermission(), permission -> permission.getGranted()));
    assertFalse(grants.get(InstancePermission.DELETE_INSTANCE));
    assertFalse(grants.get(InstancePermission.UPDATE_INSTANCE_META));
    assertFalse(grants.get(InstancePermission.RAW_PROTOCOL));
    assertTrue(grants.get(InstancePermission.READ_INSTANCE));
    assertTrue(grants.get(InstancePermission.CONTROL_BOTS));
    server.dsl().update(Tables.INSTANCES).set(Tables.INSTANCES.OWNER_ID, ownerId.toString())
      .where(Tables.INSTANCES.ID.eq(ids.getFirst().toString())).execute();
    policy(true);
    var restored = list(owner);
    assertEquals(2, restored.getInstancesCount());
    assertTrue(restored.getInstancesList().stream().flatMap(instance -> instance.getInstancePermissionsList().stream())
      .filter(permission -> permission.getInstancePermission() == InstancePermission.DELETE_INSTANCE
        || permission.getInstancePermission() == InstancePermission.UPDATE_INSTANCE_META)
      .allMatch(permission -> permission.getGranted()));
  }

  private void policy(boolean allowed) {
    source.set(new ServerSettingsImpl(new ServerSettingsImpl.Stem(Map.of(ServerSettings.ALLOW_DELETING_INSTANCES.namespace(),
      Map.<String, JsonElement>of(ServerSettings.ALLOW_DELETING_INSTANCES.key(), new JsonPrimitive(allowed),
        ServerSettings.ALLOW_CHANGING_INSTANCE_META.key(), new JsonPrimitive(allowed))))));
    settings.invalidate();
  }

  private InstanceListResponse list(SoulFireUser user) {
    StreamObserver<InstanceListResponse> observer = mock();
    Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, user).run(() ->
      new InstanceServiceImpl(server).listInstances(InstanceListRequest.getDefaultInstance(), observer));
    var response = ArgumentCaptor.forClass(InstanceListResponse.class);
    verify(observer).onNext(response.capture());
    verify(observer).onCompleted();
    return response.getValue();
  }
}
