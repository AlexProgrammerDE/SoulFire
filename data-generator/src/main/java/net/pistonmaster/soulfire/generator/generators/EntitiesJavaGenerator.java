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

import net.minecraft.core.registries.BuiltInRegistries;
import net.pistonmaster.soulfire.generator.util.GeneratorConstants;
import net.pistonmaster.soulfire.generator.util.ResourceHelper;

import java.util.Locale;

public class EntitiesJavaGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "EntityType.java";
    }

    @Override
    public String generateDataJson() {
        var base = ResourceHelper.getResource("/templates/EntityType.java");
        return base.replace(GeneratorConstants.VALUES_REPLACE, String.join("\n    ",
                BuiltInRegistries.ENTITY_TYPE
                        .stream().map(s -> {
                            var name = BuiltInRegistries.ENTITY_TYPE.getKey(s).getPath();
                            return "public static final EntityType " + name.toUpperCase(Locale.ROOT) + " = register(\"" + name + "\");";
                        })
                        .toArray(String[]::new)));
    }
}
