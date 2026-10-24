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
import com.soulfiremc.mod.mixin.soulfire.MerchantScreenAccessor;
import com.soulfiremc.test.utils.TestBootstrap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

final class ContainerLayoutTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @Test
  void merchantSelectionTracksOnlyTheMatchingNativeScreen() {
    var stack = new ItemStack(Holder.direct(Items.EMERALD, DataComponentMap.EMPTY), 1);
    stack.set(DataComponents.ITEM_NAME, Component.empty());
    var offer = mock(MerchantOffer.class);
    when(offer.getResult()).thenReturn(stack);
    when(offer.getCostA()).thenReturn(stack);
    when(offer.getCostB()).thenReturn(ItemStack.EMPTY);
    var offers = new MerchantOffers();
    offers.add(offer);
    offers.add(offer);
    var merchant = mock(Merchant.class);
    when(merchant.getOffers()).thenReturn(offers);
    var inventory = new Inventory(mock(LocalPlayer.class), new EntityEquipment());
    var menu = new MerchantMenu(1, inventory, merchant);
    var screen = mock(MerchantScreen.class, withSettings().extraInterfaces(MerchantScreenAccessor.class));
    when(screen.getMenu()).thenReturn(menu);
    var minecraft = mock(Minecraft.class);
    minecraft.gui = mock(Gui.class);
    when(minecraft.gui.screen()).thenReturn(screen);

    for (var selected : new int[]{0, 1, 0}) {
      when(((MerchantScreenAccessor) screen).soulfire$getSelectedTrade()).thenReturn(selected);
      var layout = BotServiceImpl.buildContainerLayout(menu, "", minecraft);
      assertEquals(List.of(selected), layout.getButtonsList().stream()
        .filter(ContainerButton::getSelected).map(ContainerButton::getButtonId).toList());
    }

    when(screen.getMenu()).thenReturn(new MerchantMenu(2, inventory, merchant));
    assertTrue(BotServiceImpl.buildContainerLayout(menu, "", minecraft).getButtonsList().stream()
      .noneMatch(ContainerButton::getSelected));
    when(minecraft.gui.screen()).thenReturn(null);
    assertTrue(BotServiceImpl.buildContainerLayout(menu, "", minecraft).getButtonsList().stream()
      .noneMatch(ContainerButton::getSelected));
  }

  @Test
  void lecternRegionsStayWithinTheActualMenuSlots() {
    var menu = new LecternMenu(1);
    var layout = BotServiceImpl.buildContainerLayout(menu, "", mock(Minecraft.class));

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
    var minecraft = mock(Minecraft.class);
    var firstPage = BotServiceImpl.buildContainerLayout(menu, "", minecraft);
    assertFalse(firstPage.getButtonsList().stream().filter(button -> button.getButtonId() == 2).findFirst().orElseThrow().getDisabled());

    menu.setData(0, 1);
    var lastPage = BotServiceImpl.buildContainerLayout(menu, "", minecraft);
    assertTrue(lastPage.getButtonsList().stream().filter(button -> button.getButtonId() == 2).findFirst().orElseThrow().getDisabled());
  }

  @Test
  void lecternTakeBookTracksBuildingPermission() {
    var menu = new LecternMenu(1);
    menu.getSlot(0).set(new ItemStack(Holder.direct(Items.WRITABLE_BOOK, DataComponentMap.EMPTY), 1));
    var minecraft = mock(Minecraft.class);
    minecraft.player = mock(LocalPlayer.class);
    for (var mayBuild : new boolean[]{false, true, false}) {
      when(minecraft.player.mayBuild()).thenReturn(mayBuild);
      var layout = BotServiceImpl.buildContainerLayout(menu, "", minecraft);
      var takeBook = layout.getButtonsList().stream().filter(button -> button.getButtonId() == 3).findFirst().orElseThrow();
      if (mayBuild) assertFalse(takeBook.getDisabled());
      else assertTrue(takeBook.getDisabled());
    }
  }

  @Test
  void lecternActionsRejectUnavailableControlsBeforeSendingPackets() {
    var menu = new LecternMenu(1);
    var book = new ItemStack(Holder.direct(Items.WRITABLE_BOOK, DataComponentMap.EMPTY), 1);
    book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(
      Filterable.passThrough("first"), Filterable.passThrough("second"))));
    menu.getSlot(0).set(book);
    var minecraft = mock(Minecraft.class);
    minecraft.player = mock(LocalPlayer.class);
    minecraft.player.containerMenu = menu;
    minecraft.gameMode = mock(MultiPlayerGameMode.class);

    assertFalse(BotServiceImpl.activateContainerButton(minecraft, 0));
    assertFalse(BotServiceImpl.activateContainerButton(minecraft, 1));
    assertFalse(BotServiceImpl.activateContainerButton(minecraft, 3));
    assertFalse(BotServiceImpl.activateContainerButton(minecraft, 4));
    verifyNoInteractions(minecraft.gameMode);

    assertTrue(BotServiceImpl.activateContainerButton(minecraft, 2));
    verify(minecraft.gameMode).handleInventoryButtonClick(menu.containerId, 2);
    menu.setData(0, 1);
    assertFalse(BotServiceImpl.activateContainerButton(minecraft, 2));
    assertTrue(BotServiceImpl.activateContainerButton(minecraft, 1));
    verify(minecraft.gameMode).handleInventoryButtonClick(menu.containerId, 1);

    when(minecraft.player.mayBuild()).thenReturn(true);
    assertTrue(BotServiceImpl.activateContainerButton(minecraft, 3));
    verify(minecraft.gameMode).handleInventoryButtonClick(menu.containerId, 3);
    menu.getSlot(0).set(ItemStack.EMPTY);
    assertFalse(BotServiceImpl.activateContainerButton(minecraft, 3));
  }
}
