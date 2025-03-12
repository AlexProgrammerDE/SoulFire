/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.generator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.soulfiremc.generator.util.MCHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.Objects;

@Slf4j
public final class MenuJsonGenerator implements IDataGenerator {

  @SneakyThrows
  public static JsonObject generateMenu(MenuType<?> menuType) {
    var menuDesc = new JsonObject();

    menuDesc.addProperty("id", BuiltInRegistries.MENU.getId(menuType));
    menuDesc.addProperty("key", Objects.requireNonNull(BuiltInRegistries.MENU.getKey(menuType)).toString());

    var playerInventory = new Inventory(MCHelper.createPlayer());
    var menuInstance = menuType.create(0, playerInventory);
    fillMenuData(playerInventory, menuInstance, menuDesc);

    return menuDesc;
  }

  public static void fillMenuData(Inventory inventory, AbstractContainerMenu menu, JsonObject menuDesc) {
    menuDesc.addProperty("slots", menu.slots.size());
    var playerInvMap = new JsonObject();
    for (var slot : menu.slots) {
      if (slot.container == inventory) {
        playerInvMap.addProperty(String.valueOf(slot.index), slot.getContainerSlot());
      }
    }
    menuDesc.add("playerInventory", playerInvMap);
  }

  @Override
  public String getDataName() {
    return "data/menus.json";
  }

  @Override
  public JsonArray generateDataJson() {
    var resultArray = new JsonArray();
    BuiltInRegistries.MENU.forEach(menu -> resultArray.add(generateMenu(menu)));

    {
      var inventoryMenuDesc = new JsonObject();
      inventoryMenuDesc.addProperty("id", -1);
      inventoryMenuDesc.addProperty("key", "soulfire:inventory_menu");
      var player = MCHelper.createPlayer();
      var playerInventory = new Inventory(player);
      var menuInstance = new InventoryMenu(playerInventory, true, player);
      fillMenuData(playerInventory, menuInstance, inventoryMenuDesc);
      resultArray.add(inventoryMenuDesc);
    }

    return resultArray;
  }
}
