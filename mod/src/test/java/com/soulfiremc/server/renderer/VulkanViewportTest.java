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
package com.soulfiremc.server.renderer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VulkanViewportTest {
  @ParameterizedTest
  @CsvSource({"320, 180, 427, 240", "160, 90, 427, 240", "200, 300, 320, 480"})
  void smallViewersKeepTheNativeMinimumLayoutAndTheirAspectRatio(int width, int height, int guiWidth, int guiHeight) {
    assertEquals(new VulkanRenderer.Viewport(guiWidth, guiHeight), VulkanRenderer.Viewport.interactive(width, height));
  }

  @ParameterizedTest
  @CsvSource({"320, 240", "640, 480", "1280, 720", "3840, 2160"})
  void largerViewersRetainTheirNativeScaleSelection(int width, int height) {
    assertEquals(new VulkanRenderer.Viewport(width, height), VulkanRenderer.Viewport.interactive(width, height));
  }
}
