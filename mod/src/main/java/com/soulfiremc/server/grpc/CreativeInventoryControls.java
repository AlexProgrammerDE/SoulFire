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
package com.soulfiremc.server.grpc;

import com.soulfiremc.grpc.generated.ContainerButton;
import com.soulfiremc.grpc.generated.ContainerLayout;
import com.soulfiremc.grpc.generated.ContainerTextInput;
import com.soulfiremc.grpc.generated.SlotRegion;
import com.soulfiremc.grpc.generated.SlotRegionType;
import com.soulfiremc.mod.mixin.soulfire.CreativeModeInventoryScreenAccessor;
import com.soulfiremc.server.bot.BotConnection;
import io.grpc.Status;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

final class CreativeInventoryControls {
  private static final int PREVIOUS_PAGE = -1;
  private static final int NEXT_PAGE = -2;

  private CreativeInventoryControls() {}

  static ContainerLayout layout(CreativeModeInventoryScreen screen) {
    var tab = selectedTab();
    var menu = screen.getMenu();
    var builder = ContainerLayout.newBuilder()
      .setTitle("Creative Inventory")
      .setContainerType("creative")
      .setTotalSlots(menu.slots.size());
    if (screen.isInventoryOpen()) {
      builder.addRegions(region("armor", "Armor", 5, 4, 2, SlotRegionType.SLOT_REGION_ARMOR));
      builder.addRegions(region("offhand", "Offhand", 45, 1, 1, SlotRegionType.SLOT_REGION_NORMAL));
      builder.addRegions(region("player_inventory", "Inventory", 9, 27, 9, SlotRegionType.SLOT_REGION_NORMAL));
      builder.addRegions(region("player_hotbar", "Hotbar", 36, 9, 9, SlotRegionType.SLOT_REGION_HOTBAR));
      builder.addRegions(region("destroy_item", "Destroy Item", 46, 1, 1, SlotRegionType.SLOT_REGION_NORMAL));
    } else {
      builder.addRegions(region("creative_catalog", tab.getDisplayName().getString(), 0, 45, 9, SlotRegionType.SLOT_REGION_NORMAL));
      builder.addRegions(region("player_hotbar", "Hotbar", 45, 9, 9, SlotRegionType.SLOT_REGION_HOTBAR));
    }
    for (var candidate : CreativeModeTabs.tabs()) {
      builder.addButtons(ContainerButton.newBuilder()
        .setButtonId(BuiltInRegistries.CREATIVE_MODE_TAB.getId(candidate))
        .setLabel(candidate.getDisplayName().getString())
        .setIconItemId(candidate.getIconItem().typeHolder().getRegisteredName())
        .setSelected(candidate == tab));
    }
    if (tab.canScroll() && menu.canScroll()) {
      var scroll = ((CreativeModeInventoryScreenAccessor) screen).soulfire$scrollOffset();
      builder.addButtons(ContainerButton.newBuilder().setButtonId(PREVIOUS_PAGE).setLabel("Previous Page").setDisabled(scroll <= 0));
      builder.addButtons(ContainerButton.newBuilder().setButtonId(NEXT_PAGE).setLabel("Next Page").setDisabled(scroll >= 1));
    }
    if (tab.getType() == CreativeModeTab.Type.SEARCH) {
      builder.addTextInputs(ContainerTextInput.newBuilder()
        .setId("search").setLabel("Search")
        .setCurrentValue(((CreativeModeInventoryScreenAccessor) screen).soulfire$searchBox().getValue())
        .setMaxLength(50));
    }
    return builder.build();
  }

  static boolean activate(CreativeModeInventoryScreen screen, int id) {
    if (id == PREVIOUS_PAGE || id == NEXT_PAGE) {
      var scroll = ((CreativeModeInventoryScreenAccessor) screen).soulfire$scrollOffset();
      if (!selectedTab().canScroll() || !screen.getMenu().canScroll()
        || id == PREVIOUS_PAGE && scroll <= 0 || id == NEXT_PAGE && scroll >= 1) return false;
      var next = ((CreativeModeInventoryScreenAccessor.Menu) screen.getMenu())
        .soulfire$subtractScroll(scroll, id == PREVIOUS_PAGE ? 5 : -5);
      ((CreativeModeInventoryScreenAccessor) screen).soulfire$scrollOffset(next);
      screen.getMenu().scrollTo(next);
      return true;
    }
    var tab = CreativeModeTabs.tabs().stream()
      .filter(candidate -> BuiltInRegistries.CREATIVE_MODE_TAB.getId(candidate) == id)
      .findFirst();
    if (tab.isEmpty()) return false;
    ((CreativeModeInventoryScreenAccessor) screen).soulfire$selectTab(tab.get());
    return true;
  }

  static void search(CreativeModeInventoryScreen screen, String text) {
    var access = (CreativeModeInventoryScreenAccessor) screen;
    if (!access.soulfire$searchBox().isVisible()) {
      throw Status.FAILED_PRECONDITION.withDescription("Creative search is not active").asRuntimeException();
    }
    access.soulfire$searchBox().setValue(text);
    access.soulfire$refreshSearchResults();
  }

  private static CreativeModeTab selectedTab() {
    return BotConnection.current().creativeInventoryState().selectedTab();
  }

  private static SlotRegion region(String id, String label, int start, int count, int columns, SlotRegionType type) {
    return SlotRegion.newBuilder().setId(id).setLabel(label).setStartIndex(start)
      .setSlotCount(count).setColumns(columns).setType(type).build();
  }
}
