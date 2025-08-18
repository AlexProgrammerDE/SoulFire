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
package com.soulfiremc.server.plugins;

import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.InternalPluginClass;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.BotConnectionInitEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.ControllingTask;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@InternalPluginClass
public final class AutoArmor extends InternalPlugin {
  private static final Map<EquipmentSlot, Integer> SLOT_VIEW_MAP = Map.of(
    EquipmentSlot.HEAD, InventoryMenu.ARMOR_SLOT_START,
    EquipmentSlot.CHEST, InventoryMenu.ARMOR_SLOT_START + 1,
    EquipmentSlot.LEGS, InventoryMenu.ARMOR_SLOT_START + 2,
    EquipmentSlot.FEET, InventoryMenu.ARMOR_SLOT_START + 3
  );
  private static final Map<EquipmentSlot, List<Item>> SLOT_ITEMS_MAP = Map.of(
    EquipmentSlot.HEAD, List.of(
      Items.DIAMOND_HELMET, Items.GOLDEN_HELMET, Items.IRON_HELMET, Items.CHAINMAIL_HELMET, Items.LEATHER_HELMET
    ),
    EquipmentSlot.CHEST, List.of(
      Items.DIAMOND_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.LEATHER_CHESTPLATE
    ),
    EquipmentSlot.LEGS, List.of(
      Items.DIAMOND_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.IRON_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.LEATHER_LEGGINGS
    ),
    EquipmentSlot.FEET, List.of(
      Items.DIAMOND_BOOTS, Items.GOLDEN_BOOTS, Items.IRON_BOOTS, Items.CHAINMAIL_BOOTS, Items.LEATHER_BOOTS
    )
  );

  public AutoArmor() {
    super(new PluginInfo(
      "auto-armor",
      "1.0.0",
      "Automatically puts on the best armor",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  private static IntStream storageSlots() {
    return IntStream.range(InventoryMenu.INV_SLOT_START, InventoryMenu.USE_ROW_SLOT_END);
  }

  private static void putOn(
    BotConnection connection,
    LocalPlayer player,
    EquipmentSlot equipmentSlotEnum) {
    var inventory = player.inventoryMenu;
    var equipmentSlotInt = SLOT_VIEW_MAP.get(equipmentSlotEnum);
    var itemTypes = SLOT_ITEMS_MAP.get(equipmentSlotEnum);

    var bestItemSlotOptional =
      storageSlots()
        .mapToObj(inventory::getSlot)
        .filter(
          s -> {
            if (s.getItem().isEmpty()) {
              return false;
            }

            return itemTypes.contains(s.getItem().getItem());
          })
        .reduce(
          (first, second) -> {
            var firstIndex = itemTypes.indexOf(first.getItem().getItem());
            var secondIndex = itemTypes.indexOf(second.getItem().getItem());

            return firstIndex > secondIndex ? first : second;
          });

    if (bestItemSlotOptional.isEmpty()) {
      return;
    }

    var bestItemSlot = bestItemSlotOptional.get();
    var bestItem = bestItemSlot.getItem();
    if (bestItem.isEmpty()) {
      return;
    }

    var equipmentSlot = inventory.getSlot(equipmentSlotInt);
    var equipmentSlotItem = equipmentSlot.getItem();
    if (!equipmentSlotItem.isEmpty()) {
      var targetIndex = itemTypes.indexOf(equipmentSlotItem.getItem());
      var bestIndex = itemTypes.indexOf(bestItem.getItem());

      if (targetIndex >= bestIndex) {
        return;
      }
    }

    if (player.hasContainerOpen()) {
      return;
    }

    var gameMode = connection.minecraft().gameMode;
    connection.botControl().maybeRegister(ControllingTask.staged(List.of(
      new ControllingTask.RunnableStage(player::sendOpenInventory),
      new ControllingTask.RunnableStage(() -> gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, bestItemSlot.index, 0, ClickType.PICKUP, player)),
      new ControllingTask.WaitDelayStage(() -> 50L),
      new ControllingTask.RunnableStage(() -> gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, equipmentSlot.index, 0, ClickType.PICKUP, player)),
      new ControllingTask.WaitDelayStage(() -> 50L),
      new ControllingTask.RunnableStage(() -> {
        if (!player.inventoryMenu.getCarried().isEmpty()) {
          gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, bestItemSlot.index, 0, ClickType.PICKUP, player);
        }
      }),
      new ControllingTask.WaitDelayStage(() -> 50L),
      new ControllingTask.RunnableStage(player::closeContainer)
    )));
  }

  @EventHandler
  public static void onJoined(BotConnectionInitEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    connection.scheduler().scheduleWithDynamicDelay(
      () -> {
        if (!settingsSource.get(AutoArmorSettings.ENABLED)) {
          return;
        }

        var player = connection.minecraft().player;
        if (player == null) {
          return;
        }

        for (var type : Arrays.stream(EquipmentSlot.values())
          .filter(slot -> slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
          .toList()) {
          putOn(connection, player, type);
        }
      },
      settingsSource.getRandom(AutoArmorSettings.DELAY).asLongSupplier(),
      TimeUnit.SECONDS);
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(AutoArmorSettings.class, "Auto Armor", this, "shield", AutoArmorSettings.ENABLED);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class AutoArmorSettings implements SettingsObject {
    private static final String NAMESPACE = "auto-armor";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Auto Armor")
        .description("Put on best armor automatically")
        .defaultValue(true)
        .build();
    public static final MinMaxProperty DELAY =
      ImmutableMinMaxProperty.builder()
        .namespace(NAMESPACE)
        .key("delay")
        .minValue(0)
        .maxValue(Integer.MAX_VALUE)
        .minEntry(
          ImmutableMinMaxPropertyEntry.builder()
            .uiName("Min delay (seconds)")
            .description("Minimum delay between putting on armor")
            .defaultValue(1)
            .build())
        .maxEntry(
          ImmutableMinMaxPropertyEntry.builder()
            .uiName("Max delay (seconds)")
            .description("Maximum delay between putting on armor")
            .defaultValue(2)
            .build())
        .build();
  }
}
