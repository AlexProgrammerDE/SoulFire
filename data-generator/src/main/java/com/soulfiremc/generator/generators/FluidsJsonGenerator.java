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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

public class FluidsJsonGenerator implements IDataGenerator {
  public static JsonObject generateFluid(Fluid fluid) {
    var fluidDesc = new JsonObject();

    fluidDesc.addProperty("id", BuiltInRegistries.FLUID.getId(fluid));
    fluidDesc.addProperty("key", BuiltInRegistries.FLUID.getKey(fluid).toString());

    return fluidDesc;
  }

  @Override
  public String getDataName() {
    return "data/fluids.json";
  }

  @Override
  public JsonArray generateDataJson() {
    var resultBlocksArray = new JsonArray();

    BuiltInRegistries.FLUID.forEach(fluid -> resultBlocksArray.add(generateFluid(fluid)));
    return resultBlocksArray;
  }
}
