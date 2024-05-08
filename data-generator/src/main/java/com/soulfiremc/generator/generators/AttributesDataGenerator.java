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
package com.soulfiremc.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class AttributesDataGenerator implements IDataGenerator {
  public static JsonObject generateAttribute(Attribute attribute) {
    var attributeDesc = new JsonObject();

    attributeDesc.addProperty(
      "id", BuiltInRegistries.ATTRIBUTE.getId(attribute));
    attributeDesc.addProperty(
      "key", Objects.requireNonNull(BuiltInRegistries.ATTRIBUTE.getKey(attribute)).toString());

    var rangedAttribute = (RangedAttribute) attribute;
    attributeDesc.addProperty("min", rangedAttribute.getMinValue());
    attributeDesc.addProperty("max", rangedAttribute.getMaxValue());

    attributeDesc.addProperty("defaultValue", rangedAttribute.getDefaultValue());

    return attributeDesc;
  }

  @Override
  public String getDataName() {
    return "data/attributes.json";
  }

  @Override
  public JsonArray generateDataJson() {
    var resultAttributesArray = new JsonArray();

    BuiltInRegistries.ATTRIBUTE.forEach(
      attribute -> resultAttributesArray.add(generateAttribute(attribute)));
    return resultAttributesArray;
  }
}
