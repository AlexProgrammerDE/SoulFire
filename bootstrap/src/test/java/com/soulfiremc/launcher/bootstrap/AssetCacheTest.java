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
package com.soulfiremc.launcher.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssetCacheTest {
  @TempDir
  Path directory;

  private final byte[] content = content();

  @Test
  void reuseVerifiedArtifactAcrossLauncherRunsWithoutOpeningSource() throws Exception {
    var requests = new AtomicInteger();
    var asset = asset(content);
    var first = new AssetCache(directory, _ -> {
      requests.incrementAndGet();
      return new ByteArrayInputStream(content);
    }).materialize(asset);
    var modified = Files.getLastModifiedTime(first);

    var cached = new AssetCache(directory, _ -> {
      throw new IOException("Offline");
    }).materialize(asset);

    assertEquals(1, requests.get());
    assertEquals(modified, Files.getLastModifiedTime(cached));
    assertArrayEquals(content, Files.readAllBytes(cached));
  }

  @Test
  void repairCorruptArtifactEvenWhenItsSizeIsUnchanged() throws Exception {
    var asset = asset(content);
    var corrupt = content.clone();
    corrupt[corrupt.length / 2] ^= 1;
    var path = directory.resolve(asset.path());
    Files.createDirectories(path.getParent());
    Files.write(path, corrupt);

    var cache = new AssetCache(directory, _ -> new ByteArrayInputStream(content));
    assertArrayEquals(content, Files.readAllBytes(cache.materialize(asset)));
  }

  @Test
  void rejectWrongChecksumAndKeepPreviousArtifactIntact() throws Exception {
    var asset = asset(content);
    var previous = content.clone();
    previous[0] ^= 1;
    var path = directory.resolve(asset.path());
    Files.createDirectories(path.getParent());
    Files.write(path, previous);

    var cache = new AssetCache(directory, _ -> new ByteArrayInputStream(previous));
    assertThrows(IOException.class, () -> cache.materialize(asset));
    assertArrayEquals(previous, Files.readAllBytes(path));
    try (var files = Files.list(path.getParent())) {
      assertEquals(1, files.count());
    }
  }

  @Test
  void discardInterruptedAndOversizedDownloads() throws Exception {
    var asset = asset(content);
    var interrupted = new AssetCache(directory, _ -> new InputStream() {
      private int offset;

      @Override
      public int read() throws IOException {
        if (offset == content.length / 2) {
          throw new IOException("Connection closed");
        }
        return content[offset++] & 255;
      }
    });
    assertThrows(IOException.class, () -> interrupted.materialize(asset));
    var oversized = new AssetCache(directory, _ -> new ByteArrayInputStream(new byte[content.length + 1]));
    assertThrows(IOException.class, () -> oversized.materialize(asset));
    assertFalse(Files.exists(directory.resolve(asset.path())));
    try (var files = Files.list(directory.resolve(asset.path()).getParent())) {
      assertEquals(0, files.count());
    }
  }

  private LauncherManifest.Asset asset(byte[] bytes) throws Exception {
    var sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    return new LauncherManifest.Asset("test artifact", "libraries/test/artifact/1/artifact-1.jar", "library",
      bytes.length, sha256, "https://example.invalid/artifact.jar", null, null);
  }

  private static byte[] content() {
    var bytes = new byte[131073];
    new Random(42).nextBytes(bytes);
    return bytes;
  }
}
