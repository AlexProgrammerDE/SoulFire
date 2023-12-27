package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.pistonmaster.serverwrecker.generator.util.DGU;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockCollisionShapesDataGenerator implements IDataGenerator {

    private static class BlockShapesCache {
        public final Map<VoxelShape, Integer> uniqueBlockShapes = new HashMap<>();
        public final Map<Block, List<Integer>> blockCollisionShapes = new HashMap<>();
        private int lastCollisionShapeId = 0;

        public void processBlock(Block block) {
            List<BlockState> blockStates = block.getStateDefinition().getPossibleStates();
            List<Integer> blockCollisionShapes = new ArrayList<>();

            for (var blockState : blockStates) {
                var blockShape = blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);

                // Replace block offset
                var blockShapeCenter = blockState.getOffset(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                var inverseBlockShapeCenter = blockShapeCenter.reverse();
                blockShape = blockShape.move(inverseBlockShapeCenter.x, inverseBlockShapeCenter.y, inverseBlockShapeCenter.z);

                var blockShapeIndex = uniqueBlockShapes.get(blockShape);

                if (blockShapeIndex == null) {
                    blockShapeIndex = lastCollisionShapeId++;
                    uniqueBlockShapes.put(blockShape, blockShapeIndex);
                }
                blockCollisionShapes.add(blockShapeIndex);
            }

            this.blockCollisionShapes.put(block, blockCollisionShapes);
        }

        public JsonObject dumpBlockShapeIndices(Registry<Block> blockRegistry) {
            var resultObject = new JsonObject();

            for (var entry : blockCollisionShapes.entrySet()) {
                var blockCollisions = entry.getValue();
                JsonElement blockCollision;
                if (blockCollisions.size() == 1) {
                    blockCollision = new JsonPrimitive(blockCollisions.getFirst());
                } else {
                    var blockCollisionArray = new JsonArray();
                    for (int collisionId : blockCollisions) {
                        blockCollisionArray.add(collisionId);
                    }
                    blockCollision = blockCollisionArray;
                }

                var registryKey = blockRegistry.getResourceKey(entry.getKey()).orElseThrow().location();
                resultObject.add(registryKey.getPath(), blockCollision);
            }

            return resultObject;
        }

        public JsonObject dumpShapesObject() {
            var shapesObject = new JsonObject();

            for (var entry : uniqueBlockShapes.entrySet()) {
                var boxesArray = new JsonArray();
                entry.getKey().forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
                    var oneBoxJsonArray = new JsonArray();

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
        return "blockCollisionShapes.json";
    }

    @Override
    public JsonObject generateDataJson() {
        var blockRegistry = DGU.getWorld().registryAccess().registryOrThrow(Registries.BLOCK);
        var blockShapesCache = new BlockShapesCache();

        blockRegistry.forEach(blockShapesCache::processBlock);

        var resultObject = new JsonObject();

        resultObject.add("blocks", blockShapesCache.dumpBlockShapeIndices(blockRegistry));
        resultObject.add("shapes", blockShapesCache.dumpShapesObject());

        return resultObject;
    }
}
