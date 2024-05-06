package com.soulfiremc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockType;
import org.junit.jupiter.api.Test;

public class DataTest {
  @Test
  public void checkBlockStateTypeCorrect() {
    var airBlockState = BlockState.forDefaultBlockType(BlockType.AIR);

    assertEquals(airBlockState.blockType(), BlockType.AIR);
    assertNotEquals(airBlockState.blockType(), BlockType.DIRT);

    assertSame(airBlockState.blockType(), BlockType.AIR);
    assertNotSame(airBlockState.blockType(), BlockType.DIRT);
  }

  @Test
  public void checkBlockShapesCorrect() {
    var airBlockState = BlockState.forDefaultBlockType(BlockType.AIR);
    var dirtBlockState = BlockState.forDefaultBlockType(BlockType.DIRT);

    assertNotEquals(airBlockState.blockShapeGroup(), dirtBlockState.blockShapeGroup());
    assertNotSame(airBlockState.blockShapeGroup(), dirtBlockState.blockShapeGroup());

    assertEquals(airBlockState.blockShapeGroup().blockShapes().size(), 0);
    assertEquals(dirtBlockState.blockShapeGroup().blockShapes().size(), 1);
  }
}
