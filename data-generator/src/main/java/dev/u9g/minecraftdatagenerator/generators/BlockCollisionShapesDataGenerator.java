package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockCollisionShapesDataGenerator implements IDataGenerator {

    private static class BlockShapesCache {
        public Map<VoxelShape, Integer> uniqueBlockShapes = new HashMap<>();
        public Map<Block, List<Integer>> blockCollisionShapes = new HashMap<>();
        private int lastCollisionShapeId = 0;

        public void processBlock(Block block) {
            List<BlockState> blockStates = block.getStateDefinition().getPossibleStates();
            List<Integer> blockCollisionShapes = new ArrayList<>();

            for (BlockState blockState : blockStates) {
                VoxelShape blockShape = blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);

                // Replace block offset
                Vec3 blockShapeCenter = blockState.getOffset(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                Vec3 inverseBlockShapeCenter = blockShapeCenter.reverse();
                blockShape = blockShape.move(inverseBlockShapeCenter.x, inverseBlockShapeCenter.y, inverseBlockShapeCenter.z);

                Integer blockShapeIndex = uniqueBlockShapes.get(blockShape);

                if (blockShapeIndex == null) {
                    blockShapeIndex = lastCollisionShapeId++;
                    uniqueBlockShapes.put(blockShape, blockShapeIndex);
                }
                blockCollisionShapes.add(blockShapeIndex);
            }

            this.blockCollisionShapes.put(block, blockCollisionShapes);
        }

        public JsonObject dumpBlockShapeIndices(Registry<Block> blockRegistry) {
            JsonObject resultObject = new JsonObject();

            for (var entry : blockCollisionShapes.entrySet()) {
                List<Integer> blockCollisions = entry.getValue();
                long distinctShapesCount = blockCollisions.stream().distinct().count();
                JsonElement blockCollision;
                if (distinctShapesCount == 1L) {
                    blockCollision = new JsonPrimitive(blockCollisions.get(0));
                } else {
                    blockCollision = new JsonArray();
                    for (int collisionId : blockCollisions) {
                        ((JsonArray) blockCollision).add(collisionId);
                    }
                }

                ResourceLocation registryKey = blockRegistry.getResourceKey(entry.getKey()).orElseThrow().location();
                resultObject.add(registryKey.getPath(), blockCollision);
            }

            return resultObject;
        }

        public JsonObject dumpShapesObject() {
            JsonObject shapesObject = new JsonObject();

            for (var entry : uniqueBlockShapes.entrySet()) {
                JsonArray boxesArray = new JsonArray();
                entry.getKey().forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
                    JsonArray oneBoxJsonArray = new JsonArray();

                    oneBoxJsonArray.add(x1);
                    oneBoxJsonArray.add(y1);
                    oneBoxJsonArray.add(z1);

                    oneBoxJsonArray.add(x2);
                    oneBoxJsonArray.add(y2);
                    oneBoxJsonArray.add(z2);

                    boxesArray.add(oneBoxJsonArray);
                });
                shapesObject.add(Integer.toString(entry.getValue()), boxesArray);
            }
            return shapesObject;
        }
    }

    @Override
    public String getDataName() {
        return "blockCollisionShapes";
    }

    @Override
    public JsonObject generateDataJson() {
        Registry<Block> blockRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.BLOCK);
        BlockShapesCache blockShapesCache = new BlockShapesCache();

        blockRegistry.forEach(blockShapesCache::processBlock);

        JsonObject resultObject = new JsonObject();

        resultObject.add("blocks", blockShapesCache.dumpBlockShapeIndices(blockRegistry));
        resultObject.add("shapes", blockShapesCache.dumpShapesObject());

        return resultObject;
    }
}
