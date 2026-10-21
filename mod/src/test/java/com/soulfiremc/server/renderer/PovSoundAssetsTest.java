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
package com.soulfiremc.server.renderer;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PovSoundAssetsTest {
  @TempDir Path directory;

  @Test
  void discoversWithoutDownloadingAndSharesVerifiedContentAcrossConcurrentReaders() throws Exception {
    var bytes = new byte[8192];
    new Random(12).nextBytes(bytes);
    var asset = asset(bytes);
    var id = Identifier.withDefaultNamespace("sounds/test.ogg");
    var downloads = new AtomicInteger();
    var sounds = new PovSoundAssets(Map.of(id, asset), directory, _ -> {
      downloads.incrementAndGet();
      return new ByteArrayInputStream(bytes);
    });
    var listed = new HashMap<Identifier, Object>();
    sounds.list("minecraft", "sounds", listed::put);
    assertEquals(1, listed.size());
    assertEquals(0, downloads.get());
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var reads = java.util.stream.IntStream.range(0, 8)
        .mapToObj(_ -> executor.submit(() -> {
          try (var input = sounds.resource(id).get()) { return input.readAllBytes(); }
        })).toList();
      for (var read : reads) assertArrayEquals(bytes, read.get());
    }
    assertEquals(1, downloads.get());
    var offline = new PovSoundAssets(Map.of(id, asset), directory, _ -> { throw new IOException("Offline"); });
    try (var input = offline.resource(id).get()) { assertArrayEquals(bytes, input.readAllBytes()); }
  }

  @Test
  void repairsCorruptCacheAndRejectsOversizedOrIncorrectDownloads() throws Exception {
    var bytes = new byte[64];
    new Random(3).nextBytes(bytes);
    var asset = asset(bytes);
    var id = Identifier.withDefaultNamespace("sounds/test.ogg");
    var file = directory.resolve(asset.hash().substring(0, 2)).resolve(asset.hash());
    Files.createDirectories(file.getParent());
    Files.write(file, new byte[bytes.length]);
    var sounds = new PovSoundAssets(Map.of(id, asset), directory, _ -> new ByteArrayInputStream(bytes));
    try (var input = sounds.resource(id).get()) { assertArrayEquals(bytes, input.readAllBytes()); }
    Files.delete(file);
    for (var invalid : new byte[][]{new byte[bytes.length], new byte[bytes.length + 1], new byte[bytes.length - 1]}) {
      var invalidSounds = new PovSoundAssets(Map.of(id, asset), directory, _ -> new ByteArrayInputStream(invalid));
      assertThrows(IOException.class, () -> invalidSounds.resource(id).get());
      try (var remaining = Files.list(file.getParent())) { assertTrue(remaining.findAny().isEmpty()); }
    }
  }

  private static PovSoundAssets.Asset asset(byte[] data) throws Exception {
    return new PovSoundAssets.Asset(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data)), data.length);
  }
}
