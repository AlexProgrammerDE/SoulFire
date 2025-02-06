/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.util.structs;

import com.google.gson.reflect.TypeToken;
import com.soulfiremc.server.protocol.bot.state.TagsState;
import com.soulfiremc.server.util.SFHelpers;
import net.kyori.adventure.key.Key;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@SuppressWarnings("unchecked")
public class DefaultTagsState {
  public static final TagsState TAGS_STATE = new TagsState();

  static {
    var byteArrayInputStream =
      new ByteArrayInputStream(SFHelpers.getResourceAsBytes("minecraft/default_tags.json.zip"));
    try (var gzipInputStream = new GZIPInputStream(byteArrayInputStream);
         var reader = new InputStreamReader(gzipInputStream)) {
      var type = new TypeToken<Map<Key, Map<Key, int[]>>>() {}.getType();
      var data = GsonInstance.GSON.fromJson(reader, type);
      TAGS_STATE.handleTagData((Map<Key, Map<Key, int[]>>) data);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load default tags", e);
    }
  }
}
