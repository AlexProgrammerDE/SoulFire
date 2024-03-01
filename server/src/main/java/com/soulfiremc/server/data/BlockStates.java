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
package com.soulfiremc.server.data;

import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.List;

public record BlockStates(BlockState defaultState, List<BlockState> possibleStates) {
  public static BlockStates fromJsonArray(BlockType blockType, JsonArray array) {
    BlockState defaultState = null;
    List<BlockState> possibleStates = new ArrayList<>();
    var i = 0;
    for (var state : array) {
      var stateObject = state.getAsJsonObject();
      var stateId = stateObject.get("id").getAsInt();
      var defaultStateValue = stateObject.get("default") != null;

      var properties = new BlockStateProperties(stateObject.getAsJsonObject("properties"));

      var blockState = new BlockState(stateId, defaultStateValue, properties, blockType, i);

      if (defaultStateValue) {
        defaultState = blockState;
      }

      possibleStates.add(blockState);
      i++;
    }

    return new BlockStates(defaultState, possibleStates);
  }
}
