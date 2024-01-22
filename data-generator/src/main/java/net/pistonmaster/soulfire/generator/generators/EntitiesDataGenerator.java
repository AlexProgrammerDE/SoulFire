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
package net.pistonmaster.soulfire.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

public class EntitiesDataGenerator implements IDataGenerator {

    public static JsonObject generateEntity(EntityType<?> entityType) {
        var entityDesc = new JsonObject();

        entityDesc.addProperty("id", BuiltInRegistries.ENTITY_TYPE.getId(entityType));
        entityDesc.addProperty("name", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath());

        var dimensions = entityType.getDimensions();
        entityDesc.addProperty("width", dimensions.width);
        entityDesc.addProperty("height", dimensions.height);

        var category = entityType.getCategory();
        entityDesc.addProperty("category", category.getName());
        entityDesc.addProperty("friendly", category.isFriendly());

        return entityDesc;
    }

    @Override
    public String getDataName() {
        return "entities.json";
    }

    @Override
    public JsonArray generateDataJson() {
        var resultArray = new JsonArray();
        BuiltInRegistries.ENTITY_TYPE.forEach(entity -> resultArray.add(generateEntity(entity)));
        return resultArray;
    }
}
