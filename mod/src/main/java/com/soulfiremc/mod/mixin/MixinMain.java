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
package com.soulfiremc.mod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.soulfiremc.mod.util.SFConstants;
import com.soulfiremc.server.util.log4j.GenericTerminalConsole;
import lombok.SneakyThrows;
import me.earth.headlessmc.lwjgl.agent.LwjglAgent;
import net.lenni0451.reflect.Agents;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public final class MixinMain {
  @SneakyThrows
  @Inject(method = "main([Ljava/lang/String;)V", at = @At("HEAD"))
  private static void init(CallbackInfo cir) {
    Agents.getInstrumentation().addTransformer(new LwjglAgent());
    GenericTerminalConsole.setupStreams();
    SharedConstants.CHECK_DATA_FIXER_SCHEMA = false;
  }

  @SneakyThrows
  @Redirect(method = "main([Ljava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;run()V"))
  private static void init(Minecraft instance, @Local(argsOnly = true) String[] args) {
    // We want this to not inject anywhere else
    SFConstants.MINECRAFT_INSTANCE.remove();

    SFConstants.BASE_MC_INSTANCE = instance;

    try {
      Class.forName(System.getProperty("sf.bootstrap.class"))
        .getDeclaredMethod("bootstrap", String[].class)
        .invoke(null, (Object) args);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }
}
