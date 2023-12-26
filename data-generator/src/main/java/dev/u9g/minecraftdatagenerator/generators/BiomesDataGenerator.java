package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.mixin.TheEndBiomeDataAccessor;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.fabricmc.fabric.api.biome.v1.NetherBiomes;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.HashSet;
import java.util.Set;

public class BiomesDataGenerator implements IDataGenerator {
    private static final Set<ResourceKey<Biome>> END_BIOMES = new HashSet<>();

    private static String guessBiomeDimensionFromCategory(Biome biome, String biomeName) {
        var key = DGU.getWorld().registryAccess().registryOrThrow(Registries.BIOME).getResourceKey(biome).orElseThrow();
        if (NetherBiomes.canGenerateInNether(key)) {
            return "nether";
        } else if (END_BIOMES.contains(key) || biomeName.startsWith("end_")) {
            return "end";
        }
        return "overworld";
    }

    private static String guessCategoryBasedOnName(String name, String dimension) {
        if (dimension.equals("nether")) {
            return "nether";
        } else if (dimension.equals("end")) {
            return "the_end";
        }

        if (name.contains("end")) {
            System.out.println();
        }

        if (name.contains("hills")) {
            return "extreme_hills";
        } else if (name.contains("ocean")) {
            return "ocean";
        } else if (name.contains("plains")) {
            return "plains";
        } else if (name.contains("ice") || name.contains("frozen")) {
            return "ice";
        } else if (name.contains("jungle")) {
            return "jungle";
        } else if (name.contains("desert")) {
            return "desert";
        } else if (name.contains("forest") || name.contains("grove")) {
            return "forest";
        } else if (name.contains("taiga")) {
            return "taiga";
        } else if (name.contains("swamp")) {
            return "swamp";
        } else if (name.contains("river")) {
            return "river";
        } else if (name.equals("the_end")) {
            return "the_end";
        } else if (name.contains("mushroom")) {
            return "mushroom";
        } else if (name.contains("beach") || name.equals("stony_shore")) {
            return "beach";
        } else if (name.contains("savanna")) {
            return "savanna";
        } else if (name.contains("badlands")) {
            return "mesa";
        } else if (name.contains("peaks") || name.equals("snowy_slopes") || name.equals("meadow")) {
            return "mountain";
        } else if (name.equals("the_void")) {
            return "none";
        } else if (name.contains("cave") || name.equals("deep_dark")) {
            return "underground";
        } else {
            System.out.println("Unable to find biome category for biome with name: '"+name+"'");
            return "none";
        }
    }

    private static int getBiomeColorFor(String biomeName) {
        if (biomeName.equals("the_void")) return 0;
        else if (biomeName.equals("plains")) return 9286496;
        else if (biomeName.equals("sunflower_plains")) return 11918216;
        else if (biomeName.equals("snowy_plains")) return 16777215;
        else if (biomeName.equals("ice_spikes")) return 11853020;
        else if (biomeName.equals("desert")) return 16421912;
        else if (biomeName.equals("swamp")) return 522674;
        else if (biomeName.equals("forest")) return 353825;
        else if (biomeName.equals("flower_forest")) return 2985545;
        else if (biomeName.equals("birch_forest")) return 3175492;
        else if (biomeName.equals("dark_forest")) return 4215066;
        else if (biomeName.equals("old_growth_birch_forest")) return 5807212;
        else if (biomeName.equals("old_growth_pine_taiga")) return 5858897;
        else if (biomeName.equals("old_growth_spruce_taiga")) return 8490617;
        else if (biomeName.equals("taiga")) return 747097;
        else if (biomeName.equals("snowy_taiga")) return 3233098;
        else if (biomeName.equals("savanna")) return 12431967;
        else if (biomeName.equals("savanna_plateau")) return 10984804;
        else if (biomeName.equals("windswept_hills")) return 6316128;
        else if (biomeName.equals("windswept_gravelly_hills")) return 8947848;
        else if (biomeName.equals("windswept_forest")) return 2250012;
        else if (biomeName.equals("windswept_savanna")) return 15063687;
        else if (biomeName.equals("jungle")) return 5470985;
        else if (biomeName.equals("sparse_jungle")) return 6458135;
        else if (biomeName.equals("bamboo_jungle")) return 7769620;
        else if (biomeName.equals("badlands")) return 14238997;
        else if (biomeName.equals("eroded_badlands")) return 16739645;
        else if (biomeName.equals("wooded_badlands")) return 11573093;
        else if (biomeName.equals("meadow")) return 9217136;
        else if (biomeName.equals("grove")) return 14675173;
        else if (biomeName.equals("snowy_slopes")) return 14348785;
        else if (biomeName.equals("frozen_peaks")) return 15399931;
        else if (biomeName.equals("jagged_peaks")) return 14937325;
        else if (biomeName.equals("stony_peaks")) return 13750737;
        else if (biomeName.equals("river")) return 255;
        else if (biomeName.equals("frozen_river")) return 10526975;
        else if (biomeName.equals("beach")) return 16440917;
        else if (biomeName.equals("snowy_beach")) return 16445632;
        else if (biomeName.equals("stony_shore")) return 10658436;
        else if (biomeName.equals("warm_ocean")) return 172;
        else if (biomeName.equals("lukewarm_ocean")) return 144;
        else if (biomeName.equals("deep_lukewarm_ocean")) return 64;
        else if (biomeName.equals("ocean")) return 112;
        else if (biomeName.equals("deep_ocean")) return 48;
        else if (biomeName.equals("cold_ocean")) return 2105456;
        else if (biomeName.equals("deep_cold_ocean")) return 2105400;
        else if (biomeName.equals("frozen_ocean")) return 7368918;
        else if (biomeName.equals("deep_frozen_ocean")) return 4210832;
        else if (biomeName.equals("mushroom_fields")) return 16711935;
        else if (biomeName.equals("dripstone_caves")) return 12690831;
        else if (biomeName.equals("lush_caves")) return 14652980;
        else if (biomeName.equals("nether_wastes")) return 12532539;
        else if (biomeName.equals("warped_forest")) return 4821115;
        else if (biomeName.equals("crimson_forest")) return 14485512;
        else if (biomeName.equals("soul_sand_valley")) return 6174768;
        else if (biomeName.equals("basalt_deltas")) return 4208182;
        else if (biomeName.equals("the_end")) return 8421631;
        else if (biomeName.equals("end_highlands")) return 12828041;
        else if (biomeName.equals("end_midlands")) return 15464630;
        else if (biomeName.equals("small_end_islands")) return 42;
        else if (biomeName.equals("end_barrens")) return 9474162;
        System.out.println("Don't know the color of biome: '"+biomeName+"'");
        return 0;
    }

    public static JsonObject generateBiomeInfo(Registry<Biome> registry, Biome biome) {
        JsonObject biomeDesc = new JsonObject();
        ResourceLocation registryKey = registry.getResourceKey(biome).orElseThrow().location();
        String localizationKey = String.format("biome.%s.%s", registryKey.getNamespace(), registryKey.getPath());
        String name = registryKey.getPath();
        biomeDesc.addProperty("id", registry.getId(biome));
        biomeDesc.addProperty("name", name);
        String dimension = guessBiomeDimensionFromCategory(biome, name);
        biomeDesc.addProperty("category", guessCategoryBasedOnName(name, dimension));
        biomeDesc.addProperty("temperature", biome.getBaseTemperature());
        //biomeDesc.addProperty("precipitation", biome.getPrecipitation().getName());// - removed in 1.19.4
        biomeDesc.addProperty("has_precipitation", biome.hasPrecipitation());
        //biomeDesc.addProperty("depth", biome.getDepth()); - Doesn't exist anymore in minecraft source
        biomeDesc.addProperty("dimension", dimension);
        biomeDesc.addProperty("displayName", DGU.translateText(localizationKey));
        biomeDesc.addProperty("color", getBiomeColorFor(registryKey.getPath()));
        //biomeDesc.addProperty("rainfall", biome.getDownfall());// - removed in 1.19.4

        return biomeDesc;
    }

    @Override
    public String getDataName() {
        return "biomes";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray biomesArray = new JsonArray();
        RegistryAccess registryManager = DGU.getWorld().registryAccess();
        Registry<Biome> biomeRegistry = registryManager.registryOrThrow(Registries.BIOME);

        biomeRegistry.stream()
                .map(biome -> generateBiomeInfo(biomeRegistry, biome))
                .forEach(biomesArray::add);
        return biomesArray;
    }

    static {
        END_BIOMES.addAll(TheEndBiomeDataAccessor.END_BIOMES_MAP().keySet());
        END_BIOMES.addAll(TheEndBiomeDataAccessor.END_BARRENS_MAP().keySet());
        END_BIOMES.addAll(TheEndBiomeDataAccessor.END_MIDLANDS_MAP().keySet());
    }
}
