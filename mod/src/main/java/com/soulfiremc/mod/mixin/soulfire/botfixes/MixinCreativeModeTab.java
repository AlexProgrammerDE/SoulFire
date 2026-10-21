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
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.Set;

/// Tab definitions are global, but their generated stacks use each client's registries and permissions.
@Mixin(CreativeModeTab.class)
public class MixinCreativeModeTab {
  @Shadow private Collection<ItemStack> displayItems;
  @Shadow private Set<ItemStack> displayItemsSearchTab;

  @Redirect(method = "*", at = @At(value = "FIELD",
    target = "Lnet/minecraft/world/item/CreativeModeTab;displayItems:Ljava/util/Collection;", opcode = Opcodes.GETFIELD))
  private Collection<ItemStack> displayItems(CreativeModeTab tab) {
    var connection = BotConnection.currentOptional().orElse(null);
    return connection == null ? displayItems : connection.creativeInventoryState().contents(tab).displayItems();
  }

  @Redirect(method = "buildContents", at = @At(value = "FIELD",
    target = "Lnet/minecraft/world/item/CreativeModeTab;displayItems:Ljava/util/Collection;", opcode = Opcodes.PUTFIELD))
  private void displayItems(CreativeModeTab tab, Collection<ItemStack> items) {
    var connection = BotConnection.currentOptional().orElse(null);
    if (connection == null) displayItems = items;
    else connection.creativeInventoryState().contents(tab).displayItems(items);
  }

  @Redirect(method = "*", at = @At(value = "FIELD",
    target = "Lnet/minecraft/world/item/CreativeModeTab;displayItemsSearchTab:Ljava/util/Set;", opcode = Opcodes.GETFIELD))
  private Set<ItemStack> searchItems(CreativeModeTab tab) {
    var connection = BotConnection.currentOptional().orElse(null);
    return connection == null ? displayItemsSearchTab : connection.creativeInventoryState().contents(tab).searchItems();
  }

  @Redirect(method = "buildContents", at = @At(value = "FIELD",
    target = "Lnet/minecraft/world/item/CreativeModeTab;displayItemsSearchTab:Ljava/util/Set;", opcode = Opcodes.PUTFIELD))
  private void searchItems(CreativeModeTab tab, Set<ItemStack> items) {
    var connection = BotConnection.currentOptional().orElse(null);
    if (connection == null) displayItemsSearchTab = items;
    else connection.creativeInventoryState().contents(tab).searchItems(items);
  }
}
