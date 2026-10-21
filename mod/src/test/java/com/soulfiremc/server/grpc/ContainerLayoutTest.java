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

import com.soulfiremc.test.utils.TestBootstrap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

final class ContainerLayoutTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @Test
  void lecternRegionsStayWithinTheActualMenuSlots() {
    var menu = new LecternMenu(1);
    var layout = BotServiceImpl.buildContainerLayout(menu, "", mock(ClientLevel.class));

    assertTrue(layout.getRegionsList().stream().allMatch(region ->
      region.getStartIndex() >= 0 && region.getStartIndex() + region.getSlotCount() <= menu.slots.size()));
  }

  @Test
  void lecternNextPageStopsAtTheLastPage() {
    var menu = new LecternMenu(1);
    var book = new ItemStack(Holder.direct(Items.WRITABLE_BOOK, DataComponentMap.EMPTY), 1);
    book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(
      Filterable.passThrough("first"), Filterable.passThrough("second"))));
    menu.getSlot(0).set(book);
    var level = mock(ClientLevel.class);
    var firstPage = BotServiceImpl.buildContainerLayout(menu, "", level);
    assertFalse(firstPage.getButtonsList().stream().filter(button -> button.getButtonId() == 2).findFirst().orElseThrow().getDisabled());

    menu.setData(0, 1);
    var lastPage = BotServiceImpl.buildContainerLayout(menu, "", level);
    assertTrue(lastPage.getButtonsList().stream().filter(button -> button.getButtonId() == 2).findFirst().orElseThrow().getDisabled());
  }
}
