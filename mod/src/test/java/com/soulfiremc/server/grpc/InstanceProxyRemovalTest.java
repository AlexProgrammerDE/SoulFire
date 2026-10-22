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
import com.soulfiremc.grpc.generated.InstanceRemoveProxiesBatchRequest;
import com.soulfiremc.grpc.generated.InstanceRemoveProxyEntriesRequest;
import com.soulfiremc.grpc.generated.ProxyProto;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.proxy.ProxyType;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.SocketAddressHelper;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class InstanceProxyRemovalTest {
  private final SoulFireServer server = mock();
  private final InstanceServiceImpl service = new InstanceServiceImpl(server);
  private final Context context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, mock(SoulFireUser.class));
  private final String instanceId = UUID.randomUUID().toString();
  private final SFProxy first = proxy(ProxyType.HTTP, 19180, "first");
  private final SFProxy second = proxy(ProxyType.HTTP, 19180, "second");
  private final SFProxy otherProtocol = proxy(ProxyType.SOCKS5, 19180, "first");
  private final SFProxy otherAddress = proxy(ProxyType.HTTP, 19181, "first");

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var dataSource = new SQLiteDataSource();
    dataSource.setUrl("jdbc:sqlite:" + directory.resolve("instances.sqlite"));
    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    when(server.dsl()).thenReturn(DSL.using(dataSource, SQLDialect.SQLITE));
    var owner = UUID.randomUUID().toString();
    server.dsl().execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      owner, "owner", "owner@example.com", "ADMIN");
    server.dsl().insertInto(Tables.INSTANCES)
      .set(Tables.INSTANCES.ID, instanceId)
      .set(Tables.INSTANCES.FRIENDLY_NAME, "proxies")
      .set(Tables.INSTANCES.OWNER_ID, owner)
      .set(Tables.INSTANCES.ICON, "pickaxe")
      .set(Tables.INSTANCES.SETTINGS, serialize(List.of()))
      .execute();
  }

  @Test
  void exactRemovalPreservesOtherCredentialsAndProtocols() {
    store(List.of(first, second, otherProtocol, otherAddress));

    remove(InstanceRemoveProxyEntriesRequest.newBuilder().addProxies(first.toProto()));

    assertEquals(List.of(second, otherProtocol, otherAddress), stored());
  }

  @Test
  void eachSelectorRemovesOneOccurrence() {
    store(List.of(first, second, first, first));

    remove(InstanceRemoveProxyEntriesRequest.newBuilder().addProxies(first.toProto()).addProxies(first.toProto()));

    assertEquals(List.of(second, first), stored());
  }

  @Test
  void addressRemovalStillRemovesEveryMatchingEntry() {
    store(List.of(first, second, otherProtocol, otherAddress));

    var request = InstanceRemoveProxiesBatchRequest.newBuilder().setId(instanceId)
      .addAddresses(SocketAddressHelper.serialize(first.address())).build();
    context.run(() -> service.removeInstanceProxiesBatch(request, mock()));

    assertEquals(List.of(otherAddress), stored());
  }

  @Test
  void unmatchedSelectorsLeaveTheOtherEntriesIntact() {
    store(List.of(first, second, otherProtocol, otherAddress));

    remove(InstanceRemoveProxyEntriesRequest.newBuilder()
      .addProxies(proxy(ProxyType.HTTP, 19180, "missing").toProto())
      .addProxies(otherAddress.toProto()));

    assertEquals(List.of(first, second, otherProtocol), stored());
  }

  @Test
  void invalidSelectorsLeaveTheStoredProxiesIntact() {
    store(List.of(first, second));
    var invalid = first.toProto().toBuilder().setType(ProxyProto.Type.SOCKS4).build();

    var error = assertThrows(StatusRuntimeException.class,
      () -> remove(InstanceRemoveProxyEntriesRequest.newBuilder().addProxies(invalid)));

    assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
    assertEquals(List.of(first, second), stored());
  }

  private void remove(InstanceRemoveProxyEntriesRequest.Builder request) {
    context.run(() -> service.removeInstanceProxyEntries(request.setId(instanceId).build(), mock()));
  }

  private void store(List<SFProxy> proxies) {
    server.dsl().update(Tables.INSTANCES)
      .set(Tables.INSTANCES.SETTINGS, serialize(proxies))
      .where(Tables.INSTANCES.ID.eq(instanceId)).execute();
  }

  private List<SFProxy> stored() {
    var data = server.dsl().select(Tables.INSTANCES.SETTINGS).from(Tables.INSTANCES)
      .where(Tables.INSTANCES.ID.eq(instanceId)).fetchSingle(Tables.INSTANCES.SETTINGS);
    return InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(data, JsonElement.class)).proxies();
  }

  private static String serialize(List<SFProxy> proxies) {
    return GsonInstance.CONFIG_GSON.toJson(InstanceSettingsImpl.Stem.EMPTY.withProxies(proxies).serializeToTree());
  }

  private static SFProxy proxy(ProxyType type, int port, String username) {
    return new SFProxy(type, new InetSocketAddress("127.0.0.1", port), username, "password");
  }
}
