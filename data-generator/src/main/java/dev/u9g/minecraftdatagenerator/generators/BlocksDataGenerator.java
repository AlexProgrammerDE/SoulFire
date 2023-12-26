package dev.u9g.minecraftdatagenerator.generators;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.generators.MaterialsDataGenerator.MaterialInfo;
import dev.u9g.minecraftdatagenerator.mixin.BlockAccessor;
import dev.u9g.minecraftdatagenerator.util.BlockSettingsAccessor;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BlocksDataGenerator implements IDataGenerator {

    private static final Logger logger = LoggerFactory.getLogger(BlocksDataGenerator.class);

    private static List<Item> getItemsEffectiveForBlock(BlockState blockState) {
        return DGU.getWorld().registryAccess().registryOrThrow(Registries.ITEM).stream()
                .filter(item -> item.getDefaultInstance().isCorrectToolForDrops(blockState))
                .collect(Collectors.toList());
    }

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

    private static String getPropertyTypeName(Property<?> property) {
        //Explicitly handle default minecraft properties
        if (property instanceof BooleanProperty) {
            return "bool";
        }
        if (property instanceof IntegerProperty) {
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
        Collection<T> propertyValues = property.getPossibleValues();

        propertyObject.addProperty("name", property.getName());
        propertyObject.addProperty("type", getPropertyTypeName(property));
        propertyObject.addProperty("num_values", propertyValues.size());

        //Do not add values for vanilla boolean properties, they are known by default
        if (!(property instanceof BooleanProperty)) {
            JsonArray propertyValuesArray = new JsonArray();
            for (T propertyValue : propertyValues) {
                propertyValuesArray.add(property.getName(propertyValue));
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
        Registry<Block> blockRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.BLOCK);
        List<MaterialInfo> availableMaterials = MaterialsDataGenerator.getGlobalMaterialInfo();

        blockRegistry.forEach(block -> resultBlocksArray.add(generateBlock(blockRegistry, availableMaterials, block)));
        return resultBlocksArray;
    }

    private static String findMatchingBlockMaterial(BlockState blockState, List<MaterialInfo> materials) {
        List<MaterialInfo> matchingMaterials = materials.stream()
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

    public static JsonObject generateBlock(Registry<Block> blockRegistry, List<MaterialInfo> materials, Block block) {
        JsonObject blockDesc = new JsonObject();

        List<BlockState> blockStates = block.getStateDefinition().getPossibleStates();
        BlockState defaultState = block.defaultBlockState();
        ResourceLocation registryKey = blockRegistry.getResourceKey(block).orElseThrow().location();
        String localizationKey = block.getDescriptionId();
        List<Item> effectiveTools = getItemsEffectiveForBlock(defaultState);

        blockDesc.addProperty("id", blockRegistry.getId(block));
        blockDesc.addProperty("name", registryKey.getPath());
        blockDesc.addProperty("displayName", DGU.translateText(localizationKey));

        blockDesc.addProperty("hardness", block.defaultDestroyTime());
        blockDesc.addProperty("resistance", block.getExplosionResistance());
        blockDesc.addProperty("stackSize", block.asItem().getMaxStackSize());
        blockDesc.addProperty("diggable", block.defaultDestroyTime() != -1.0f && !(block instanceof AirBlock));
        blockDesc.addProperty("air", defaultState.isAir());
        blockDesc.addProperty("fallingBlock", block instanceof FallingBlock);
        blockDesc.addProperty("replaceable", defaultState.canBeReplaced());

        if (defaultState.hasOffsetFunction()) {
            JsonObject offsetData = new JsonObject();

            offsetData.addProperty("maxHorizontalOffset", block.getMaxHorizontalOffset());
            offsetData.addProperty("verticalModelOffsetMultiplier", block.getMaxVerticalOffset());

            BlockBehaviour.Properties blockSettings = ((BlockAccessor) block).properties();
            BlockBehaviour.OffsetType offsetType = ((BlockSettingsAccessor) blockSettings).getOffsetType();
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

        blockDesc.addProperty("transparent", !defaultState.canOcclude());
        blockDesc.addProperty("emitLight", defaultState.getLightEmission());
        blockDesc.addProperty("filterLight", defaultState.getLightBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));

        blockDesc.addProperty("defaultState", Block.getId(defaultState));
        blockDesc.addProperty("minStateId", Block.getId(blockStates.get(0)));
        blockDesc.addProperty("maxStateId", Block.getId(blockStates.get(blockStates.size() - 1)));

        JsonArray stateProperties = new JsonArray();
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            stateProperties.add(generateStateProperty(property));
        }
        blockDesc.add("states", stateProperties);

        //Only add harvest tools if tool is required for harvesting this block
        if (defaultState.requiresCorrectToolForDrops()) {
            JsonObject effectiveToolsObject = new JsonObject();
            for (Item effectiveItem : effectiveTools) {
                effectiveToolsObject.addProperty(Integer.toString(Item.getId(effectiveItem)), true);
            }
            blockDesc.add("harvestTools", effectiveToolsObject);
        }

        List<ItemStack> actualBlockDrops = new ArrayList<>();
        populateDropsIfPossible(defaultState, effectiveTools.isEmpty() ? Items.AIR : effectiveTools.get(0), actualBlockDrops);

        JsonArray dropsArray = new JsonArray();
        for (ItemStack dropStack : actualBlockDrops) {
            dropsArray.add(Item.getId(dropStack.getItem()));
        }
        blockDesc.add("drops", dropsArray);

        VoxelShape blockCollisionShape = defaultState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        blockDesc.addProperty("boundingBox", blockCollisionShape.isEmpty() ? "empty" : "block");

        return blockDesc;
    }
}
