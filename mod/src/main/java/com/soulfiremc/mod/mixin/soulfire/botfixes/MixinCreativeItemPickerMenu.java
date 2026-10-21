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
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.SimpleContainer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public class MixinCreativeItemPickerMenu {
  @WrapOperation(method = {"<init>", "scrollTo", "canTakeItemForPickAll", "canDragTo"}, at = @At(value = "FIELD",
    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;CONTAINER:Lnet/minecraft/world/SimpleContainer;",
    opcode = Opcodes.GETSTATIC))
  private static SimpleContainer container(Operation<SimpleContainer> original) {
    var connection = BotConnection.currentOptional().orElse(null);
    return connection == null ? original.call() : connection.creativeInventoryState().container();
  }
}
