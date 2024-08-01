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

import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.BotJoinedEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.data.ArmorType;
import com.soulfiremc.server.protocol.bot.container.InventoryManager;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.TimeUtil;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;

public class AutoArmor implements InternalPlugin {
  public static final PluginInfo PLUGIN_INFO = new PluginInfo(
    "auto-armor",
    "1.0.0",
    "Automatically puts on the best armor",
    "AlexProgrammerDE",
    "GPL-3.0"
  );

  private static void putOn(
    InventoryManager inventoryManager,
    ArmorType armorType) {
    var inventory = inventoryManager.playerInventory();

    var bestItemSlotOptional =
      Arrays.stream(inventory.storage())
        .filter(
          s -> {
            if (s.item() == null) {
              return false;
            }

            return armorType.itemTypes().contains(s.item().type());
          })
        .reduce(
          (first, second) -> {
            assert first.item() != null;

            var firstIndex = armorType.itemTypes().indexOf(first.item().type());
            var secondIndex = armorType.itemTypes().indexOf(second.item().type());

            return firstIndex > secondIndex ? first : second;
          });

    if (bestItemSlotOptional.isEmpty()) {
      return;
    }

    var bestItemSlot = bestItemSlotOptional.get();
    var bestItem = bestItemSlot.item();
    if (bestItem == null) {
      return;
    }

    var equipmentSlot = inventory.getEquipmentSlot(armorType.toEquipmentSlot());
    var equipmentSlotItem = equipmentSlot.item();
    if (equipmentSlotItem != null) {
      var targetIndex = armorType.itemTypes().indexOf(equipmentSlotItem.type());
      var bestIndex = armorType.itemTypes().indexOf(bestItem.type());

      if (targetIndex >= bestIndex) {
        return;
      }
    }

    if (!inventoryManager.tryInventoryControl()) {
      return;
    }

    try {
      inventoryManager.leftClickSlot(bestItemSlot);
      TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
      inventoryManager.leftClickSlot(equipmentSlot);
      TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);

      if (inventoryManager.cursorItem() != null) {
        inventoryManager.leftClickSlot(bestItemSlot);
        TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
      }
    } finally {
      inventoryManager.unlockInventoryControl();
    }
  }

  public static void onJoined(BotJoinedEvent event) {
    var connection = event.connection();
    var settingsHolder = connection.settingsHolder();
    if (!settingsHolder.get(AutoArmorSettings.ENABLED)) {
      return;
    }

    connection.scheduler().scheduleWithRandomDelay(
      () -> {
        for (var type : ArmorType.VALUES) {
          putOn(connection.dataManager().inventoryManager(), type);
        }
      },
      settingsHolder.get(AutoArmorSettings.DELAY.min()),
      settingsHolder.get(AutoArmorSettings.DELAY.max()),
      TimeUnit.SECONDS);
  }

  @EventHandler
  public static void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AutoArmorSettings.class, "Auto Armor", PLUGIN_INFO);
  }

  @Override
  public PluginInfo pluginInfo() {
    return PLUGIN_INFO;
  }

  @Override
  public void onServer(SoulFireServer soulFireServer) {
    soulFireServer.registerListeners(AutoArmor.class);
    PluginHelper.registerBotEventConsumer(soulFireServer, BotJoinedEvent.class, AutoArmor::onJoined);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class AutoArmorSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("auto-armor");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable Auto Armor",
        new String[] {"--auto-armor"},
        "Put on best armor automatically",
        true);
    public static final MinMaxPropertyLink DELAY =
      new MinMaxPropertyLink(
        BUILDER.ofInt(
          "min-delay",
          "Min delay (seconds)",
          new String[] {"--armor-min-delay"},
          "Minimum delay between putting on armor",
          1,
          0,
          Integer.MAX_VALUE,
          1),
        BUILDER.ofInt(
          "max-delay",
          "Max delay (seconds)",
          new String[] {"--armor-max-delay"},
          "Maximum delay between putting on armor",
          2,
          0,
          Integer.MAX_VALUE,
          1));
  }
}
