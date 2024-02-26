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

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import com.soulfiremc.server.api.ExecutorHelper;
import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotJoinedEvent;
import com.soulfiremc.server.api.event.lifecycle.SettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.lib.property.BooleanProperty;
import com.soulfiremc.server.settings.lib.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.lib.property.Property;
import com.soulfiremc.server.util.ItemTypeHelper;
import com.soulfiremc.server.util.TimeUtil;

public class AutoEat implements InternalPlugin {
  public static void onJoined(BotJoinedEvent event) {
    var connection = event.connection();
    var settingsHolder = connection.settingsHolder();
    if (!settingsHolder.get(AutoEatSettings.ENABLED)) {
      return;
    }

    var executor = connection.executorManager().newScheduledExecutorService(connection, "AutoEat");
    ExecutorHelper.executeRandomDelaySeconds(
        executor,
        () -> {
          var sessionDataManager = connection.sessionDataManager();

          var healthData = sessionDataManager.healthData();
          if (healthData == null || healthData.food() >= 20) {
            return;
          }

          var inventoryManager = sessionDataManager.inventoryManager();
          var playerInventory = inventoryManager.playerInventory();

          var i = 0;
          for (var slot : playerInventory.hotbar()) {
            var hotbarSlot = i++;

            if (slot.item() == null) {
              continue;
            }

            var itemType = slot.item().type();
            if (!ItemTypeHelper.isGoodEdibleFood(itemType)) {
              continue;
            }

            if (!inventoryManager.tryInventoryControl()) {
              return;
            }

            try {
              inventoryManager.heldItemSlot(hotbarSlot);
              inventoryManager.sendHeldItemChange();
              sessionDataManager.botActionManager().useItemInHand(Hand.MAIN_HAND);

              // Wait before eating again
              TimeUtil.waitTime(2, TimeUnit.SECONDS);
              return;
            } finally {
              inventoryManager.unlockInventoryControl();
            }
          }

          for (var slot : playerInventory.mainInventory()) {
            if (slot.item() == null) {
              continue;
            }

            var itemType = slot.item().type();
            if (ItemTypeHelper.isGoodEdibleFood(itemType)) {
              continue;
            }

            if (!inventoryManager.tryInventoryControl()) {
              return;
            }

            try {
              inventoryManager.leftClickSlot(slot.slot());
              inventoryManager.leftClickSlot(playerInventory.getHeldItem().slot());
              if (inventoryManager.cursorItem() != null) {
                inventoryManager.leftClickSlot(slot.slot());
              }

              // Wait before eating again
              TimeUtil.waitTime(2, TimeUnit.SECONDS);
              sessionDataManager.botActionManager().useItemInHand(Hand.MAIN_HAND);
              return;
            } finally {
              inventoryManager.unlockInventoryControl();
            }
          }
        },
        settingsHolder.get(AutoEatSettings.DELAY.min()),
        settingsHolder.get(AutoEatSettings.DELAY.max()));
  }

  @EventHandler
  public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AutoEatSettings.class, "Auto Eat");
  }

  @Override
  public void onLoad() {
    SoulFireAPI.registerListeners(AutoEat.class);
    PluginHelper.registerBotEventConsumer(BotJoinedEvent.class, AutoEat::onJoined);
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
