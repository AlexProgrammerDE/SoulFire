package dev.u9g.minecraftdatagenerator.mojangannoyances;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;

@Environment(EnvType.CLIENT)
public class BiomeColors {
    public static final ColorResolver GRASS_COLOR = Biome::getGrassColor;
    public static final ColorResolver FOLIAGE_COLOR = (biome, x, z) -> {
        return biome.getFoliageColor();
    };
    public static final ColorResolver WATER_COLOR = (biome, x, z) -> {
        return biome.getWaterColor();
    };

    public BiomeColors() {
    }

    private static int getColor(BlockAndTintGetter world, BlockPos pos, ColorResolver resolver) {
        return world.getBlockTint(pos, resolver);
    }

    public static int getGrassColor(BlockAndTintGetter world, BlockPos pos) {
        return getColor(world, pos, GRASS_COLOR);
    }

    public static int getFoliageColor(BlockAndTintGetter world, BlockPos pos) {
        return getColor(world, pos, FOLIAGE_COLOR);
    }

    public static int getWaterColor(BlockAndTintGetter world, BlockPos pos) {
        return getColor(world, pos, WATER_COLOR);
    }
}

