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
package com.soulfiremc.mod;

import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.CreativeInventoryState;
import com.soulfiremc.test.utils.TestBootstrap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class CreativeInventoryIsolationTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @Test
  void generatingTabsKeepsEachConnectionsPermissionsAndSearchItemsIndependent() throws ReflectiveOperationException {
    var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(ResourceKey.create(
      Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("op_blocks")));
    var originalDisplayItems = tab.getDisplayItems();
    var originalSearchItems = tab.getSearchTabDisplayItems();
    var holders = HolderLookup.Provider.create(Stream.empty());
    var first = mock(BotConnection.class);
    var second = mock(BotConnection.class);
    when(first.creativeInventoryState()).thenReturn(new CreativeInventoryState());
    when(second.creativeInventoryState()).thenReturn(new CreativeInventoryState());
    var components = new ConcurrentHashMap<Holder.Reference<?>, DataComponentMap>();
    var defaults = DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE, 64).build();
    BuiltInRegistries.ITEM.listElements().forEach(holder -> components.put(holder, defaults));
    when(first.registryComponents()).thenReturn(new ConcurrentHashMap<>(components));
    when(second.registryComponents()).thenReturn(new ConcurrentHashMap<>(components));
    var field = BotConnection.class.getDeclaredField("CURRENT");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    var current = (ScopedValue<BotConnection>) field.get(null);

    ScopedValue.where(current, first).run(() -> {
      tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true, holders));
      assertTrue(tab.getDisplayItems().stream().anyMatch(item -> item.is(Items.COMMAND_BLOCK)));
      assertTrue(tab.getSearchTabDisplayItems().stream().anyMatch(item -> item.is(Items.COMMAND_BLOCK)));
      ScopedValue.where(current, second).run(() -> {
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, false, holders));
        assertTrue(tab.getDisplayItems().isEmpty());
        assertTrue(tab.getSearchTabDisplayItems().isEmpty());
      });
      assertTrue(tab.getDisplayItems().stream().anyMatch(item -> item.is(Items.COMMAND_BLOCK)));
      assertTrue(tab.getSearchTabDisplayItems().stream().anyMatch(item -> item.is(Items.COMMAND_BLOCK)));
    });
    assertSame(originalDisplayItems, tab.getDisplayItems());
    assertSame(originalSearchItems, tab.getSearchTabDisplayItems());
  }
}
