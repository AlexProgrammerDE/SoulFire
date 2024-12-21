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
import com.soulfiremc.generator.util.MCHelper;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.reflect.stream.RStream;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class BlocksJsonGenerator implements IDataGenerator {
  private static List<LootPoolEntryContainer> fromComposite(CompositeEntryBase compositeEntryBase) {
    return RStream.of(compositeEntryBase).withSuper().fields().by("children").get();
  }

  private static void insertNested(LootPoolEntryContainer container, Set<ResourceLocation> drops) {
    switch (container) {
      case AlternativesEntry alternativesEntry -> {
        for (var entry : fromComposite(alternativesEntry)) {
          insertNested(entry, drops);
        }
      }
      case DynamicLoot dynamicLoot -> log.debug("Dynamic loot entry found: {}", dynamicLoot);
      case LootItem lootItem -> drops.add(RStream.of(lootItem).fields().by("item").<Holder<Item>>get().unwrapKey().orElseThrow().location());
      default -> throw new IllegalStateException("Unexpected value: " + container);
    }
  }

  @SuppressWarnings("deprecation")
  public static JsonObject generateBlock(Block block) {
    var blockDesc = new JsonObject();

    blockDesc.addProperty("id", BuiltInRegistries.BLOCK.getId(block));
    blockDesc.addProperty("key", BuiltInRegistries.BLOCK.getKey(block).toString());

    blockDesc.addProperty("destroyTime", block.defaultDestroyTime());
    blockDesc.addProperty("explosionResistance", block.getExplosionResistance());
    blockDesc.addProperty("friction", block.getFriction());
    blockDesc.addProperty("jumpFactor", block.getJumpFactor());
    blockDesc.addProperty("speedFactor", block.getSpeedFactor());

    var defaultState = block.defaultBlockState();
    if (defaultState.isAir()) {
      blockDesc.addProperty("air", true);
    }
    if (block instanceof FallingBlock) {
      blockDesc.addProperty("fallingBlock", true);
    }
    if (block instanceof IceBlock) {
      blockDesc.addProperty("iceBlock", true);
    }
    if (block instanceof FenceGateBlock) {
      blockDesc.addProperty("fenceGateBlock", true);
    }
    if (block instanceof TrapDoorBlock) {
      blockDesc.addProperty("trapDoorBlock", true);
    }
    if (block instanceof BedBlock) {
      blockDesc.addProperty("bedBlock", true);
    }
    if (defaultState.canBeReplaced()) {
      blockDesc.addProperty("replaceable", true);
    }
    if (defaultState.requiresCorrectToolForDrops()) {
      blockDesc.addProperty("requiresCorrectToolForDrops", true);
    }
    if (defaultState.blocksMotion()) {
      blockDesc.addProperty("blocksMotion", true);
    }

    var lootTableLocation = block.getLootTable();
    if (lootTableLocation.isPresent()) {
      var lootTable = MCHelper.getServer().reloadableRegistries().getLootTable(lootTableLocation.get());
      var drops = new LinkedHashSet<ResourceLocation>();
      var pools = RStream.of(lootTable).fields().by("pools").<List<LootPool>>get();
      for (var pool : pools) {
        var entries = RStream.of(pool).fields().by("entries").<List<LootPoolEntryContainer>>get();
        for (var entry : entries) {
          insertNested(entry, drops);
        }
      }

      var array = new JsonArray();
      for (var entry : drops) {
        array.add(entry.toString());
      }

      blockDesc.add("possibleDrops", array);
    }

    if (defaultState.hasOffsetFunction()) {
      var offsetData = new JsonObject();

      offsetData.addProperty("maxHorizontalOffset", RStream.of(block).withSuper().methods().by("getMaxHorizontalOffset").<Float>invoke());
      offsetData.addProperty("maxVerticalOffset", RStream.of(block).withSuper().methods().by("getMaxVerticalOffset").<Float>invoke());

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

      var fluidStateDesc = new JsonObject();
      var fluidState = state.getFluidState();
      fluidStateDesc.addProperty("type", BuiltInRegistries.FLUID.getKey(fluidState.getType()).toString());
      var amount = fluidState.getAmount();
      if (amount != 0) {
        fluidStateDesc.addProperty("amount", amount);
      }
      var ownHeight = fluidState.getOwnHeight();
      if (ownHeight != 0) {
        fluidStateDesc.addProperty("ownHeight", ownHeight);
      }
      if (fluidState.isSource()) {
        fluidStateDesc.addProperty("source", true);
      }
      if (fluidState.isEmpty()) {
        fluidStateDesc.addProperty("empty", true);
      }

      var fluidPropertiesDesc = new JsonObject();
      for (var property : fluidState.getProperties()) {
        var value = fluidState.getValue(property);
        if (value instanceof Integer integer) {
          fluidPropertiesDesc.addProperty(property.getName(), integer);
        } else if (value instanceof Boolean bool) {
          fluidPropertiesDesc.addProperty(property.getName(), bool);
        } else {
          fluidPropertiesDesc.addProperty(property.getName(), Util.getPropertyName(property, value));
        }
      }

      if (!fluidPropertiesDesc.isEmpty()) {
        fluidStateDesc.add("properties", fluidPropertiesDesc);
      }

      stateDesc.add("fluidState", fluidStateDesc);

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
