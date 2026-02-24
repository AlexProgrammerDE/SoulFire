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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.soulfiremc.server.bot.BotConnection;
import net.minecraft.client.gui.screens.ConnectScreen;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ConnectScreen.class)
public class MixinConnectScreen {
  @WrapOperation(method = "connect", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
  private void logConnectInfo(Logger instance, String s, Object o1, Object o2, Operation<Void> original) {
    // Prevent logging anything here, as it is not needed
  }

  @WrapOperation(method = "connect", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V"))
  private void startConnectionThread(Thread instance, Operation<Void> original) {
    var bot = BotConnection.CURRENT.get();
    if (bot == null) {
      throw new IllegalStateException("No bot connection found in ConnectScreen.connect, but it should be there!");
    }

    // We are preventing a new Thread from spawning naturally from this, we know what we're doing.
    bot.minecraft().execute(bot.runnableWrapper().wrap(instance));
  }
}
