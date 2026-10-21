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

import com.soulfiremc.server.bot.BotConnection;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreativeModeTabs.class)
public class MixinCreativeModeTabs {
  @Shadow private static CreativeModeTab.@Nullable ItemDisplayParameters CACHED_PARAMETERS;

  @Redirect(method = "tryRebuildTabContents", at = @At(value = "FIELD",
    target = "Lnet/minecraft/world/item/CreativeModeTabs;CACHED_PARAMETERS:Lnet/minecraft/world/item/CreativeModeTab$ItemDisplayParameters;",
    opcode = Opcodes.GETSTATIC))
  private static CreativeModeTab.@Nullable ItemDisplayParameters parameters() {
    var connection = BotConnection.currentOptional().orElse(null);
    return connection == null ? CACHED_PARAMETERS : connection.creativeInventoryState().parameters();
  }

  @Redirect(method = "tryRebuildTabContents", at = @At(value = "FIELD",
    target = "Lnet/minecraft/world/item/CreativeModeTabs;CACHED_PARAMETERS:Lnet/minecraft/world/item/CreativeModeTab$ItemDisplayParameters;",
    opcode = Opcodes.PUTSTATIC))
  private static void parameters(CreativeModeTab.ItemDisplayParameters parameters) {
    var connection = BotConnection.currentOptional().orElse(null);
    if (connection == null) CACHED_PARAMETERS = parameters;
    else connection.creativeInventoryState().parameters(parameters);
  }
}
