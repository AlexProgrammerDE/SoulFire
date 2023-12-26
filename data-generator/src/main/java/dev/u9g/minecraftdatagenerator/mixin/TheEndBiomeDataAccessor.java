package dev.u9g.minecraftdatagenerator.mixin;

import net.fabricmc.fabric.impl.biome.TheEndBiomeData;
import net.fabricmc.fabric.impl.biome.WeightedPicker;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;


@Mixin(TheEndBiomeData.class)
public interface TheEndBiomeDataAccessor {
    @Accessor(value = "END_BIOMES_MAP", remap = false)
    static Map<ResourceKey<Biome>, WeightedPicker<ResourceKey<Biome>>> END_BIOMES_MAP() {
        throw new IllegalStateException("Should never be called.");
    }
    @Accessor(value = "END_MIDLANDS_MAP", remap = false)
    static Map<ResourceKey<Biome>, WeightedPicker<ResourceKey<Biome>>> END_MIDLANDS_MAP() {
        throw new IllegalStateException("Should never be called.");
    }
    @Accessor(value = "END_BARRENS_MAP", remap = false)
    static Map<ResourceKey<Biome>, WeightedPicker<ResourceKey<Biome>>> END_BARRENS_MAP() {
        throw new IllegalStateException("Should never be called.");
    }
}
