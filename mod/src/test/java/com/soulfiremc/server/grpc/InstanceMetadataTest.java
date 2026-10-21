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
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.soulfiremc.grpc.generated.DeleteAccountMetadataEntryRequest;
import com.soulfiremc.grpc.generated.DeleteInstanceMetadataEntryRequest;
import com.soulfiremc.grpc.generated.GetAccountMetadataRequest;
import com.soulfiremc.grpc.generated.GetAccountMetadataResponse;
import com.soulfiremc.grpc.generated.GetInstanceMetadataRequest;
import com.soulfiremc.grpc.generated.GetInstanceMetadataResponse;
import com.soulfiremc.grpc.generated.SetAccountMetadataEntryRequest;
import com.soulfiremc.grpc.generated.SetInstanceMetadataEntryRequest;
import com.soulfiremc.grpc.generated.SettingsNamespace;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.account.AuthType;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.service.OfflineJavaData;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class InstanceMetadataTest {
  private final UUID instanceId = UUID.randomUUID();
  private final UUID accountId = UUID.randomUUID();
  private final SoulFireServer server = mock();
  private final InstanceServiceImpl service = new InstanceServiceImpl(server);
  private final Context context = Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, mock(SoulFireUser.class));

  @BeforeEach
  void setUp(@TempDir Path directory) {
    var source = new SQLiteDataSource();
    source.setUrl("jdbc:sqlite:" + directory.resolve("metadata.sqlite"));
    Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
    var dsl = DSL.using(source, SQLDialect.SQLITE);
    when(server.dsl()).thenReturn(dsl);
    var ownerId = UUID.randomUUID().toString();
    dsl.execute("INSERT INTO users (id, username, email, role, min_issued_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
      ownerId, "owner", "owner@example.com", "ADMIN");
    var account = new MinecraftAccount(AuthType.OFFLINE, accountId, "MetadataBot", new OfflineJavaData(), Map.of(), Map.of());
    var settings = InstanceSettingsImpl.Stem.EMPTY.withAccounts(List.of(account));
    dsl.insertInto(Tables.INSTANCES)
      .set(Tables.INSTANCES.ID, instanceId.toString())
      .set(Tables.INSTANCES.FRIENDLY_NAME, "metadata")
      .set(Tables.INSTANCES.OWNER_ID, ownerId)
      .set(Tables.INSTANCES.ICON, "pickaxe")
      .set(Tables.INSTANCES.SETTINGS, GsonInstance.CONFIG_GSON.toJson(settings.serializeToTree()))
      .execute();
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "{\"value\":null,\"items\":[null,{\"nested\":null}]}"})
  void instanceAndAccountMetadataPreserveNullValues(String json) throws Exception {
    var value = Value.newBuilder();
    JsonFormat.parser().merge(json, value);
    var expected = value.build();
    context.run(() -> service.setInstanceMetadataEntry(SetInstanceMetadataEntryRequest.newBuilder()
      .setInstanceId(instanceId.toString()).setNamespace("_metadata-1.2").setKey("folder/value-1.2_").setValue(expected).build(), mock()));
    context.run(() -> service.setAccountMetadataEntry(SetAccountMetadataEntryRequest.newBuilder()
      .setInstanceId(instanceId.toString()).setAccountId(accountId.toString())
      .setNamespace("_metadata-1.2").setKey("folder/value-1.2_").setValue(expected).build(), mock()));

    var reloadedService = new InstanceServiceImpl(server);
    var instance = context.call(() -> InstanceMetadataTest.<GetInstanceMetadataResponse>call(observer ->
      reloadedService.getInstanceMetadata(GetInstanceMetadataRequest.newBuilder()
        .setInstanceId(instanceId.toString()).build(), observer)));
    var account = context.call(() -> InstanceMetadataTest.<GetAccountMetadataResponse>call(observer ->
      reloadedService.getAccountMetadata(GetAccountMetadataRequest.newBuilder()
        .setInstanceId(instanceId.toString()).setAccountId(accountId.toString()).build(), observer)));

    assertEquals(expected, value(instance.getMetadataList()));
    assertEquals(expected, value(account.getMetadataList()));
  }

  @Test
  void metadataRpcUpdatesLeaveOtherLiveChangesPending() {
    var instance = mock(InstanceManager.class);
    var bot = mock(BotConnection.class);
    var instanceMetadata = new MetadataHolder<JsonElement>();
    var botMetadata = new MetadataHolder<JsonElement>();
    instanceMetadata.set("test", "pending", new JsonPrimitive(7));
    botMetadata.set("test", "pending", new JsonPrimitive(7));
    when(server.getInstance(instanceId)).thenReturn(Optional.of(instance));
    when(instance.persistentMetadata()).thenReturn(instanceMetadata);
    when(instance.botConnections()).thenReturn(Map.of(accountId, bot));
    when(bot.persistentMetadata()).thenReturn(botMetadata);
    var value = Value.newBuilder().setNumberValue(42).build();

    context.run(() -> service.setInstanceMetadataEntry(SetInstanceMetadataEntryRequest.newBuilder()
      .setInstanceId(instanceId.toString()).setNamespace("test").setKey("saved").setValue(value).build(), mock()));
    context.run(() -> service.setAccountMetadataEntry(SetAccountMetadataEntryRequest.newBuilder()
      .setInstanceId(instanceId.toString()).setAccountId(accountId.toString())
      .setNamespace("test").setKey("saved").setValue(value).build(), mock()));
    assertEquals(new JsonPrimitive(7), instanceMetadata.exportIfDirty().orElseThrow().get("test").get("pending"));
    assertEquals(new JsonPrimitive(7), botMetadata.exportIfDirty().orElseThrow().get("test").get("pending"));

    context.run(() -> service.deleteInstanceMetadataEntry(DeleteInstanceMetadataEntryRequest.newBuilder()
      .setInstanceId(instanceId.toString()).setNamespace("test").setKey("saved").build(), mock()));
    context.run(() -> service.deleteAccountMetadataEntry(DeleteAccountMetadataEntryRequest.newBuilder()
      .setInstanceId(instanceId.toString()).setAccountId(accountId.toString())
      .setNamespace("test").setKey("saved").build(), mock()));
    assertTrue(instanceMetadata.exportIfDirty().isPresent());
    assertTrue(botMetadata.exportIfDirty().isPresent());
  }

  @ParameterizedTest
  @CsvSource({"'',value", "metadata,''", "Invalid,value", "invalid/name,value", "invalid:name,value",
    "metadata,Invalid", "metadata,invalid:key", "metadata,invalid key", "méta,value"})
  void invalidIdentifiersNeverChangeStoredMetadata(String namespace, String key) {
    var before = server.dsl().selectFrom(Tables.INSTANCES).fetchSingle().intoMap();
    var value = Value.newBuilder().setNumberValue(42).build();

    assertAll(
      () -> assertStatus(Status.Code.INVALID_ARGUMENT, () -> service.setInstanceMetadataEntry(SetInstanceMetadataEntryRequest.newBuilder()
        .setInstanceId(instanceId.toString()).setNamespace(namespace).setKey(key).setValue(value).build(), mock())),
      () -> assertStatus(Status.Code.INVALID_ARGUMENT, () -> service.setAccountMetadataEntry(SetAccountMetadataEntryRequest.newBuilder()
        .setInstanceId(instanceId.toString()).setAccountId(accountId.toString())
        .setNamespace(namespace).setKey(key).setValue(value).build(), mock())),
      () -> assertStatus(Status.Code.INVALID_ARGUMENT, () -> service.deleteInstanceMetadataEntry(DeleteInstanceMetadataEntryRequest.newBuilder()
        .setInstanceId(instanceId.toString()).setNamespace(namespace).setKey(key).build(), mock())),
      () -> assertStatus(Status.Code.INVALID_ARGUMENT, () -> service.deleteAccountMetadataEntry(DeleteAccountMetadataEntryRequest.newBuilder()
        .setInstanceId(instanceId.toString()).setAccountId(accountId.toString())
        .setNamespace(namespace).setKey(key).build(), mock())),
      () -> assertEquals(before, server.dsl().selectFrom(Tables.INSTANCES).fetchSingle().intoMap()));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void missingAccountMutationsFailWithoutUpdatingTheInstance(boolean delete) {
    var missingAccountId = UUID.randomUUID().toString();
    var before = server.dsl().selectFrom(Tables.INSTANCES).fetchSingle().intoMap();

    assertStatus(Status.Code.NOT_FOUND, () -> {
      if (delete) {
        service.deleteAccountMetadataEntry(DeleteAccountMetadataEntryRequest.newBuilder()
          .setInstanceId(instanceId.toString()).setAccountId(missingAccountId)
          .setNamespace("metadata").setKey("value").build(), mock());
      } else {
        service.setAccountMetadataEntry(SetAccountMetadataEntryRequest.newBuilder()
          .setInstanceId(instanceId.toString()).setAccountId(missingAccountId)
          .setNamespace("metadata").setKey("value").setValue(Value.newBuilder().setNumberValue(42)).build(), mock());
      }
    });
    assertEquals(before, server.dsl().selectFrom(Tables.INSTANCES).fetchSingle().intoMap());
  }

  private void assertStatus(Status.Code expected, Runnable action) {
    var error = assertThrows(StatusRuntimeException.class, () -> context.run(action));
    assertEquals(expected, error.getStatus().getCode());
  }

  private static Value value(List<SettingsNamespace> metadata) {
    return metadata.stream().filter(namespace -> namespace.getNamespace().equals("_metadata-1.2"))
      .flatMap(namespace -> namespace.getEntriesList().stream()).filter(entry -> entry.getKey().equals("folder/value-1.2_"))
      .findFirst().orElseThrow().getValue();
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
