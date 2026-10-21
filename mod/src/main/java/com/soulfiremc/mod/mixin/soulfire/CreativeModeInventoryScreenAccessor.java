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
package com.soulfiremc.mod.mixin.soulfire;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {
  @Accessor("searchBox")
  EditBox soulfire$searchBox();

  @Accessor("scrollOffs")
  float soulfire$scrollOffset();

  @Accessor("scrollOffs")
  void soulfire$scrollOffset(float offset);

  @Accessor("hasClickedOutside")
  boolean soulfire$hasClickedOutside();

  @Accessor("hasClickedOutside")
  void soulfire$hasClickedOutside(boolean outside);

  @Invoker("selectTab")
  void soulfire$selectTab(CreativeModeTab tab);

  @Invoker("refreshSearchResults")
  void soulfire$refreshSearchResults();

  @Invoker("slotClicked")
  void soulfire$clickSlot(@Nullable Slot slot, int slotId, int button, ContainerInput input);

  @Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
  interface Menu {
    @Invoker("subtractInputFromScroll")
    float soulfire$subtractScroll(float offset, double input);
  }
}
