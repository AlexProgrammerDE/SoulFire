package dev.u9g.minecraftdatagenerator.generators;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.mixin.BlockAccessor;
import dev.u9g.minecraftdatagenerator.util.BlockSettingsAccessor;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.block.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BlocksDataGenerator implements IDataGenerator {

    private static Logger logger = LoggerFactory.getLogger(BlocksDataGenerator.class);

    private static List<Item> getItemsEffectiveForBlock(BlockState blockState) {
        return DGU.getWorld().getRegistryManager().get(RegistryKeys.ITEM).stream()
                .filter(item -> item.getDefaultStack().isSuitableFor(blockState))
                .collect(Collectors.toList());
    }

    private static void populateDropsIfPossible(BlockState blockState, Item firstToolItem, List<ItemStack> outDrops) {
        MinecraftServer minecraftServer = DGU.getCurrentlyRunningServer();
        if (minecraftServer != null) {
            //If we have local world context, we can actually evaluate loot tables and determine actual data
            ServerWorld serverWorld = minecraftServer.getOverworld();
            LootContextParameterSet.Builder lootContextParameterSet = new LootContextParameterSet.Builder(serverWorld)
                    .add(LootContextParameters.BLOCK_STATE, blockState)
                    .add(LootContextParameters.ORIGIN, Vec3d.ZERO)
                    .add(LootContextParameters.TOOL, firstToolItem.getDefaultStack());
            outDrops.addAll(blockState.getDroppedStacks(lootContextParameterSet));
        } else {
            //If we're lacking world context to correctly determine drops, assume that default drop is ItemBlock stack in quantity of 1
            Item itemBlock = blockState.getBlock().asItem();
            if (itemBlock != Items.AIR) {
                outDrops.add(itemBlock.getDefaultStack());
            }
        }
    }

    private static String getPropertyTypeName(Property<?> property) {
        //Explicitly handle default minecraft properties
        if (property instanceof BooleanProperty) {
            return "bool";
        }
        if (property instanceof IntProperty) {
            return "int";
        }
        if (property instanceof EnumProperty) {
            return "enum";
        }

        //Use simple class name as fallback, this code will give something like
        //example_type for ExampleTypeProperty class name
        String rawPropertyName = property.getClass().getSimpleName().replace("Property", "");
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, rawPropertyName);
    }

    private static <T extends Comparable<T>> JsonObject generateStateProperty(Property<T> property) {
        JsonObject propertyObject = new JsonObject();
        Collection<T> propertyValues = property.getValues();

        propertyObject.addProperty("name", property.getName());
        propertyObject.addProperty("type", getPropertyTypeName(property));
        propertyObject.addProperty("num_values", propertyValues.size());

        //Do not add values for vanilla boolean properties, they are known by default
        if (!(property instanceof BooleanProperty)) {
            JsonArray propertyValuesArray = new JsonArray();
            for (T propertyValue : propertyValues) {
                propertyValuesArray.add(property.name(propertyValue));
            }
            propertyObject.add("values", propertyValuesArray);
        }
        return propertyObject;
    }

    @Override
    public String getDataName() {
        return "blocks";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultBlocksArray = new JsonArray();
        Registry<Block> blockRegistry = DGU.getWorld().getRegistryManager().get(RegistryKeys.BLOCK);
        List<MaterialsDataGenerator.MaterialInfo> availableMaterials = MaterialsDataGenerator.getGlobalMaterialInfo();

        blockRegistry.forEach(block -> resultBlocksArray.add(generateBlock(blockRegistry, availableMaterials, block)));
        return resultBlocksArray;
    }

    private static String findMatchingBlockMaterial(BlockState blockState, List<MaterialsDataGenerator.MaterialInfo> materials) {
        List<MaterialsDataGenerator.MaterialInfo> matchingMaterials = materials.stream()
                .filter(material -> material.getPredicate().test(blockState))
                .collect(Collectors.toList());

        if (matchingMaterials.size() > 1) {
            var firstMaterial = matchingMaterials.get(0);
            var otherMaterials = matchingMaterials.subList(1, matchingMaterials.size());

            if (!otherMaterials.stream().allMatch(firstMaterial::includesMaterial)) {
                logger.error("Block {} matches multiple materials: {}", blockState.getBlock(), matchingMaterials);
            }
        }
        if (matchingMaterials.isEmpty()) {
            return "default";
        }
        return matchingMaterials.get(0).getMaterialName();
    }

    public static JsonObject generateBlock(Registry<Block> blockRegistry, List<MaterialsDataGenerator.MaterialInfo> materials, Block block) {
        JsonObject blockDesc = new JsonObject();

        List<BlockState> blockStates = block.getStateManager().getStates();
        BlockState defaultState = block.getDefaultState();
        Identifier registryKey = blockRegistry.getKey(block).orElseThrow().getValue();
        String localizationKey = block.getTranslationKey();
        List<Item> effectiveTools = getItemsEffectiveForBlock(defaultState);

        blockDesc.addProperty("id", blockRegistry.getRawId(block));
        blockDesc.addProperty("name", registryKey.getPath());
        blockDesc.addProperty("displayName", DGU.translateText(localizationKey));

        blockDesc.addProperty("hardness", block.getHardness());
        blockDesc.addProperty("resistance", block.getBlastResistance());
        blockDesc.addProperty("stackSize", block.asItem().getMaxCount());
        blockDesc.addProperty("diggable", block.getHardness() != -1.0f && !(block instanceof AirBlock));
        blockDesc.addProperty("air", defaultState.isAir());
        blockDesc.addProperty("fallingBlock", block instanceof FallingBlock);
        blockDesc.addProperty("replaceable", defaultState.isReplaceable());

        if (defaultState.hasModelOffset()) {
            JsonObject offsetData = new JsonObject();

            offsetData.addProperty("maxHorizontalOffset", block.getMaxHorizontalModelOffset());
            offsetData.addProperty("verticalModelOffsetMultiplier", block.getVerticalModelOffsetMultiplier());

            AbstractBlock.Settings blockSettings = ((BlockAccessor) block).settings();
            AbstractBlock.OffsetType offsetType = ((BlockSettingsAccessor) blockSettings).getOffsetType();
            offsetData.addProperty("offsetType", offsetType.name());

            blockDesc.add("modelOffset", offsetData);
        }

//        JsonObject effTools = new JsonObject();
//        effectiveTools.forEach(item -> effTools.addProperty(
//                String.valueOf(Registry.ITEM.getRawId(item)), // key
//                item.getMiningSpeedMultiplier(item.getDefaultStack(), defaultState) // value
//        ));
//        blockDesc.add("effectiveTools", effTools);
        blockDesc.addProperty("material", findMatchingBlockMaterial(defaultState, materials));

        blockDesc.addProperty("transparent", !defaultState.isOpaque());
        blockDesc.addProperty("emitLight", defaultState.getLuminance());
        blockDesc.addProperty("filterLight", defaultState.getOpacity(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));

        blockDesc.addProperty("defaultState", Block.getRawIdFromState(defaultState));
        blockDesc.addProperty("minStateId", Block.getRawIdFromState(blockStates.get(0)));
        blockDesc.addProperty("maxStateId", Block.getRawIdFromState(blockStates.get(blockStates.size() - 1)));

        JsonArray stateProperties = new JsonArray();
        for (Property<?> property : block.getStateManager().getProperties()) {
            stateProperties.add(generateStateProperty(property));
        }
        blockDesc.add("states", stateProperties);

        //Only add harvest tools if tool is required for harvesting this block
        if (defaultState.isToolRequired()) {
            JsonObject effectiveToolsObject = new JsonObject();
            for (Item effectiveItem : effectiveTools) {
                effectiveToolsObject.addProperty(Integer.toString(Item.getRawId(effectiveItem)), true);
            }
            blockDesc.add("harvestTools", effectiveToolsObject);
        }

        List<ItemStack> actualBlockDrops = new ArrayList<>();
        populateDropsIfPossible(defaultState, effectiveTools.isEmpty() ? Items.AIR : effectiveTools.get(0), actualBlockDrops);

        JsonArray dropsArray = new JsonArray();
        for (ItemStack dropStack : actualBlockDrops) {
            dropsArray.add(Item.getRawId(dropStack.getItem()));
        }
        blockDesc.add("drops", dropsArray);

        VoxelShape blockCollisionShape = defaultState.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
        blockDesc.addProperty("boundingBox", blockCollisionShape.isEmpty() ? "empty" : "block");

        return blockDesc;
    }
}
