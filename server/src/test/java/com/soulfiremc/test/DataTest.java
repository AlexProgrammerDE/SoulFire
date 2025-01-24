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

    assertEquals(BlockType.AIR, airBlockState.blockType());
    assertNotEquals(BlockType.DIRT, airBlockState.blockType());

    assertSame(BlockType.AIR, airBlockState.blockType());
    assertNotSame(BlockType.DIRT, airBlockState.blockType());
  }

  @Test
  public void checkBlockShapesCorrect() {
    var airBlockState = BlockState.forDefaultBlockType(BlockType.AIR);
    var dirtBlockState = BlockState.forDefaultBlockType(BlockType.DIRT);

    assertNotEquals(airBlockState.collisionShape(), dirtBlockState.collisionShape());
    assertNotSame(airBlockState.collisionShape(), dirtBlockState.collisionShape());

    assertEquals(0, airBlockState.collisionShape().blockShapes().size());
    assertEquals(1, dirtBlockState.collisionShape().blockShapes().size());
  }
}
