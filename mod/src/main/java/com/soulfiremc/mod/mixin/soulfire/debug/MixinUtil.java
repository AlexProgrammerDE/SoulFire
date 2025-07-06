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
package com.soulfiremc.mod.mixin.soulfire.debug;

import net.minecraft.Util;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Util.class)
public class MixinUtil {
  @Final
  @Shadow
  static Logger LOGGER;

  @Redirect(method = "logAndPauseIfInIde(Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;)V"))
  private static void logAndPauseIfInIde(Logger instance, String s) {
    LOGGER.error(s, new RuntimeException(s));
  }
}
