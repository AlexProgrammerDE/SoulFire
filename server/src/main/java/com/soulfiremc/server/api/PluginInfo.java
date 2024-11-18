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
package com.soulfiremc.server.api;

import com.soulfiremc.grpc.generated.ServerPlugin;
import com.soulfiremc.server.util.pf4j.SFPluginDescriptor;
import lombok.SneakyThrows;
import org.pf4j.PluginClassLoader;

import java.lang.reflect.Field;

/**
 * Represents information about a plugin.
 * All data can be shown to the user.
 *
 * @param id          The plugin ID
 * @param version     The plugin version
 * @param description The plugin description
 * @param author      The plugin author
 * @param license     The plugin license
 */
public record PluginInfo(String id, String version, String description, String author, String license, String website) {
  private static final Field PLUGIN_DESCRIPTOR_FIELD;

  static {
    try {
      PLUGIN_DESCRIPTOR_FIELD = PluginClassLoader.class.getDeclaredField("pluginDescriptor");
      PLUGIN_DESCRIPTOR_FIELD.setAccessible(true);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new PluginInfo instance from the given plugin class.
   *
   * @param clazz The plugin class
   *              (must be loaded by a {@link PluginClassLoader})
   * @return The PluginInfo instance
   */
  @SneakyThrows
  public static PluginInfo fromClassLoader(Class<?> clazz) {
    var plugin = (PluginClassLoader) clazz.getClassLoader();
    if (plugin == null) {
      throw new IllegalArgumentException("Class is not a plugin");
    }

    var descriptor = (SFPluginDescriptor) PLUGIN_DESCRIPTOR_FIELD.get(plugin);
    return new PluginInfo(
      descriptor.getPluginId(),
      descriptor.getVersion(),
      descriptor.getPluginDescription(),
      descriptor.getProvider(),
      descriptor.getLicense(),
      descriptor.website()
    );
  }

  public ServerPlugin toProto() {
    return ServerPlugin.newBuilder()
      .setId(id)
      .setVersion(version)
      .setDescription(description)
      .setAuthor(author)
      .setLicense(license)
      .setWebsite(website)
      .build();
  }
}
