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
package com.soulfiremc.server.bot;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Mutable creative catalog and selection state belongs to one client connection.
@Getter
public final class CreativeInventoryState {
  private final SimpleContainer container = new SimpleContainer(45);
  private final Map<CreativeModeTab, TabContents> contents = new IdentityHashMap<>();
  @Setter
  private @Nullable CreativeModeTab selectedTab;
  @Setter
  private CreativeModeTab.@Nullable ItemDisplayParameters parameters;

  public CreativeModeTab selectedTab() {
    return selectedTab == null ? CreativeModeTabs.getDefaultTab() : selectedTab;
  }

  public TabContents contents(CreativeModeTab tab) {
    return contents.computeIfAbsent(tab, _ -> new TabContents());
  }

  @Getter
  @Setter
  public static final class TabContents {
    private Collection<ItemStack> displayItems = List.of();
    private Set<ItemStack> searchItems = Set.of();
  }
}
