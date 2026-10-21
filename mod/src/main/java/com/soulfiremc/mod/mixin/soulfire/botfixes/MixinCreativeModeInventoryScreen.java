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
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.CreativeModeTab;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public class MixinCreativeModeInventoryScreen {
  @Shadow @Final private static SimpleContainer CONTAINER;
  @Shadow private static CreativeModeTab selectedTab;

  @Redirect(method = "*", at = @At(value = "FIELD",
    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;CONTAINER:Lnet/minecraft/world/SimpleContainer;",
    opcode = Opcodes.GETSTATIC))
  private static SimpleContainer container() {
    var connection = BotConnection.currentOptional().orElse(null);
    return connection == null ? CONTAINER : connection.creativeInventoryState().container();
  }

  @Redirect(method = "*", at = @At(value = "FIELD",
    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;selectedTab:Lnet/minecraft/world/item/CreativeModeTab;",
    opcode = Opcodes.GETSTATIC))
  private static CreativeModeTab selectedTab() {
    var connection = BotConnection.currentOptional().orElse(null);
    return connection == null ? selectedTab : connection.creativeInventoryState().selectedTab();
  }

  @Redirect(method = "*", at = @At(value = "FIELD",
    target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen;selectedTab:Lnet/minecraft/world/item/CreativeModeTab;",
    opcode = Opcodes.PUTSTATIC))
  private static void selectedTab(CreativeModeTab tab) {
    var connection = BotConnection.currentOptional().orElse(null);
    if (connection == null) selectedTab = tab;
    else connection.creativeInventoryState().selectedTab(tab);
  }

  @Inject(method = "selectTab", at = @At("RETURN"))
  private void indexSlots(CreativeModeTab tab, CallbackInfo ci) {
    if (BotConnection.currentOptional().isEmpty()) return;
    // The inventory tab bypasses addSlot when replacing its slots.
    var slots = ((CreativeModeInventoryScreen) (Object) this).getMenu().slots;
    for (var index = 0; index < slots.size(); index++) slots.get(index).index = index;
  }
}
