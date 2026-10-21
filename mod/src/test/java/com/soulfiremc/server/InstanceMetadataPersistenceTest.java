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
package com.soulfiremc.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.soulfiremc.server.account.AuthType;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.service.OfflineJavaData;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.util.structs.GsonInstance;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.TransactionListener;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class InstanceMetadataPersistenceTest {
  private final UUID instanceId = UUID.randomUUID();
  private final UUID botId = UUID.randomUUID();
  private final MetadataHolder<JsonElement> instanceMetadata = new MetadataHolder<>();
  private final MetadataHolder<JsonElement> botMetadata = new MetadataHolder<>();
  private final BotConnection bot = mock();
  private DSLContext dsl;

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var source = new SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + directory.resolve("metadata.sqlite"));
    Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    dsl = DSL.using(source, SQLDialect.SQLITE);
    var ownerId = UUID.randomUUID().toString();
    dsl.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      ownerId, "owner", "owner@example.com", "ADMIN");
    var account = new MinecraftAccount(AuthType.OFFLINE, botId, "MetadataBot", new OfflineJavaData(), Map.of(), Map.of());
    var settings = InstanceSettingsImpl.Stem.EMPTY.withAccounts(List.of(account));
    dsl.insertInto(Tables.INSTANCES)
      .set(Tables.INSTANCES.ID, instanceId.toString())
      .set(Tables.INSTANCES.FRIENDLY_NAME, "metadata")
      .set(Tables.INSTANCES.OWNER_ID, ownerId)
      .set(Tables.INSTANCES.ICON, "pickaxe")
      .set(Tables.INSTANCES.SETTINGS, GsonInstance.CONFIG_GSON.toJson(settings.serializeToTree()))
      .execute();
    when(bot.persistentMetadata()).thenReturn(botMetadata);
    instanceMetadata.set("test", "value", new JsonPrimitive(1));
    botMetadata.set("test", "value", new JsonPrimitive(1));
  }

  @Test
  void aFailedTransactionKeepsBothSnapshotsAvailableForRetry() {
    var unavailable = DSL.using(SQLDialect.SQLITE);
    assertThrows(DataAccessException.class,
      () -> InstanceMetadataPersistence.persist(unavailable, instanceId, instanceMetadata, Map.of(botId, bot)));
    assertTrue(instanceMetadata.exportIfDirty().isPresent());
    assertTrue(botMetadata.exportIfDirty().isPresent());

    InstanceMetadataPersistence.persist(dsl, instanceId, instanceMetadata, Map.of(botId, bot));

    assertEquals(new JsonPrimitive(1), stored().persistentMetadata().get("test").get("value"));
    assertEquals(new JsonPrimitive(1), stored().accounts().getFirst().persistentMetadata().get("test").get("value"));
    assertTrue(instanceMetadata.exportIfDirty().isEmpty());
    assertTrue(botMetadata.exportIfDirty().isEmpty());
  }

  @Test
  void changesDuringCommitRemainDirtyUntilTheirOwnSnapshotIsSaved() {
    var listener = TransactionListener.onCommitEnd(_ -> {
      instanceMetadata.set("test", "value", new JsonPrimitive(2));
      botMetadata.remove("test", "value");
    });
    var racing = DSL.using(dsl.configuration().derive(listener));

    InstanceMetadataPersistence.persist(racing, instanceId, instanceMetadata, Map.of(botId, bot));

    assertEquals(new JsonPrimitive(1), stored().persistentMetadata().get("test").get("value"));
    assertTrue(instanceMetadata.exportIfDirty().isPresent());
    assertTrue(botMetadata.exportIfDirty().isPresent());
    InstanceMetadataPersistence.persist(dsl, instanceId, instanceMetadata, Map.of(botId, bot));
    assertEquals(new JsonPrimitive(2), stored().persistentMetadata().get("test").get("value"));
    assertTrue(stored().accounts().getFirst().persistentMetadata().isEmpty());
    assertTrue(instanceMetadata.exportIfDirty().isEmpty());
    assertTrue(botMetadata.exportIfDirty().isEmpty());
  }

  @Test
  void aMissingInstanceDoesNotAcknowledgeUnwrittenValues() {
    InstanceMetadataPersistence.persist(dsl, UUID.randomUUID(), instanceMetadata, Map.of(botId, bot));
    assertTrue(instanceMetadata.exportIfDirty().isPresent());
    assertTrue(botMetadata.exportIfDirty().isPresent());
  }

  private InstanceSettingsImpl.Stem stored() {
    var json = dsl.select(Tables.INSTANCES.SETTINGS).from(Tables.INSTANCES)
      .where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchSingle().value1();
    return InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(json, JsonElement.class));
  }
}
