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
package com.soulfiremc.server.script.nodes.action;

import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotRotationController;
import com.soulfiremc.server.script.NodeRuntime;
import com.soulfiremc.server.script.NodeValue;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

final class SetRotationNodeTest {
  private final SetRotationNode node = new SetRotationNode();
  private final NodeRuntime runtime = mock();
  private final BotConnection bot = mock();
  private final BotRotationController rotation = mock();

  @Test
  void largeFiniteAnglesReachTheControllerWithoutAnUnboundedLoop() {
    when(runtime.isTickSynchronous()).thenReturn(true);
    when(bot.rotationControl()).thenReturn(rotation);
    for (var yaw : new float[]{Float.MAX_VALUE, -Float.MAX_VALUE, 1.0E20F}) {
      assertTimeoutPreemptively(Duration.ofSeconds(2),
        () -> node.executeReactive(runtime, inputs(yaw, Float.MAX_VALUE)).block());
      verify(rotation).lookTo(yaw, Float.MAX_VALUE);
    }
  }

  @Test
  void nonFiniteAndOverflowedAnglesAreRejectedBeforeScheduling() {
    for (var value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE}) {
      assertThrows(IllegalArgumentException.class, () -> node.executeReactive(runtime, inputs(value, 0)).block());
      assertThrows(IllegalArgumentException.class, () -> node.executeReactive(runtime, inputs(0, value)).block());
    }
    verifyNoInteractions(runtime, rotation);
  }

  private Map<String, NodeValue> inputs(double yaw, double pitch) {
    return Map.of("bot", NodeValue.ofBot(bot), "yaw", NodeValue.of(yaw), "pitch", NodeValue.of(pitch));
  }
}
