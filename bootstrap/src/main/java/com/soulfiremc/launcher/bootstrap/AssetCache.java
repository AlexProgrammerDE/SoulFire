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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class AssetCache {
  private final Path directory;
  private final Source source;

  AssetCache(Path directory, Source source) {
    this.directory = directory.toAbsolutePath().normalize();
    this.source = source;
  }

  Path materialize(LauncherManifest.Asset asset) throws IOException {
    var destination = directory.resolve(asset.path()).normalize();
    if (!destination.startsWith(directory)) {
      throw new IOException("Asset path leaves the cache: " + asset.path());
    }
    if (Files.isRegularFile(destination) && Files.size(destination) == asset.size()
      && checksum(destination).equals(asset.sha256())) {
      return destination;
    }

    IO.println("Preparing " + asset.name());
    Files.createDirectories(destination.getParent());
    var temporary = Files.createTempFile(destination.getParent(), ".download-", ".tmp");
    try {
      var digest = sha256();
      long size = 0;
      try (var input = source.open(asset); var output = Files.newOutputStream(temporary)) {
        var buffer = new byte[65536];
        int count;
        while ((count = input.read(buffer)) != -1) {
          size += count;
          if (size > asset.size()) {
            throw new IOException("Asset exceeds its pinned size: " + asset.name());
          }
          digest.update(buffer, 0, count);
          output.write(buffer, 0, count);
        }
      }
      if (size != asset.size() || !HexFormat.of().formatHex(digest.digest()).equals(asset.sha256())) {
        throw new IOException("Asset checksum or size mismatch: " + asset.name());
      }
      try {
        Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException _) {
        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
      }
      return destination;
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static String checksum(Path file) throws IOException {
    var digest = sha256();
    try (var input = new DigestInputStream(Files.newInputStream(file), digest)) {
      input.transferTo(OutputStream.nullOutputStream());
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @FunctionalInterface
  interface Source {
    InputStream open(LauncherManifest.Asset asset) throws IOException;
  }
}
