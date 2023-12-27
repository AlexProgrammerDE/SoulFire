package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.mixin.BlockAccessor;
import dev.u9g.minecraftdatagenerator.util.BlockSettingsAccessor;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BlocksDataGenerator implements IDataGenerator {
    private static void populateDropsIfPossible(BlockState blockState, Item firstToolItem, List<ItemStack> outDrops) {
        MinecraftServer minecraftServer = DGU.getCurrentlyRunningServer();
        if (minecraftServer != null) {
            //If we have local world context, we can actually evaluate loot tables and determine actual data
            ServerLevel serverWorld = minecraftServer.overworld();
            LootParams.Builder lootContextParameterSet = new LootParams.Builder(serverWorld)
                    .withParameter(LootContextParams.BLOCK_STATE, blockState)
                    .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                    .withParameter(LootContextParams.TOOL, firstToolItem.getDefaultInstance());
            outDrops.addAll(blockState.getDrops(lootContextParameterSet));
        } else {
            //If we're lacking world context to correctly determine drops, assume that default drop is ItemBlock stack in quantity of 1
            Item itemBlock = blockState.getBlock().asItem();
            if (itemBlock != Items.AIR) {
                outDrops.add(itemBlock.getDefaultInstance());
            }
        }
    }

    @Override
    public String getDataName() {
        return "blocks";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultBlocksArray = new JsonArray();
        Registry<Block> blockRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.BLOCK);

        blockRegistry.forEach(block -> resultBlocksArray.add(generateBlock(blockRegistry, block)));
        return resultBlocksArray;
    }

    public static JsonObject generateBlock(Registry<Block> blockRegistry, Block block) {
        JsonObject blockDesc = new JsonObject();

        List<BlockState> blockStates = block.getStateDefinition().getPossibleStates();
        BlockState defaultState = block.defaultBlockState();
        ResourceLocation registryKey = blockRegistry.getResourceKey(block).orElseThrow().location();
        String localizationKey = block.getDescriptionId();

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
            JsonObject offsetData = new JsonObject();

            offsetData.addProperty("maxHorizontalOffset", block.getMaxHorizontalOffset());
            offsetData.addProperty("verticalModelOffsetMultiplier", block.getMaxVerticalOffset());

            BlockBehaviour.Properties blockSettings = ((BlockAccessor) block).properties();
            BlockBehaviour.OffsetType offsetType = ((BlockSettingsAccessor) blockSettings).serverwrecker$getOffsetType();
            offsetData.addProperty("offsetType", offsetType.name());

            blockDesc.add("modelOffset", offsetData);
        }

        return blockDesc;
    }
}
