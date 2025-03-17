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

import lombok.SneakyThrows;
import org.graalvm.polyglot.io.FileSystem;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.Set;

/**
 * Sandboxed file system for scripts.
 * Scripts can only access files within their own directory or subdirectories.
 * The scheme is forced to be UNIX, and the root is the script's directory.
 */
public class SandboxedFileSystem implements FileSystem {
  private final Path baseDir;
  private final Path tempDir;
  private Path workingDir;
  private final FileSystem delegateFs = FileSystem.newDefaultFileSystem();

  @SneakyThrows
  public SandboxedFileSystem(Path baseDir) {
    this.baseDir = baseDir;
    this.tempDir = Files.createTempDirectory("sandbox-tmp-dir");
    this.workingDir = baseDir;
  }

  @Override
  public Path parsePath(URI uri) {
    return validatePath(delegateFs.parsePath(uri));
  }

  @Override
  public Path parsePath(String path) {
    return validatePath(delegateFs.parsePath(path));
  }

  @Override
  public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
    var resolvedPath = validatePath(path);
    delegateFs.checkAccess(resolvedPath, modes, linkOptions);
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    var resolvedPath = validatePath(dir);
    delegateFs.createDirectory(resolvedPath, attrs);
  }

  @Override
  public void delete(Path path) throws IOException {
    var resolvedPath = validatePath(path);
    delegateFs.delete(resolvedPath);
  }

  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    var resolvedPath = validatePath(path);
    return delegateFs.newByteChannel(resolvedPath, options, attrs);
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
    var resolvedPath = validatePath(dir);
    return delegateFs.newDirectoryStream(resolvedPath, filter);
  }

  @Override
  public Path toAbsolutePath(Path path) {
    var resolvedPath = validatePath(path);
    return delegateFs.toAbsolutePath(resolvedPath);
  }

  @Override
  public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
    var resolvedPath = validatePath(path);
    var realPath = delegateFs.toRealPath(resolvedPath, linkOptions);
    validatePath(realPath); // Ensure real path is still within sandbox
    return realPath;
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    var resolvedPath = validatePath(path);
    return delegateFs.readAttributes(resolvedPath, attributes, options);
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    var resolvedPath = validatePath(path);
    delegateFs.setAttribute(resolvedPath, attribute, value, options);
  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    var resolvedSource = validatePath(source);
    var resolvedTarget = validatePath(target);
    delegateFs.copy(resolvedSource, resolvedTarget, options);
  }

  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    var resolvedSource = validatePath(source);
    var resolvedTarget = validatePath(target);
    delegateFs.move(resolvedSource, resolvedTarget, options);
  }

  @Override
  public void createLink(Path link, Path existing) throws IOException {
    var resolvedLink = validatePath(link);
    var resolvedExisting = validatePath(existing);
    delegateFs.createLink(resolvedLink, resolvedExisting);
  }

  @Override
  public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
    var resolvedLink = validatePath(link);
    var resolvedTarget = validatePath(target);
    delegateFs.createSymbolicLink(resolvedLink, resolvedTarget, attrs);
  }

  @Override
  public Path readSymbolicLink(Path link) throws IOException {
    var resolvedLink = validatePath(link);
    return delegateFs.readSymbolicLink(resolvedLink);
  }

  @Override
  public void setCurrentWorkingDirectory(Path currentWorkingDirectory) {
    this.workingDir = validatePath(currentWorkingDirectory);
  }

  @Override
  public String getSeparator() {
    return delegateFs.getSeparator();
  }

  @Override
  public String getPathSeparator() {
    return delegateFs.getPathSeparator();
  }

  @Override
  public String getMimeType(Path path) {
    var resolvedPath = validatePath(path);
    return delegateFs.getMimeType(resolvedPath);
  }

  @Override
  public Charset getEncoding(Path path) {
    var resolvedPath = validatePath(path);
    return delegateFs.getEncoding(resolvedPath);
  }

  @Override
  public Path getTempDirectory() {
    return delegateFs.getTempDirectory();
  }

  @Override
  public boolean isSameFile(Path path1, Path path2, LinkOption... options) throws IOException {
    var resolvedPath1 = validatePath(path1);
    var resolvedPath2 = validatePath(path2);

    return delegateFs.isSameFile(resolvedPath1, resolvedPath2, options);
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
      normalizedPath = workingDir.resolve(path).normalize();
    }

    if (!normalizedPath.startsWith(baseDir) && !normalizedPath.startsWith(tempDir)) {
      throw new SecurityException("Access to path outside sandbox: " + path);
    }

    return normalizedPath;
  }
}
