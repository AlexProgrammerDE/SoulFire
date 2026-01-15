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
package com.soulfiremc.server.api;

import com.soulfiremc.grpc.generated.ServerPlugin;

/// Represents information about a plugin.
/// All data can be shown to the user.
///
/// @param id          The plugin ID
/// @param version     The plugin version
/// @param description The plugin description
/// @param author      The plugin author
/// @param license     The plugin license
public record PluginInfo(String id, String version, String description, String author, String license, String website) {
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
