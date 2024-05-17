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
package com.soulfiremc.server.protocol;

import com.soulfiremc.util.ResourceHelper;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.game.KnownPack;
import org.intellij.lang.annotations.Subst;

@Slf4j
public class BuiltInKnownPackRegistry {
  public static final BuiltInKnownPackRegistry INSTANCE = new BuiltInKnownPackRegistry();
  private final Set<KnownPack> supportedPacks = new HashSet<>();
  private final Map<Key, Map<Key, Pair<KnownPack, NbtMap>>> builtInRegistry = new HashMap<>();

  public BuiltInKnownPackRegistry() {
    var byteArrayInputStream =
      new ByteArrayInputStream(ResourceHelper.getResourceBytes("/minecraft/builtin_packs.bin.zip"));
    try (var gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
      var bytes = gzipInputStream.readAllBytes();
      var in = Unpooled.wrappedBuffer(bytes);
      var helper = new MinecraftCodecHelper(Int2ObjectMaps.emptyMap(), Map.of());

      var registriesSize = helper.readVarInt(in);
      for (var i = 0; i < registriesSize; i++) {
        @Subst("empty") var string = helper.readResourceLocation(in);
        var registryKey = Key.key(string);
        var holdersSize = helper.readVarInt(in);
        var holders = new HashMap<Key, Pair<KnownPack, NbtMap>>();
        for (var j = 0; j < holdersSize; j++) {
          var knownPack = new KnownPack(helper.readString(in), helper.readString(in), helper.readString(in));
          supportedPacks.add(knownPack);
          @Subst("empty") var string1 = helper.readResourceLocation(in);
          holders.put(Key.key(string1), Pair.of(
            knownPack,
            helper.readNullable(in, helper::readCompoundTag)
          ));
        }

        builtInRegistry.put(registryKey, holders);
      }
    } catch (Exception e) {
      log.error("Failed to load known packs", e);
    }
  }

  public List<KnownPack> getMatchingPacks(List<KnownPack> requested) {
    var returned = new ArrayList<KnownPack>();
    for (var requestedPack : requested) {
      if (supportedPacks.contains(requestedPack)) {
        returned.add(requestedPack);
      }
    }

    return returned;
  }

  public NbtMap mustFindData(Key registryKey, Key holderKey, List<KnownPack> allowedPacks) {
    var holders = builtInRegistry.get(registryKey);
    if (holders == null) {
      throw new RuntimeException("Unknown registry key: " + registryKey);
    }

    var holder = holders.get(holderKey);
    if (holder == null) {
      throw new RuntimeException("Unknown holder key: " + holderKey);
    }

    if (!allowedPacks.contains(holder.left())) {
      throw new RuntimeException("Unknown pack: " + holder.left());
    }

    return holder.right();
  }
}
