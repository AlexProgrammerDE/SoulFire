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
package com.soulfiremc.server.settings.lib;

import com.google.gson.JsonElement;
import com.soulfiremc.grpc.generated.BotConfig;
import com.soulfiremc.server.settings.property.Property;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@With
@Slf4j
public record BotSettingsImpl(Stem stem, InstanceSettingsSource instanceSettings) implements BotSettingsSource {
  @Override
  public Optional<JsonElement> get(Property<SettingsSource.Bot> property) {
    return this.stem.get(property)
      // TODO: Properly store a base BotSettings steam inside a InstanceSettings stem in the future
      .or(() -> SettingsSource.Stem.getFromRawSettings(instanceSettings.stem().settings(), property));
  }

  @With
  public record Stem(Map<String, Map<String, JsonElement>> settings) implements SettingsSource.Stem<SettingsSource.Bot> {
    public static final Stem EMPTY = new Stem(Map.of());

    public static Stem fromProto(BotConfig request) {
      return new Stem(
        SettingsSource.Stem.settingsFromProto(request.getSettingsList())
      );
    }

    public BotConfig toProto() {
      return BotConfig.newBuilder()
        .addAllSettings(this.settingsToProto())
        .build();
    }
  }
}
