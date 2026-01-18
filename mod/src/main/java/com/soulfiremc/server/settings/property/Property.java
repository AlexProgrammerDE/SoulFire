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
package com.soulfiremc.server.settings.property;

import com.soulfiremc.grpc.generated.SettingsEntryIdentifier;
import com.soulfiremc.server.settings.lib.SettingsSource;

@SuppressWarnings("unused")
public sealed interface Property<S extends SettingsSource.SourceType> permits BooleanProperty, ComboProperty, DoubleProperty, IntProperty, MinMaxProperty, StringListProperty, StringProperty {
  S sourceType();

  String namespace();

  String key();

  default SettingsEntryIdentifier toProtoIdentifier() {
    return SettingsEntryIdentifier.newBuilder()
      .setNamespace(this.namespace())
      .setKey(this.key())
      .build();
  }
}
