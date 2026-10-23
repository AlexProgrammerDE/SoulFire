/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.grpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatCompletionTest {
  private static final List<String> PLAYERS = List.of("E2EBot", "Elsewhere", "Other");

  @ParameterizedTest
  @ValueSource(strings = {"", " ", " E2", "  E2", "😀 E2"})
  void everyCursorProducesARangeWithinTheTypedPrefix(String input) {
    for (int cursor = 0; cursor <= input.length(); cursor++) {
      var result = ChatServiceImpl.chatSuggestions(input, cursor, PLAYERS);
      assertTrue(result.getRange().getStart() >= 0);
      assertTrue(result.getRange().getStart() <= result.getRange().getEnd());
      assertTrue(result.getRange().getEnd() <= cursor);
    }
  }

  @Test
  void filtersNamesWithoutReplacingTextAfterTheCursor() {
    var input = "hi e2!";
    var result = ChatServiceImpl.chatSuggestions(input, 5, PLAYERS);
    assertEquals(3, result.getRange().getStart());
    assertEquals(5, result.getRange().getEnd());
    assertEquals(1, result.getList().size());
    assertEquals("hi E2EBot!", result.getList().getFirst().apply(input));
  }
}
