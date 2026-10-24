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

import com.soulfiremc.mod.mixin.soulfire.CreativeModeInventoryScreenAccessor;
import com.soulfiremc.server.bot.BotConnection;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
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

  @Inject(method = "removed", at = @At("HEAD"))
  private void returnCreativeCursor(CallbackInfo ci) {
    var connection = BotConnection.currentOptional().orElse(null);
    if (connection == null) return;
    var minecraft = connection.minecraft();
    var listener = minecraft.getConnection();
    var screen = (CreativeModeInventoryScreen) (Object) this;
    var menu = screen.getMenu();
    if (minecraft.player == null || minecraft.gameMode == null || !minecraft.player.hasInfiniteMaterials()
      || listener == null || !listener.getConnection().isConnected() || menu.getCarried().isEmpty()) return;

    // Creative clicks keep the cursor client-side; closing a server menu cannot return it.
    connection.beginCreativeCursorReturn(menu.getCarried());
    var previousTab = selectedTab();
    var inventoryTab = CreativeModeTabs.tabs().stream()
      .filter(tab -> tab.getType() == CreativeModeTab.Type.INVENTORY)
      .findFirst().orElseThrow();
    var access = (CreativeModeInventoryScreenAccessor) screen;
    access.soulfire$selectTab(inventoryTab);
    var returnedSlot = -1;
    try {
      for (var pass = 0; pass < 2 && !menu.getCarried().isEmpty(); pass++) {
        for (var index = InventoryMenu.INV_SLOT_START; index < InventoryMenu.USE_ROW_SLOT_END; index++) {
          var carried = menu.getCarried();
          if (carried.isEmpty()) break;
          var slot = menu.getSlot(index);
          var existing = slot.getItem();
          if (slot.mayPlace(carried) && (pass == 1
            ? !existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, carried)
              && existing.getCount() < slot.getMaxStackSize(carried)
            : existing.isEmpty())) {
            access.soulfire$clickSlot(slot, index, 0, ContainerInput.PICKUP);
            returnedSlot = index;
          }
        }
      }
      if (!menu.getCarried().isEmpty()) {
        access.soulfire$clickSlot(null, -999, 0, ContainerInput.THROW);
      }
    } finally {
      access.soulfire$selectTab(previousTab);
    }
    if (returnedSlot >= 0 && minecraft.player.containerMenu == minecraft.player.inventoryMenu) {
      // Creative slot writes have no echo. A server-owned pickup and close confirms the return.
      minecraft.gameMode.handleContainerInput(minecraft.player.inventoryMenu.containerId,
        returnedSlot, 0, ContainerInput.PICKUP, minecraft.player);
      listener.send(new ServerboundContainerClosePacket(minecraft.player.inventoryMenu.containerId));
    }
  }

  @Inject(method = "selectTab", at = @At("RETURN"))
  private void indexSlots(CreativeModeTab tab, CallbackInfo ci) {
    if (BotConnection.currentOptional().isEmpty()) return;
    // The inventory tab bypasses addSlot when replacing its slots.
    var slots = ((CreativeModeInventoryScreen) (Object) this).getMenu().slots;
    for (var index = 0; index < slots.size(); index++) slots.get(index).index = index;
  }
}
