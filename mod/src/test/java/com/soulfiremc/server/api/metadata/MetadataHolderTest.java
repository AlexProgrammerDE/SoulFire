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
package com.soulfiremc.server.api.metadata;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class MetadataHolderTest {
  @Test
  void settingTheSameMutableValueAgainInvalidatesAnOlderSnapshot() {
    var holder = new MetadataHolder<JsonObject>();
    var value = new JsonObject();
    value.addProperty("count", 1);
    holder.set("test", "value", value);
    var saved = holder.snapshotIfDirty().orElseThrow();

    value.addProperty("count", 2);
    holder.set("test", "value", value);
    holder.markClean(saved);

    assertTrue(holder.exportIfDirty().isPresent());
    holder.markClean(holder.snapshotIfDirty().orElseThrow());
    assertTrue(holder.exportIfDirty().isEmpty());
  }

  @Test
  void persistedEditsPreserveOtherPendingChangesAndInvalidateOlderAcknowledgements() {
    var holder = new MetadataHolder<Integer>();
    holder.setPersisted("test", "stored", 1);
    assertTrue(holder.exportIfDirty().isEmpty());
    holder.set("test", "pending", 2);
    var older = holder.snapshotIfDirty().orElseThrow();

    holder.removePersisted("test", "stored");
    holder.markClean(older);

    assertTrue(holder.exportIfDirty().isPresent());
    holder.markClean(holder.snapshotIfDirty().orElseThrow());
    holder.removePersisted("test", "pending");
    assertTrue(holder.exportIfDirty().isEmpty());
  }
}
