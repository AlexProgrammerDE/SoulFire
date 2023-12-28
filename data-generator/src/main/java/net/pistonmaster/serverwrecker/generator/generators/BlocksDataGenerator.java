package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.pistonmaster.serverwrecker.generator.mixin.BlockAccessor;
import net.pistonmaster.serverwrecker.generator.util.BlockSettingsAccessor;

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
            var offsetType = ((BlockSettingsAccessor) blockSettings).serverwrecker$getOffsetType();
            offsetData.addProperty("offsetType", offsetType.name());

            blockDesc.add("offsetData", offsetData);
        }

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
