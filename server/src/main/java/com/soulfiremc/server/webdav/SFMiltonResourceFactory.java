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
package com.soulfiremc.server.webdav;

import io.milton.http.HttpManager;
import io.milton.http.ResourceFactory;
import io.milton.http.fs.FileSystemResourceFactory;
import io.milton.resource.Resource;
import io.milton.servlet.Config;
import io.milton.servlet.Initable;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;

public class SFMiltonResourceFactory implements ResourceFactory, Initable {
  private FileSystemResourceFactory fileSystemResourceFactory;

  @Override
  public @Nullable Resource getResource(String host, String path) {
    // Your custom logic to control file access
    // For example, restrict certain paths or validate permissions
    if (path.contains("restricted")) {
      return null; // Return null to deny access
    }

    return fileSystemResourceFactory.getResource(host, path);
  }

  @Override
  public void init(Config config, HttpManager manager) {
    this.fileSystemResourceFactory = new FileSystemResourceFactory(
      Path.of(config.getInitParameter("soulfire.objectStoragePath")).toFile(), new SFMiltonSecurityManager());
  }

  @Override
  public void destroy(HttpManager manager) {
    this.fileSystemResourceFactory = null;
  }
}
