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
package com.soulfiremc.server.database;

import com.google.gson.JsonElement;
import com.soulfiremc.server.settings.lib.ServerSettingsImpl;
import com.soulfiremc.server.util.structs.GsonInstance;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ServerSettingsConverter implements AttributeConverter<ServerSettingsImpl, String> {
  @Override
  public String convertToDatabaseColumn(ServerSettingsImpl attribute) {
    return GsonInstance.GSON.toJson(attribute.serializeToTree());
  }

  @Override
  public ServerSettingsImpl convertToEntityAttribute(String dbData) {
    return ServerSettingsImpl.deserialize(GsonInstance.GSON.fromJson(dbData, JsonElement.class));
  }
}
