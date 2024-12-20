/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.test;

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

    assertNotEquals(airBlockState.collisionShape(), dirtBlockState.collisionShape());
    assertNotSame(airBlockState.collisionShape(), dirtBlockState.collisionShape());

    assertEquals(airBlockState.collisionShape().blockShapes().size(), 0);
    assertEquals(dirtBlockState.collisionShape().blockShapes().size(), 1);
  }
}
