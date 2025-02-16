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
package com.soulfiremc.server.util;

import com.soulfiremc.server.util.structs.CancellationCollector;
import lombok.extern.slf4j.Slf4j;
import org.shredzone.acme4j.exception.AcmeProtocolException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.*;
import java.util.regex.Pattern;

@Slf4j
public class SFHelpers {
  public static final char[] ILLEGAL_FILE_CHARACTERS = new char[]{'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};
  private static final Pattern RESERVED_WINDOWS_FILENAMES = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", Pattern.CASE_INSENSITIVE);

  private SFHelpers() {}

  public static byte[][] split(byte[] data, byte separator) {
    var count = 0;
    for (var b : data) {
      if (b == separator) {
        count++;
      }
    }

    if (count == 0) {
      return new byte[][]{data};
    }

    var result = new byte[count + 1][];
    var last = 0;
    var index = 0;
    for (var i = 0; i < data.length; i++) {
      if (data[i] == separator) {
        result[index++] = Arrays.copyOfRange(data, last, i);
        last = i + 1;
      }
    }

    result[index] = Arrays.copyOfRange(data, last, data.length);
    return result;
  }

  public static OptionalInt parseInt(String s) {
    try {
      return OptionalInt.of(Integer.parseInt(s));
    } catch (NumberFormatException e) {
      return OptionalInt.empty();
    }
  }

  public static BooleanSupplier not(BooleanSupplier supplier) {
    return () -> !supplier.getAsBoolean();
  }

  public static void writeIfNeeded(Path path, String content) throws IOException {
    if (Files.exists(path)) {
      var existingContent = Files.readString(path);
      if (!existingContent.equals(content)) {
        Files.writeString(path, content);
      }
    } else {
      Files.createDirectories(path.getParent());
      Files.writeString(path, content);
    }
  }

  public static <S, T> List<T> maxFutures(int maxFutures, Collection<S> source, Function<S, CompletableFuture<T>> toFuture, Consumer<T> onProgress, CancellationCollector cancellationCollector) {
    final var sourceIter = source.iterator();
    final var futures = new ArrayList<CompletableFuture<Void>>(maxFutures);
    final var result = new ArrayList<T>(source.size());
    while (sourceIter.hasNext()) {
      while (futures.size() < maxFutures && sourceIter.hasNext()) {
        // Abort if cancelled
        if (cancellationCollector.cancelled()) {
          return List.of();
        }

        futures.add(toFuture.apply(sourceIter.next()).thenAccept(r -> {
          onProgress.accept(r);
          synchronized (result) {
            result.add(r);
          }
        }));
      }

      CompletableFuture.anyOf(futures.toArray(CompletableFuture[]::new)).join();
      futures.removeIf(CompletableFuture::isDone);
    }

    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    return result;
  }

  public static int getRandomInt(int min, int max) {
    if (min > max) {
      throw new IllegalArgumentException("min must not be greater than max");
    }

    if (min == max) {
      return min;
    }

    return ThreadLocalRandom.current().nextInt(min, max);
  }

  public static <E> E getRandomEntry(List<E> list) {
    if (list.isEmpty()) {
      throw new IllegalArgumentException("List must not be empty");
    }

    return list.get(ThreadLocalRandom.current().nextInt(list.size()));
  }

  public static boolean isNewer(String currentVersion, String checkVersion) {
    currentVersion = currentVersion.replace("-SNAPSHOT", "");
    checkVersion = checkVersion.replace("-SNAPSHOT", "");

    try {
      var currentVersionData =
        Arrays.stream(currentVersion.split("\\.")).mapToInt(Integer::parseInt).toArray();
      var checkVersionData =
        Arrays.stream(checkVersion.split("\\.")).mapToInt(Integer::parseInt).toArray();

      var i = 0;
      for (var version : checkVersionData) {
        if (i == currentVersionData.length) {
          return true;
        }

        if (version > currentVersionData[i]) {
          return true;
        } else if (version < currentVersionData[i]) {
          return false;
        }

        i++;
      }
    } catch (NumberFormatException e) {
      log.error("Error while parsing version!", e);
    }

    return false;
  }

  public static String getResourceAsString(String path) {
    return new String(getResourceAsBytes(path), StandardCharsets.UTF_8);
  }

  public static byte[] getResourceAsBytes(String path) {
    try {
      return Objects.requireNonNull(SFHelpers.class.getClassLoader().getResourceAsStream(path), path).readAllBytes();
    } catch (Exception e) {
      throw new RuntimeException("Failed to get file", e);
    }
  }

  public static <T> T make(T object, Consumer<? super T> consumer) {
    consumer.accept(object);
    return object;
  }

  public static String stripForChat(String s) {
    var builder = new StringBuilder(s.length());
    for (var c : s.toCharArray()) {
      if (isAllowedChatCharacter(c)) {
        builder.append(c);
      }
    }

    return builder.toString();
  }

  public static boolean isAllowedChatCharacter(char c) {
    return c != 167 && c >= ' ' && c != 127;
  }

  public static void mustSupply(Supplier<Runnable> supplier) {
    supplier.get().run();
  }

  public static String sanitizeFileName(String name) {
    for (var c : ILLEGAL_FILE_CHARACTERS) {
      name = name.replace(c, '_');
    }

    name = name.replaceAll("[./\"]", "_");

    if (RESERVED_WINDOWS_FILENAMES.matcher(name).matches()) {
      name = "_" + name + "_";
    }

    return name;
  }

  public static byte[] md5Hash(String str) {
    try {
      var md = MessageDigest.getInstance("MD5");
      md.update(str.getBytes(StandardCharsets.UTF_8));
      return md.digest();
    } catch (NoSuchAlgorithmException ex) {
      throw new AcmeProtocolException("Could not compute hash", ex);
    }
  }

  public static <T> Optional<T> awaitResultPredicate(Iterator<T> iterator, Predicate<T> function) {
    while (iterator.hasNext()) {
      var next = iterator.next();
      if (function.test(next)) {
        return Optional.of(next);
      }
    }

    return Optional.empty();
  }
}
