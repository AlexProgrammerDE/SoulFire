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
package com.soulfiremc.server.script;

import lombok.RequiredArgsConstructor;
import org.graalvm.polyglot.io.FileSystem;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Sandboxed file system for scripts.
 * Scripts can only access files within their own directory or subdirectories.
 * The scheme is forced to be UNIX, and the root is the script's directory.
 */
@RequiredArgsConstructor
public class SandboxedFileSystem implements FileSystem {
  private final Path baseDir;
  private final FileSystem delegateFs = FileSystem.newDefaultFileSystem();

  @Override
  public Path parsePath(URI uri) {
    return parsePath(uri.getPath());
  }

  @Override
  public Path parsePath(String path) {
    Path resolved = Paths.get(path).normalize();
    // Make path relative to base directory
    return baseDir.resolve(resolved.toString()).normalize();
  }

  @Override
  public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
    Path resolvedPath = validatePath(path);
    delegateFs.checkAccess(resolvedPath, modes, linkOptions);
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    Path resolvedPath = validatePath(dir);
    delegateFs.createDirectory(resolvedPath, attrs);
  }

  @Override
  public void delete(Path path) throws IOException {
    Path resolvedPath = validatePath(path);
    delegateFs.delete(resolvedPath);
  }

  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    Path resolvedPath = validatePath(path);
    return delegateFs.newByteChannel(resolvedPath, options, attrs);
  }

  @SuppressWarnings("resource")
  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
    Path resolvedPath = validatePath(dir);

    final DirectoryStream<Path> original = delegateFs.newDirectoryStream(resolvedPath, filter);
    return new DirectoryStream<>() {
      @Override
      public Iterator<Path> iterator() {
        Iterator<Path> originalIter = original.iterator();
        return new Iterator<>() {
          @Override
          public boolean hasNext() {
            return originalIter.hasNext();
          }

          @Override
          public Path next() {
            Path next = originalIter.next();
            // Convert absolute paths back to relative paths for the sandbox
            return baseDir.relativize(next);
          }
        };
      }

      @Override
      public void close() throws IOException {
        original.close();
      }
    };
  }

  @Override
  public Path toAbsolutePath(Path path) {
    Path resolvedPath = validatePath(path);
    return delegateFs.toAbsolutePath(resolvedPath);
  }

  @Override
  public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
    Path resolvedPath = validatePath(path);
    Path realPath = delegateFs.toRealPath(resolvedPath, linkOptions);
    validatePath(realPath); // Ensure real path is still within sandbox
    return realPath;
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    Path resolvedPath = validatePath(path);
    return delegateFs.readAttributes(resolvedPath, attributes, options);
  }

  /**
   * Validates that the given path is within the sandbox and returns the resolved path.
   *
   * @throws SecurityException if the path is outside the sandbox
   */
  private Path validatePath(Path path) {
    Path normalizedPath;

    if (path.isAbsolute()) {
      normalizedPath = path.normalize();
    } else {
      normalizedPath = baseDir.resolve(path).normalize();
    }

    if (!normalizedPath.startsWith(baseDir)) {
      throw new SecurityException("Access to path outside sandbox: " + path);
    }

    return normalizedPath;
  }
}
