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
import com.soulfiremc.generator.mixin.BlockAccessor;
import com.soulfiremc.generator.util.BlockSettingsAccessor;
import com.soulfiremc.generator.util.GsonInstance;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.SneakyThrows;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class BlocksDataGenerator implements IDataGenerator {
  @SneakyThrows
  public static JsonObject generateBlock(Block block) {
    var blockDesc = new JsonObject();

    blockDesc.addProperty("id", BuiltInRegistries.BLOCK.getId(block));
    blockDesc.addProperty("key", BuiltInRegistries.BLOCK.getKey(block).toString());

    blockDesc.addProperty("destroyTime", block.defaultDestroyTime());
    blockDesc.addProperty("explosionResistance", block.getExplosionResistance());

    var defaultState = block.defaultBlockState();
    if (defaultState.isAir()) {
      blockDesc.addProperty("air", true);
    }
    if (block instanceof FallingBlock) {
      blockDesc.addProperty("fallingBlock", true);
    }
    if (defaultState.canBeReplaced()) {
      blockDesc.addProperty("replaceable", true);
    }
    if (defaultState.requiresCorrectToolForDrops()) {
      blockDesc.addProperty("requiresCorrectToolForDrops", true);
    }

    blockDesc.addProperty("fluidType",
      BuiltInRegistries.FLUID.getKey(defaultState.getFluidState().getType()).toString());

    JsonArray poolsArray;
    var lootTableKey = block.getLootTable();
    if (lootTableKey == BuiltInLootTables.EMPTY) {
      poolsArray = new JsonArray();
    } else {
      var lootTableFilePath = "/data/minecraft/loot_table/%s.json".formatted(block.getLootTable().location().getPath());
      try (var in = Objects.requireNonNull(BlocksDataGenerator.class.getResourceAsStream(lootTableFilePath))) {
        var data = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        var dataJsonObject = GsonInstance.GSON.fromJson(data, JsonObject.class);

        poolsArray = dataJsonObject.getAsJsonArray("pools");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    blockDesc.add("lootTableData", poolsArray);

    if (defaultState.hasOffsetFunction()) {
      var offsetData = new JsonObject();

      var horizontalOffsetMethod = BlockBehaviour.class.getDeclaredMethod("getMaxHorizontalOffset");
      var verticalOffsetMethod = BlockBehaviour.class.getDeclaredMethod("getMaxVerticalOffset");
      horizontalOffsetMethod.setAccessible(true);
      verticalOffsetMethod.setAccessible(true);

      offsetData.addProperty("maxHorizontalOffset", (float) horizontalOffsetMethod.invoke(block));
      offsetData.addProperty("maxVerticalOffset", (float) verticalOffsetMethod.invoke(block));

      var blockSettings = ((BlockAccessor) block).properties();
      var offsetType = ((BlockSettingsAccessor) blockSettings).soulfire$getOffsetType();
      offsetData.addProperty("offsetType", offsetType.name());

      blockDesc.add("offsetData", offsetData);
    }

    var statesArray = new JsonArray();
    for (var state : block.getStateDefinition().getPossibleStates()) {
      var stateDesc = new JsonObject();

      stateDesc.addProperty("id", Block.getId(state));

      if (state == defaultState) {
        stateDesc.addProperty("default", true);
      }

      var propertiesDesc = new JsonObject();
      for (var property : state.getProperties()) {
        var value = state.getValue(property);
        if (value instanceof Integer integer) {
          propertiesDesc.addProperty(property.getName(), integer);
        } else if (value instanceof Boolean bool) {
          propertiesDesc.addProperty(property.getName(), bool);
        } else {
          propertiesDesc.addProperty(property.getName(), Util.getPropertyName(property, value));
        }
      }

      if (!propertiesDesc.isEmpty()) {
        stateDesc.add("properties", propertiesDesc);
      }

      statesArray.add(stateDesc);
    }

    blockDesc.add("states", statesArray);

    return blockDesc;
  }

  @Override
  public String getDataName() {
    return "data/blocks.json";
  }

  @Override
  public JsonArray generateDataJson() {
    var resultBlocksArray = new JsonArray();

    BuiltInRegistries.BLOCK.forEach(block -> resultBlocksArray.add(generateBlock(block)));
    return resultBlocksArray;
  }
}
