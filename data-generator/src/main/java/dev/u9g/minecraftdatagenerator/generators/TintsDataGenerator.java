package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.mojangannoyances.BlockColors;
import dev.u9g.minecraftdatagenerator.util.DGU;
import dev.u9g.minecraftdatagenerator.util.EmptyRenderBlockView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TintsDataGenerator implements IDataGenerator {

    public static class BiomeTintColors {
        Map<Integer, List<Biome>> grassColoursMap = new HashMap<>();
        Map<Integer, List<Biome>> foliageColoursMap = new HashMap<>();
        Map<Integer, List<Biome>> waterColourMap = new HashMap<>();
    }

    public static BiomeTintColors generateBiomeTintColors(Registry<Biome> biomeRegistry) {
        BiomeTintColors colors = new BiomeTintColors();

        biomeRegistry.forEach(biome -> {
            int biomeGrassColor = biome.getGrassColor(0.0, 0.0);
            int biomeFoliageColor = biome.getFoliageColor();
            int biomeWaterColor = biome.getWaterColor();

            colors.grassColoursMap.computeIfAbsent(biomeGrassColor, k -> new ArrayList<>()).add(biome);
            colors.foliageColoursMap.computeIfAbsent(biomeFoliageColor, k -> new ArrayList<>()).add(biome);
            colors.waterColourMap.computeIfAbsent(biomeWaterColor, k -> new ArrayList<>()).add(biome);
        });
        return colors;
    }

    public static Map<Integer, Integer> generateRedstoneTintColors() {
        Map<Integer, Integer> resultColors = new HashMap<>();

        for (int redstoneLevel : RedStoneWireBlock.POWER.getPossibleValues()) {
            int color = RedStoneWireBlock.getColorForPower(redstoneLevel);
            resultColors.put(redstoneLevel, color);
        }
        return resultColors;
    }

    private static int getBlockColor(Block block, BlockColors blockColors) {
        return blockColors.getColor(block.defaultBlockState(), EmptyRenderBlockView.INSTANCE, BlockPos.ZERO, 0xFFFFFF);
    }

    public static Map<Block, Integer> generateConstantTintColors() {
        Map<Block, Integer> resultColors = new HashMap<>();
        BlockColors blockColors = BlockColors.create();

        resultColors.put(Blocks.BIRCH_LEAVES, FoliageColor.getBirchColor());
        resultColors.put(Blocks.SPRUCE_LEAVES, FoliageColor.getEvergreenColor());

        resultColors.put(Blocks.LILY_PAD, getBlockColor(Blocks.LILY_PAD, blockColors));
        resultColors.put(Blocks.ATTACHED_MELON_STEM, getBlockColor(Blocks.ATTACHED_MELON_STEM, blockColors));
        resultColors.put(Blocks.ATTACHED_PUMPKIN_STEM, getBlockColor(Blocks.ATTACHED_PUMPKIN_STEM, blockColors));

        //not really constant, depend on the block age, but kinda have to be handled since textures are literally white without them
        resultColors.put(Blocks.MELON_STEM, getBlockColor(Blocks.MELON_STEM, blockColors));
        resultColors.put(Blocks.PUMPKIN_STEM, getBlockColor(Blocks.PUMPKIN_STEM, blockColors));

        return resultColors;
    }

    private static JsonObject encodeBiomeColorMap(Registry<Biome> biomeRegistry, Map<Integer, List<Biome>> colorsMap) {
        JsonArray resultColorsArray = new JsonArray();
        for (var entry : colorsMap.entrySet()) {
            JsonObject entryObject = new JsonObject();

            JsonArray keysArray = new JsonArray();
            for (Biome biome : entry.getValue()) {
                ResourceLocation registryKey = biomeRegistry.getResourceKey(biome).orElseThrow().location();
                keysArray.add(registryKey.getPath());
            }

            entryObject.add("keys", keysArray);
            entryObject.addProperty("color", entry.getKey());
            resultColorsArray.add(entryObject);
        }

        JsonObject resultObject = new JsonObject();
        resultObject.add("data", resultColorsArray);
        return resultObject;
    }

    private static JsonObject encodeRedstoneColorMap(Map<Integer, Integer> colorsMap) {
        JsonArray resultColorsArray = new JsonArray();
        for (var entry : colorsMap.entrySet()) {
            JsonObject entryObject = new JsonObject();

            JsonArray keysArray = new JsonArray();
            keysArray.add(entry.getKey());

            entryObject.add("keys", keysArray);
            entryObject.addProperty("color", entry.getValue());
            resultColorsArray.add(entryObject);
        }

        JsonObject resultObject = new JsonObject();
        resultObject.add("data", resultColorsArray);
        return resultObject;
    }

    private static JsonObject encodeBlocksColorMap(Registry<Block> blockRegistry, Map<Block, Integer> colorsMap) {
        JsonArray resultColorsArray = new JsonArray();
        for (var entry : colorsMap.entrySet()) {
            JsonObject entryObject = new JsonObject();

            JsonArray keysArray = new JsonArray();
            ResourceLocation registryKey = blockRegistry.getResourceKey(entry.getKey()).orElseThrow().location();
            keysArray.add(registryKey.getPath());

            entryObject.add("keys", keysArray);
            entryObject.addProperty("color", entry.getValue());
            resultColorsArray.add(entryObject);
        }

        JsonObject resultObject = new JsonObject();
        resultObject.add("data", resultColorsArray);
        return resultObject;
    }

    @Override
    public String getDataName() {
        return "tints";
    }

    @Override
    public JsonObject generateDataJson() {
        RegistryAccess registryManager = DGU.getWorld().registryAccess();
        Registry<Biome> biomeRegistry = registryManager.registryOrThrow(Registries.BIOME);
        Registry<Block> blockRegistry = registryManager.registryOrThrow(Registries.BLOCK);

        BiomeTintColors biomeTintColors = generateBiomeTintColors(biomeRegistry);
        Map<Integer, Integer> redstoneColors = generateRedstoneTintColors();
        Map<Block, Integer> constantTintColors = generateConstantTintColors();

        JsonObject resultObject = new JsonObject();

        resultObject.add("grass", encodeBiomeColorMap(biomeRegistry, biomeTintColors.grassColoursMap));
        resultObject.add("foliage", encodeBiomeColorMap(biomeRegistry, biomeTintColors.foliageColoursMap));
        resultObject.add("water", encodeBiomeColorMap(biomeRegistry, biomeTintColors.waterColourMap));

        resultObject.add("redstone", encodeRedstoneColorMap(redstoneColors));
        resultObject.add("constant", encodeBlocksColorMap(blockRegistry, constantTintColors));

        return resultObject;
    }
}
