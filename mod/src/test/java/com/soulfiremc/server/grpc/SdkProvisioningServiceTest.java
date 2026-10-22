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
import com.soulfiremc.grpc.generated.BotAuthentication;
import com.soulfiremc.grpc.generated.InstanceGetOrCreateBotRequest;
import com.soulfiremc.grpc.generated.InstanceGetOrCreateBotResponse;
import com.soulfiremc.grpc.generated.InstancePermission;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.settings.lib.ServerSettingsDelegate;
import com.soulfiremc.server.settings.lib.ServerSettingsImpl;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.structs.GsonInstance;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Jwts;
import net.kyori.adventure.util.TriState;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class SdkProvisioningServiceTest {
  private final SoulFireServer server = mock();
  private final InstanceServiceImpl service = new InstanceServiceImpl(server);
  private final UUID instanceId = UUID.randomUUID();
  private HikariDataSource pool;
  private SoulFireUser owner;
  private AuthSystem auth;

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var source = new SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + directory.resolve("provisioning.sqlite"));
    Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    var config = new HikariConfig();
    config.setDataSource(source);
    config.setMaximumPoolSize(1);
    config.setConnectionTimeout(250);
    pool = new HikariDataSource(config);
    var dsl = DSL.using(pool, SQLDialect.SQLITE);
    when(server.dsl()).thenReturn(dsl);
    when(server.settingsSource()).thenReturn(new ServerSettingsDelegate(() -> new ServerSettingsImpl(ServerSettingsImpl.Stem.EMPTY)));
    when(server.jwtSecretKey()).thenReturn(Jwts.SIG.HS256.key().build());
    when(server.getInstance(instanceId)).thenReturn(Optional.empty());
    auth = new AuthSystem(server, dsl);
    var ownerId = UUID.randomUUID();
    dsl.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      ownerId.toString(), "owner", "owner@example.com", "USER");
    dsl.insertInto(Tables.INSTANCES)
      .set(Tables.INSTANCES.ID, instanceId.toString())
      .set(Tables.INSTANCES.OWNER_ID, ownerId.toString())
      .set(Tables.INSTANCES.FRIENDLY_NAME, "Provisioning")
      .set(Tables.INSTANCES.ICON, "pickaxe")
      .set(Tables.INSTANCES.SETTINGS, GsonInstance.CONFIG_GSON.toJson(InstanceSettingsImpl.Stem.EMPTY.serializeToTree()))
      .execute();
    owner = auth.authenticateBySubject(ownerId, Instant.now()).orElseThrow();
  }

  @AfterEach
  void tearDown() {
    if (pool != null) pool.close();
  }

  @Test
  void ownerCanProvisionAndReuseABotWithOneDatabaseConnection() {
    var created = provision(owner, "Builder");
    var reused = provision(owner, "Builder");
    assertTrue(created.getCreated());
    assertFalse(reused.getCreated());
    assertEquals(created.getBotId(), reused.getBotId());
    assertEquals(0, pool.getHikariPoolMXBean().getActiveConnections());
  }

  @Test
  void readOnlyAccessAllowsReuseButCannotAddAnAccount() {
    var root = auth.authenticateBySubject(AuthSystem.ROOT_USER_ID, Instant.now()).orElseThrow();
    var created = provision(root, "Builder");
    var readOnly = spy(owner);
    doReturn(TriState.FALSE).when(readOnly).getPermission(
      PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));
    var reused = provision(readOnly, "Builder");
    assertFalse(reused.getCreated());
    assertEquals(created.getBotId(), reused.getBotId());
    assertEquals(Status.Code.PERMISSION_DENIED, assertThrows(StatusRuntimeException.class,
      () -> provision(readOnly, "OtherBot")).getStatus().getCode());
    var stored = server.dsl().selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchSingle();
    var settings = InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(stored.getSettings(), JsonElement.class));
    assertEquals(1, settings.accounts().size());
  }

  private InstanceGetOrCreateBotResponse provision(SoulFireUser user, String name) {
    StreamObserver<InstanceGetOrCreateBotResponse> observer = mock();
    Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, user).run(() ->
      service.getOrCreateBot(InstanceGetOrCreateBotRequest.newBuilder()
        .setId(instanceId.toString()).setName(name).setAuth(BotAuthentication.BOT_AUTHENTICATION_OFFLINE).build(), observer));
    var result = ArgumentCaptor.forClass(InstanceGetOrCreateBotResponse.class);
    verify(observer).onNext(result.capture());
    verify(observer).onCompleted();
    return result.getValue();
  }
}
