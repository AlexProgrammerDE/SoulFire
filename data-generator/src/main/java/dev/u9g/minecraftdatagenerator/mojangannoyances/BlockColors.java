package dev.u9g.minecraftdatagenerator.mojangannoyances;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.core.BlockPos;
import net.minecraft.core.IdMapper;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class BlockColors {
    private static final int NO_COLOR = -1;
    private final IdMapper<BlockColorProvider> providers = new IdMapper(32);
    private final Map<Block, Set<Property<?>>> properties = Maps.newHashMap();

    public BlockColors() {
    }

    public static BlockColors create() {
        BlockColors blockColors = new BlockColors();
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            return world != null && pos != null ? BiomeColors.getGrassColor(world, state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos) : -1;
        }, Blocks.LARGE_FERN, Blocks.TALL_GRASS);
        blockColors.registerColorProperty(DoublePlantBlock.HALF, Blocks.LARGE_FERN, Blocks.TALL_GRASS);
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            return world != null && pos != null ? BiomeColors.getGrassColor(world, pos) : GrassColor.get(0.5, 1.0);
        }, Blocks.GRASS_BLOCK, Blocks.FERN, Blocks.GRASS, Blocks.POTTED_FERN);
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            return FoliageColor.getEvergreenColor();
        }, Blocks.SPRUCE_LEAVES);
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            return FoliageColor.getBirchColor();
        }, Blocks.BIRCH_LEAVES);
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            return world != null && pos != null ? BiomeColors.getFoliageColor(world, pos) : FoliageColor.getDefaultColor();
        }, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.VINE, Blocks.MANGROVE_LEAVES);
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            return world != null && pos != null ? BiomeColors.getWaterColor(world, pos) : -1;
        }, Blocks.WATER, Blocks.BUBBLE_COLUMN, Blocks.WATER_CAULDRON);
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            return RedStoneWireBlock.getColorForPower(state.getValue(RedStoneWireBlock.POWER));
        }, Blocks.REDSTONE_WIRE);
        blockColors.registerColorProperty(RedStoneWireBlock.POWER, Blocks.REDSTONE_WIRE);
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            return world != null && pos != null ? BiomeColors.getGrassColor(world, pos) : -1;
        }, Blocks.SUGAR_CANE);
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            return 14731036;
        }, Blocks.ATTACHED_MELON_STEM, Blocks.ATTACHED_PUMPKIN_STEM);
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            int i = state.getValue(StemBlock.AGE);
            int j = i * 32;
            int k = 255 - i * 8;
            int l = i * 4;
            return j << 16 | k << 8 | l;
        }, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);
        blockColors.registerColorProperty(StemBlock.AGE, Blocks.MELON_STEM, Blocks.PUMPKIN_STEM);
        blockColors.registerColorProvider((state, world, pos, tintIndex) -> {
            return world != null && pos != null ? 2129968 : 7455580;
        }, Blocks.LILY_PAD);
        return blockColors;
    }

    public int getParticleColor(BlockState state, Level world, BlockPos pos) {
        BlockColorProvider blockColorProvider = this.providers.byId(DGU.getWorld().registryAccess().registryOrThrow(Registries.BLOCK).getId(state.getBlock()));
        if (blockColorProvider != null) {
            return blockColorProvider.getColor(state, null, null, 0);
        } else {
            MapColor mapColor = state.getMapColor(world, pos);
            return mapColor != null ? mapColor.col : -1;
        }
    }

    public int getColor(BlockState state, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos, int tintIndex) {
        BlockColorProvider blockColorProvider = this.providers.byId(DGU.getWorld().registryAccess().registryOrThrow(Registries.BLOCK).getId(state.getBlock()));
        return blockColorProvider == null ? -1 : blockColorProvider.getColor(state, world, pos, tintIndex);
    }

    public void registerColorProvider(BlockColorProvider provider, Block... blocks) {
        Block[] var3 = blocks;
        int var4 = blocks.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            Block block = var3[var5];
            this.providers.addMapping(provider, DGU.getWorld().registryAccess().registryOrThrow(Registries.BLOCK).getId(block));
        }

    }

    private void registerColorProperties(Set<Property<?>> properties, Block... blocks) {
        Block[] var3 = blocks;
        int var4 = blocks.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            Block block = var3[var5];
            this.properties.put(block, properties);
        }

    }

    private void registerColorProperty(Property<?> property, Block... blocks) {
        this.registerColorProperties(ImmutableSet.of(property), blocks);
    }

    public Set<Property<?>> getProperties(Block block) {
        return this.properties.getOrDefault(block, ImmutableSet.of());
    }
}
