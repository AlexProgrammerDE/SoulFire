package net.pistonmaster.serverwrecker.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockCollisionShapesDataGenerator implements IDataGenerator {

    private static class BlockShapesCache {
        public final Object2IntMap<VoxelShape> uniqueBlockShapes = new Object2IntOpenHashMap<>() {{
            defaultReturnValue(-1);
        }};
        public final Map<Block, IntList> blockCollisionShapes = new HashMap<>();
        private int lastCollisionShapeId = 0;

        public void processBlock(Block block) {
            List<BlockState> blockStates = block.getStateDefinition().getPossibleStates();
            IntList blockCollisionShapes = new IntArrayList();

            for (var blockState : blockStates) {
                var blockShape = blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);

                // Replace block offset
                var blockShapeCenter = blockState.getOffset(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                var inverseBlockShapeCenter = blockShapeCenter.reverse();
                blockShape = blockShape.move(inverseBlockShapeCenter.x, inverseBlockShapeCenter.y, inverseBlockShapeCenter.z);

                var blockShapeIndex = uniqueBlockShapes.getInt(blockShape);
                if (blockShapeIndex == -1) {
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

            for (var entry : uniqueBlockShapes.object2IntEntrySet()) {
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
                shapesObject.add(Integer.toString(entry.getIntValue()), boxesArray);
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
        var blockRegistry = BuiltInRegistries.BLOCK;
        var blockShapesCache = new BlockShapesCache();

        blockRegistry.forEach(blockShapesCache::processBlock);

        var resultObject = new JsonObject();

        resultObject.add("blocks", blockShapesCache.dumpBlockShapeIndices(blockRegistry));
        resultObject.add("shapes", blockShapesCache.dumpShapesObject());

        return resultObject;
    }
}
