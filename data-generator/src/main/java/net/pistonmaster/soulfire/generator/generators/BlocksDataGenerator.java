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
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.pistonmaster.soulfire.generator.mixin.BlockAccessor;
import net.pistonmaster.soulfire.generator.util.BlockSettingsAccessor;

public class BlocksDataGenerator implements IDataGenerator {
    public static JsonObject generateBlock(Block block) {
        var blockDesc = new JsonObject();

        var defaultState = block.defaultBlockState();

        blockDesc.addProperty("id", BuiltInRegistries.BLOCK.getId(block));
        blockDesc.addProperty("name", BuiltInRegistries.BLOCK.getKey(block).getPath());

        blockDesc.addProperty("destroyTime", block.defaultDestroyTime());
        blockDesc.addProperty("explosionResistance", block.getExplosionResistance());
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
        if (defaultState.getFluidState().isSource()) {
            blockDesc.addProperty("fluidSource", true);
        }

        if (defaultState.hasOffsetFunction()) {
            var offsetData = new JsonObject();

            offsetData.addProperty("maxHorizontalOffset", block.getMaxHorizontalOffset());
            offsetData.addProperty("maxVerticalOffset", block.getMaxVerticalOffset());

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
                propertiesDesc.addProperty(property.getName(), Util.getPropertyName(property, state.getValue(property)));
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
        return "blocks.json";
    }

    @Override
    public JsonArray generateDataJson() {
        var resultBlocksArray = new JsonArray();

        BuiltInRegistries.BLOCK.forEach(block -> resultBlocksArray.add(generateBlock(block)));
        return resultBlocksArray;
    }
}
