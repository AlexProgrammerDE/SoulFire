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
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.util.structs.GsonInstance;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class InstanceMetadataPersistence {
  private InstanceMetadataPersistence() {
  }

  static void persist(DSLContext dsl, UUID instanceId, MetadataHolder<JsonElement> instanceMetadata,
                      Map<UUID, BotConnection> bots) {
    var dirtyBots = new LinkedHashMap<UUID, PendingMetadata>();
    for (var entry : bots.entrySet()) {
      var holder = entry.getValue().persistentMetadata();
      holder.snapshotIfDirty().ifPresent(snapshot -> dirtyBots.put(entry.getKey(), new PendingMetadata(holder, snapshot)));
    }
    var dirtyInstanceMetadata = instanceMetadata.snapshotIfDirty();
    if (dirtyBots.isEmpty() && dirtyInstanceMetadata.isEmpty()) {
      return;
    }

    var persisted = dsl.transactionResult(cfg -> {
      var ctx = DSL.using(cfg);
      var record = ctx.selectFrom(Tables.INSTANCES).where(Tables.INSTANCES.ID.eq(instanceId.toString())).fetchOne();
      if (record == null) {
        return Optional.<List<PendingMetadata>>empty();
      }
      var savedBots = new ArrayList<PendingMetadata>();
      var currentSettings = InstanceSettingsImpl.Stem.deserialize(GsonInstance.GSON.fromJson(record.getSettings(), JsonElement.class));
      if (!dirtyBots.isEmpty()) {
        var newAccounts = currentSettings.accounts().stream().map(account -> {
          var update = dirtyBots.get(account.profileId());
          if (update == null) {
            return account;
          }
          savedBots.add(update);
          return account.withPersistentMetadata(update.snapshot().values());
        }).toList();
        currentSettings = currentSettings.withAccounts(newAccounts);
      }
      if (dirtyInstanceMetadata.isPresent()) {
        currentSettings = currentSettings.withPersistentMetadata(dirtyInstanceMetadata.get().values());
      }
      ctx.update(Tables.INSTANCES)
        .set(Tables.INSTANCES.SETTINGS, GsonInstance.CONFIG_GSON.toJson(currentSettings.serializeToTree()))
        .set(Tables.INSTANCES.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
        .where(Tables.INSTANCES.ID.eq(instanceId.toString()))
        .execute();
      return Optional.of(List.copyOf(savedBots));
    });

    persisted.ifPresent(savedBots -> {
      savedBots.forEach(update -> update.holder().markClean(update.snapshot()));
      dirtyInstanceMetadata.ifPresent(instanceMetadata::markClean);
    });
  }

  private record PendingMetadata(MetadataHolder<JsonElement> holder, MetadataHolder.Snapshot<JsonElement> snapshot) {}
}
