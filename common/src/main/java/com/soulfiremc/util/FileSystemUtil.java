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
package com.soulfiremc.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FileSystemUtil {

  private FileSystemUtil() {}

  public static Map<Path, byte[]> getFilesInDirectory(final String assetPath)
    throws IOException, URISyntaxException {
    var resource = FileSystemUtil.class.getResource(assetPath);
    if (resource == null) {
      return Collections.emptyMap();
    }

    var uri = resource.toURI();
    if (uri.getScheme().equals("file")) {
      return getFilesInPath(Paths.get(uri));
    } else if (uri.getScheme().equals("jar")) {
      return runInFileSystem(
        uri,
        fileSystem -> {
          try {
            return getFilesInPath(fileSystem.getPath(assetPath));
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
    } else {
      throw new IllegalArgumentException("Unsupported URI scheme: " + uri.getScheme());
    }
  }

  private static Map<Path, byte[]> getFilesInPath(final Path path) throws IOException {
    try (var stream = Files.walk(path)) {
      return stream
        .filter(Files::isRegularFile)
        .sorted(Comparator.comparing(Path::toString))
        .collect(
          Collectors.toMap(
            f -> f,
            f -> {
              try {
                return Files.readAllBytes(f);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            },
            (u, v) -> {
              throw new IllegalStateException("Duplicate key");
            },
            LinkedHashMap::new));
    }
  }

  private static <R> R runInFileSystem(final URI uri, final Function<FileSystem, R> action) {
    try {
      return action.apply(FileSystems.getFileSystem(uri));
    } catch (FileSystemNotFoundException e) {
      try (var fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
        return action.apply(fileSystem);
      } catch (IOException e1) {
        throw new UncheckedIOException(e1);
      }
    }
  }
}
