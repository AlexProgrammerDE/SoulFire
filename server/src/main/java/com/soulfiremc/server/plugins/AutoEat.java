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
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.ItemTypeHelper;
import com.soulfiremc.server.util.TimeUtil;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

public class AutoEat implements InternalPlugin {
  public static final PluginInfo PLUGIN_INFO = new PluginInfo(
    "auto-eat",
    "1.0.0",
    "Automatically eats food when hungry",
    "AlexProgrammerDE",
    "GPL-3.0"
  );

  public static void onJoined(BotJoinedEvent event) {
    var connection = event.connection();
    var settingsHolder = connection.settingsHolder();
    if (!settingsHolder.get(AutoEatSettings.ENABLED)) {
      return;
    }

    connection.scheduler().scheduleWithRandomDelay(
      () -> {
        var dataManager = connection.dataManager();

        var healthData = dataManager.healthData();
        if (healthData == null || healthData.food() >= 20) {
          return;
        }

        var inventoryManager = dataManager.inventoryManager();
        var playerInventory = inventoryManager.playerInventory();

        var edibleSlot = playerInventory.findMatchingSlotForAction(
          slot -> slot.item() != null && ItemTypeHelper.isGoodEdibleFood(slot.item()));
        if (edibleSlot.isEmpty()) {
          return;
        }

        var slot = edibleSlot.get();
        if (!inventoryManager.tryInventoryControl()) {
          return;
        }

        try {
          if (!playerInventory.isHeldItem(slot) && playerInventory.isHotbar(slot)) {
            inventoryManager.heldItemSlot(playerInventory.toHotbarIndex(slot));
            inventoryManager.sendHeldItemChange();
          } else if (playerInventory.isMainInventory(slot)) {
            inventoryManager.leftClickSlot(slot);
            inventoryManager.leftClickSlot(playerInventory.getHeldItem());
            if (inventoryManager.cursorItem() != null) {
              inventoryManager.leftClickSlot(slot);
            }
          }

          dataManager.botActionManager().useItemInHand(Hand.MAIN_HAND);

          // Wait before eating again
          TimeUtil.waitTime(2, TimeUnit.SECONDS);
        } finally {
          inventoryManager.unlockInventoryControl();
        }
      },
      settingsHolder.get(AutoEatSettings.DELAY.min()),
      settingsHolder.get(AutoEatSettings.DELAY.max()),
      TimeUnit.SECONDS);
  }

  @EventHandler
  public static void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AutoEatSettings.class, "Auto Eat", PLUGIN_INFO);
  }

  @Override
  public PluginInfo pluginInfo() {
    return PLUGIN_INFO;
  }

  @Override
  public void onServer(SoulFireServer soulFireServer) {
    soulFireServer.registerListeners(AutoEat.class);
    PluginHelper.registerBotEventConsumer(soulFireServer, BotJoinedEvent.class, AutoEat::onJoined);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class AutoEatSettings implements SettingsObject {
    public static final Property.Builder BUILDER = Property.builder("auto-eat");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable Auto Eat",
        new String[] {"--auto-eat"},
        "Eat available food automatically when hungry",
        true);
    public static final MinMaxPropertyLink DELAY =
      new MinMaxPropertyLink(
        BUILDER.ofInt(
          "min-delay",
          "Min delay (seconds)",
          new String[] {"--eat-min-delay"},
          "Minimum delay between eating",
          1,
          0,
          Integer.MAX_VALUE,
          1),
        BUILDER.ofInt(
          "max-delay",
          "Max delay (seconds)",
          new String[] {"--eat-max-delay"},
          "Maximum delay between eating",
          2,
          0,
          Integer.MAX_VALUE,
          1));
  }
}
