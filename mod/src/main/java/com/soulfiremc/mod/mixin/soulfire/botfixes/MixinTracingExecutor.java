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
package com.soulfiremc.mod.mixin.soulfire.botfixes;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.soulfiremc.server.bot.BotConnection;
import net.minecraft.TracingExecutor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TracingExecutor.class)
public class MixinTracingExecutor {
  @WrapMethod(method = "wrapUnnamed")
  private static Runnable wrapUnnamed(Runnable command, Operation<Runnable> original) {
    var returnValue = original.call(command);
    return BotConnection.currentOptional()
      .map(connection -> connection.runnableWrapper().wrap(returnValue))
      .orElse(returnValue);
  }
}
