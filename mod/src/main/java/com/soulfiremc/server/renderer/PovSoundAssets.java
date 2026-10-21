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

import com.google.common.io.ByteStreams;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// Build-pinned vanilla sounds. Resource discovery reads metadata; only opening a sound downloads it.
public final class PovSoundAssets {
  private final Map<Identifier, Asset> assets;
  private final Path objects;
  private final Downloader downloader;
  private final Map<String, Path> verified = new ConcurrentHashMap<>();

  private final Map<String, Object> locks = new ConcurrentHashMap<>();

  private static final class Shared {
    private static final PovSoundAssets INSTANCE = load();
  }

  private static final class Network {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
  }

  public static PovSoundAssets shared() { return Shared.INSTANCE; }

  private static PovSoundAssets load() {
    var index = System.getProperty("sf.soundIndex");
    if (index == null) return new PovSoundAssets(Map.of(), Path.of("."), _ -> { throw new IOException("No sound index"); });
    try {
      return read(Path.of(index), asset -> {
        var request = HttpRequest.newBuilder(URI.create("https://resources.download.minecraft.net/" + asset.hash().substring(0, 2) + "/" + asset.hash()))
          .timeout(Duration.ofSeconds(30)).GET().build();
        try {
          var response = Network.CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
          if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("Minecraft sound download returned HTTP " + response.statusCode());
          }
          return response.body();
        } catch (InterruptedException error) {
          Thread.currentThread().interrupt();
          throw new IOException("Minecraft sound download interrupted", error);
        }
      });
    } catch (IOException error) {
      throw new IllegalStateException("Cannot read the pinned sound index", error);
    }
  }

  PovSoundAssets(Map<Identifier, Asset> assets, Path objects, Downloader downloader) {
    this.assets = Map.copyOf(assets);
    this.objects = objects;
    this.downloader = downloader;
  }

  static PovSoundAssets read(Path index, Downloader downloader) throws IOException {
    var assets = new LinkedHashMap<Identifier, Asset>();
    try (var reader = Files.newBufferedReader(index)) {
      for (var entry : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("objects").entrySet()) {
        var key = entry.getKey();
        if (!key.equals("minecraft/sounds.json") && !key.startsWith("minecraft/sounds/")) continue;
        var value = entry.getValue().getAsJsonObject();
        var hash = value.get("hash").getAsString();
        var size = value.get("size").getAsLong();
        if (!hash.matches("[0-9a-f]{40}") || size < 0 || size > 256_000_000) throw new IOException("Invalid sound asset metadata");
        assets.put(Identifier.withDefaultNamespace(key.substring("minecraft/".length())), new Asset(hash, size));
      }
    }
    return new PovSoundAssets(assets, index.getParent().getParent().resolve("objects"), downloader);
  }

  public IoSupplier<InputStream> resource(Identifier location) {
    var asset = assets.get(location);
    return asset == null ? null : () -> Files.newInputStream(materialize(asset));
  }

  public void list(String namespace, String directory, PackResources.ResourceOutput output) {
    assets.forEach((location, _) -> {
      if (location.getNamespace().equals(namespace) && location.getPath().startsWith(directory + "/")) {
        output.accept(location, resource(location));
      }
    });
  }

  private Path materialize(Asset asset) throws IOException {
    synchronized (locks.computeIfAbsent(asset.hash(), _ -> new Object())) {
      return materializeLocked(asset);
    }
  }

  private Path materializeLocked(Asset asset) throws IOException {
    var cached = verified.get(asset.hash());
    if (cached != null && Files.isRegularFile(cached)) return cached;
    var file = objects.resolve(asset.hash().substring(0, 2)).resolve(asset.hash());
    if (valid(file, asset)) { verified.put(asset.hash(), file); return file; }
    Files.createDirectories(file.getParent());
    var temporary = Files.createTempFile(file.getParent(), ".sound-", ".tmp");
    try {
      var digest = sha1();
      long size;
      try (var input = new DigestInputStream(downloader.open(asset), digest); var output = Files.newOutputStream(temporary)) {
        size = ByteStreams.limit(input, asset.size() + 1).transferTo(output);
      }
      if (size != asset.size() || !HexFormat.of().formatHex(digest.digest()).equals(asset.hash())) throw new IOException("Sound download failed integrity verification");
      try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
      catch (AtomicMoveNotSupportedException _) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
      verified.put(asset.hash(), file);
      return file;
    } finally { Files.deleteIfExists(temporary); }
  }

  private static boolean valid(Path file, Asset asset) throws IOException {
    if (!Files.isRegularFile(file) || Files.size(file) != asset.size()) return false;
    var digest = sha1();
    try (var input = new DigestInputStream(Files.newInputStream(file), digest)) { input.transferTo(OutputStream.nullOutputStream()); }
    return HexFormat.of().formatHex(digest.digest()).equals(asset.hash());
  }

  private static MessageDigest sha1() {
    try { return MessageDigest.getInstance("SHA-1"); }
    catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
  }

  record Asset(String hash, long size) {}

  @FunctionalInterface
  interface Downloader { InputStream open(Asset asset) throws IOException; }
}
