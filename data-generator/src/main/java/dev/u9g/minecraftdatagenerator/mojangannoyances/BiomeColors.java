package dev.u9g.minecraftdatagenerator.mojangannoyances;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;

@Environment(EnvType.CLIENT)
public class BiomeColors {
    public static final ColorResolver GRASS_COLOR = Biome::getGrassColorAt;
    public static final ColorResolver FOLIAGE_COLOR = (biome, x, z) -> {
        return biome.getFoliageColor();
    };
    public static final ColorResolver WATER_COLOR = (biome, x, z) -> {
        return biome.getWaterColor();
    };

    public BiomeColors() {
    }

    private static int getColor(BlockRenderView world, BlockPos pos, ColorResolver resolver) {
        return world.getColor(pos, resolver);
    }

    public static int getGrassColor(BlockRenderView world, BlockPos pos) {
        return getColor(world, pos, GRASS_COLOR);
    }

    public static int getFoliageColor(BlockRenderView world, BlockPos pos) {
        return getColor(world, pos, FOLIAGE_COLOR);
    }

    public static int getWaterColor(BlockRenderView world, BlockPos pos) {
        return getColor(world, pos, WATER_COLOR);
    }
}

