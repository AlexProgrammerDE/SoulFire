package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.pistonmaster.serverwrecker.generator.mixin.BlockAccessor;
import net.pistonmaster.serverwrecker.generator.util.BlockSettingsAccessor;
import net.pistonmaster.serverwrecker.generator.util.DGU;

public class BlocksDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "blocks.json";
    }

    @Override
    public JsonArray generateDataJson() {
        var resultBlocksArray = new JsonArray();
        var blockRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.BLOCK);

        blockRegistry.forEach(block -> resultBlocksArray.add(generateBlock(blockRegistry, block)));
        return resultBlocksArray;
    }

    public static JsonObject generateBlock(Registry<Block> blockRegistry, Block block) {
        var blockDesc = new JsonObject();

        var defaultState = block.defaultBlockState();
        var registryKey = blockRegistry.getResourceKey(block).orElseThrow().location();
        var localizationKey = block.getDescriptionId();

        blockDesc.addProperty("id", blockRegistry.getId(block));
        blockDesc.addProperty("name", registryKey.getPath());
        blockDesc.addProperty("displayName", DGU.translateText(localizationKey));

        blockDesc.addProperty("hardness", block.defaultDestroyTime());
        blockDesc.addProperty("resistance", block.getExplosionResistance());
        blockDesc.addProperty("stackSize", block.asItem().getMaxStackSize());
        blockDesc.addProperty("air", defaultState.isAir());
        blockDesc.addProperty("fallingBlock", block instanceof FallingBlock);
        blockDesc.addProperty("replaceable", defaultState.canBeReplaced());

        if (defaultState.hasOffsetFunction()) {
            var offsetData = new JsonObject();

            offsetData.addProperty("maxHorizontalOffset", block.getMaxHorizontalOffset());
            offsetData.addProperty("verticalModelOffsetMultiplier", block.getMaxVerticalOffset());

            var blockSettings = ((BlockAccessor) block).properties();
            var offsetType = ((BlockSettingsAccessor) blockSettings).serverwrecker$getOffsetType();
            offsetData.addProperty("offsetType", offsetType.name());

            blockDesc.add("modelOffset", offsetData);
        }

        return blockDesc;
    }
}
