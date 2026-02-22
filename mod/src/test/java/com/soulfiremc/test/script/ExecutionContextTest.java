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
package com.soulfiremc.test.script;

import com.soulfiremc.server.script.ExecutionContext;
import com.soulfiremc.server.script.NodeValue;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for {@link ExecutionContext} immutability and merge behavior.
final class ExecutionContextTest {

  @Test
  void executionContextEmpty() {
    var ctx = ExecutionContext.empty();
    assertTrue(ctx.values().isEmpty(), "Empty context should have no values");
  }

  @Test
  void executionContextFrom() {
    var initial = Map.of(
      "bot", NodeValue.ofString("testBot"),
      "tickCount", NodeValue.ofNumber(42)
    );
    var ctx = ExecutionContext.from(initial);

    assertEquals(2, ctx.values().size(), "Context should have 2 values");
    assertEquals("testBot", ctx.values().get("bot").asString(""), "Bot value should match");
    assertEquals(42, ctx.values().get("tickCount").asInt(0), "TickCount value should match");
  }

  @Test
  void executionContextMergeWithAddsNewKeys() {
    var ctx = ExecutionContext.from(Map.of("bot", NodeValue.ofString("testBot")));
    var merged = ctx.mergeWith(Map.of("health", NodeValue.ofNumber(20)));

    assertEquals(2, merged.values().size(), "Merged context should have 2 values");
    assertEquals("testBot", merged.values().get("bot").asString(""), "Bot value should persist");
    assertEquals(20, merged.values().get("health").asInt(0), "Health value should be added");
  }

  @Test
  void executionContextMergeWithOverridesExistingKeys() {
    var ctx = ExecutionContext.from(Map.of(
      "bot", NodeValue.ofString("bot1"),
      "health", NodeValue.ofNumber(10)
    ));
    var merged = ctx.mergeWith(Map.of("health", NodeValue.ofNumber(20)));

    assertEquals("bot1", merged.values().get("bot").asString(""));
    assertEquals(20, merged.values().get("health").asInt(0), "Health should be overridden to 20");
  }

  @Test
  void executionContextMergeWithIsImmutable() {
    var ctx = ExecutionContext.from(Map.of("bot", NodeValue.ofString("bot1")));
    var merged = ctx.mergeWith(Map.of("health", NodeValue.ofNumber(20)));

    assertEquals(1, ctx.values().size(), "Original context should still have 1 value");
    assertNull(ctx.values().get("health"), "Original context should not have health");

    assertEquals(2, merged.values().size(), "Merged context should have 2 values");
  }

  @Test
  void executionContextMergeWithEmptyReturnsThis() {
    var ctx = ExecutionContext.from(Map.of("bot", NodeValue.ofString("bot1")));
    var merged = ctx.mergeWith(Map.of());

    assertSame(ctx, merged, "Merging with empty map should return same instance");
  }
}
