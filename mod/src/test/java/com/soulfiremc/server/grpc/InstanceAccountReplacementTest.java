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
import com.soulfiremc.grpc.generated.InstanceReplaceAccountsRequest;
import com.soulfiremc.grpc.generated.MinecraftAccountProto;
import com.soulfiremc.server.BotStateManager;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.account.AuthType;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.service.OfflineJavaData;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.proxy.ProxyType;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

final class InstanceAccountReplacementTest {
  private final SoulFireServer server = mock();
  private final InstanceManager instance = mock();
  private final BotStateManager bots = mock();
  private final InstanceServiceImpl service = new InstanceServiceImpl(server);
  private final Context context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, mock(SoulFireUser.class));
  private final UUID instanceId = UUID.randomUUID();
  private final MinecraftAccount first = account("First");
  private final MinecraftAccount second = account("Second");
  private final InstanceSettingsImpl.Stem original = new InstanceSettingsImpl.Stem(
    Map.of("plugin", Map.of("enabled", new JsonPrimitive(true))),
    List.of(first, second),
    List.of(new SFProxy(ProxyType.HTTP, new InetSocketAddress("127.0.0.1", 19180), null, null)),
    Map.of("marker", Map.of("value", new JsonPrimitive(4))));

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var dataSource = new SQLiteDataSource();
    dataSource.setUrl("jdbc:sqlite:" + directory.resolve("instances.sqlite"));
    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    when(server.dsl()).thenReturn(DSL.using(dataSource, SQLDialect.SQLITE));
    when(server.getInstance(instanceId)).thenReturn(Optional.of(instance));
    when(instance.botStateManager()).thenReturn(bots);
    when(bots.removeAccounts(anyCollection())).thenReturn(CompletableFuture.completedFuture(null));
    var owner = UUID.randomUUID().toString();
    server.dsl().execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      owner, "owner", "owner@example.com", "ADMIN");
    server.dsl().insertInto(Tables.INSTANCES)
      .set(Tables.INSTANCES.ID, instanceId.toString())
      .set(Tables.INSTANCES.FRIENDLY_NAME, "accounts")
      .set(Tables.INSTANCES.OWNER_ID, owner)
      .set(Tables.INSTANCES.ICON, "pickaxe")
      .set(Tables.INSTANCES.SETTINGS, GsonInstance.CONFIG_GSON.toJson(original.serializeToTree()))
      .execute();
  }

  @Test
  void replacementCommitsBeforeStoppingOnlyRemovedBotsAndPreservesOtherConfiguration() {
    var added = account("Third");
    var updated = first.withLastKnownName("Renamed");
    var expected = original.withAccounts(List.of(updated, added));
    doAnswer(_ -> {
      assertEquals(expected, stored());
      return CompletableFuture.completedFuture(null);
    }).when(bots).removeAccounts(List.of(second.profileId()));

    replace(List.of(first.toProto(), added.toProto(), updated.toProto()));

    assertEquals(expected, stored());
    verify(bots).removeAccounts(List.of(second.profileId()));
    verify(bots).syncConfiguredAccounts();
    verify(instance).invalidateSettingsCache();
  }

  @Test
  void emptyReplacementRemovesAllAccounts() {
    replace(List.of());

    assertEquals(original.withAccounts(List.of()), stored());
    verify(bots).removeAccounts(List.of(first.profileId(), second.profileId()));
  }

  @Test
  void invalidReplacementDoesNotChangeAccountsOrStopBots() {
    var invalid = second.toProto().toBuilder().clearOfflineJavaData().build();

    var error = assertThrows(StatusRuntimeException.class, () -> replace(List.of(first.toProto(), invalid)));

    assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
    assertEquals(original, stored());
    verifyNoInteractions(bots);
  }

  @Test
  void failedDatabaseWritePreservesAccountsAndLeavesBotsRunning() {
    server.dsl().execute("CREATE TRIGGER reject_replacement BEFORE UPDATE ON instances BEGIN SELECT RAISE(ABORT, 'replacement rejected'); END");

    var error = assertThrows(StatusRuntimeException.class, () -> replace(List.of(account("Third").toProto())));

    assertEquals(Status.Code.INTERNAL, error.getStatus().getCode());
    assertEquals(original, stored());
    verifyNoInteractions(bots);
  }

  private void replace(List<MinecraftAccountProto> accounts) {
    var request = InstanceReplaceAccountsRequest.newBuilder().setId(instanceId.toString()).addAllAccounts(accounts).build();
    context.run(() -> service.replaceInstanceAccounts(request, mock()));
  }

  private InstanceSettingsImpl.Stem stored() {
    var data = server.dsl().select(Tables.INSTANCES.SETTINGS).from(Tables.INSTANCES)
      .where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchSingle(Tables.INSTANCES.SETTINGS);
    return InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(data, JsonElement.class));
  }

  private static MinecraftAccount account(String name) {
    return new MinecraftAccount(AuthType.OFFLINE, OfflineJavaData.getOfflineUUID(name), name, new OfflineJavaData(), Map.of(), Map.of());
  }
}
