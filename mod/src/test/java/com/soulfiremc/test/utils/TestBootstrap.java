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
package com.soulfiremc.test.utils;

import com.soulfiremc.test.mixin.MixinSupportMain;
import lombok.SneakyThrows;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientBootstrap;
import net.minecraft.server.Bootstrap;

public final class TestBootstrap {
  private TestBootstrap() {
  }

  @SneakyThrows
  public static void bootstrapForTest() {
    MixinSupportMain.load();

    SharedConstants.tryDetectVersion();
    SharedConstants.CHECK_DATA_FIXER_SCHEMA = false;
    Bootstrap.bootStrap();
    ClientBootstrap.bootstrap();
    Bootstrap.validate();
  }
}
