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

import com.soulfiremc.grpc.generated.InstanceInfoRequest;
import com.soulfiremc.grpc.generated.InstanceRemoveProxyRequest;
import com.soulfiremc.grpc.generated.InstanceUpdateConfigEntryRequest;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.generated.Tables;
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

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class InstanceServiceErrorTest {
  private final SoulFireServer server = mock();
  private final InstanceServiceImpl service = new InstanceServiceImpl(server);
  private final Context context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, mock(SoulFireUser.class));

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var dataSource = new SQLiteDataSource();
    dataSource.setUrl("jdbc:sqlite:" + directory.resolve("instances.sqlite"));
    Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    when(server.dsl()).thenReturn(DSL.using(dataSource, SQLDialect.SQLITE));
  }

  @Test
  void missingInstancePreservesNotFound() {
    var request = InstanceInfoRequest.newBuilder().setId(UUID.randomUUID().toString()).build();
    var error = assertThrows(StatusRuntimeException.class,
      () -> context.run(() -> service.getInstanceInfo(request, mock())));
    assertEquals(Status.Code.NOT_FOUND, error.getStatus().getCode());
  }

  @Test
  void missingInstanceInTransactionPreservesNotFound() {
    var request = InstanceUpdateConfigEntryRequest.newBuilder().setId(UUID.randomUUID().toString()).build();
    var error = assertThrows(StatusRuntimeException.class,
      () -> context.run(() -> service.updateInstanceConfigEntry(request, mock())));
    assertEquals(Status.Code.NOT_FOUND, error.getStatus().getCode());
  }

  @Test
  void invalidProxyIndexPreservesInvalidArgument() {
    var ownerId = UUID.randomUUID().toString();
    var instanceId = UUID.randomUUID().toString();
    server.dsl().execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      ownerId, "owner", "owner@example.com", "ADMIN");
    server.dsl().insertInto(Tables.INSTANCES)
      .set(Tables.INSTANCES.ID, instanceId)
      .set(Tables.INSTANCES.FRIENDLY_NAME, "farm")
      .set(Tables.INSTANCES.OWNER_ID, ownerId)
      .set(Tables.INSTANCES.ICON, "pickaxe")
      .set(Tables.INSTANCES.SETTINGS, GsonInstance.CONFIG_GSON.toJson(InstanceSettingsImpl.Stem.EMPTY.serializeToTree()))
      .execute();
    var request = InstanceRemoveProxyRequest.newBuilder().setId(instanceId).setIndex(0).build();
    var error = assertThrows(StatusRuntimeException.class,
      () -> context.run(() -> service.removeInstanceProxy(request, mock())));
    assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
  }

  @Test
  void unexpectedFailureRemainsInternal() {
    when(server.dsl()).thenThrow(new IllegalStateException());
    var request = InstanceInfoRequest.newBuilder().setId(UUID.randomUUID().toString()).build();
    var error = assertThrows(StatusRuntimeException.class,
      () -> context.run(() -> service.getInstanceInfo(request, mock())));
    assertEquals(Status.Code.INTERNAL, error.getStatus().getCode());
  }
}
