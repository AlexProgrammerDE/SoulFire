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
package com.soulfiremc.generator.generators;

import com.mojang.serialization.DynamicOps;
import com.soulfiremc.generator.util.MCHelper;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.packs.repository.KnownPack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class DefaultPacksDataGenerator implements IDataGenerator {
  public static void packRegistries(
    FriendlyByteBuf friendlyByteBuf,
    DynamicOps<Tag> ops,
    RegistryAccess registryAccess
  ) {
    friendlyByteBuf.writeCollection(RegistryDataLoader.SYNCHRONIZED_REGISTRIES, (buf, registry) -> {
      friendlyByteBuf.writeResourceKey(registry.key());
      packRegistry(friendlyByteBuf, ops, (RegistryDataLoader.RegistryData<?>) registry, registryAccess);
    });
  }

  private static <T> void packRegistry(
    FriendlyByteBuf friendlyByteBuf,
    DynamicOps<Tag> ops,
    RegistryDataLoader.RegistryData<T> registryData,
    RegistryAccess registryAccess
  ) {
    var registry = registryAccess.lookupOrThrow(registryData.key());
    friendlyByteBuf.writeCollection(registry.listElements().toList(), (buf, holder) -> {
      var holderPack = registry.registrationInfo(holder.key()).flatMap(RegistrationInfo::knownPackInfo).orElseThrow();
      var holderData = registryData.elementCodec()
        .encodeStart(ops, holder.value())
        .getOrThrow(string -> new IllegalArgumentException("Failed to serialize " + holder.key() + ": " + string));

      KnownPack.STREAM_CODEC.encode(friendlyByteBuf, holderPack);
      RegistrySynchronization.PackedRegistryEntry.STREAM_CODEC.encode(friendlyByteBuf,
        new RegistrySynchronization.PackedRegistryEntry(holder.key().location(), Optional.of(holderData)));
    });
  }

  @Override
  public String getDataName() {
    return "data/builtin_packs.bin.zip";
  }

  @SneakyThrows
  @Override
  public byte[] generateDataJson() {
    var byteOutputStream = new ByteArrayOutputStream();
    try (var gzipOutputStream = new GZIPOutputStream(byteOutputStream)) {
      var buf = Unpooled.buffer();
      var friendlyByteBuf = new FriendlyByteBuf(buf);
      friendlyByteBuf.writeCollection(
        MCHelper.getServer().getResourceManager().listPacks().flatMap(arg -> arg.location().knownPackInfo().stream()).toList(),
        KnownPack.STREAM_CODEC
      );

      var registries = MCHelper.getServer().registries();
      packRegistries(
        friendlyByteBuf,
        registries.compositeAccess().createSerializationContext(NbtOps.INSTANCE),
        registries.getAccessFrom(RegistryLayer.WORLDGEN)
      );

      var bytes = new byte[buf.readableBytes()];
      buf.readBytes(bytes);
      gzipOutputStream.write(bytes);

      gzipOutputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return byteOutputStream.toByteArray();
  }
}
