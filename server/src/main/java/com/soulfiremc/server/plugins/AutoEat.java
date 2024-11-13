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
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.BotJoinedEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.ItemTypeHelper;
import com.soulfiremc.server.util.TimeUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.pf4j.Extension;

import java.util.concurrent.TimeUnit;

@Extension
public class AutoEat extends InternalPlugin {
  public AutoEat() {
    super(new PluginInfo(
      "auto-eat",
      "1.0.0",
      "Automatically eats food when hungry",
      "AlexProgrammerDE",
      "GPL-3.0"
    ));
  }

  @EventHandler
  public static void onJoined(BotJoinedEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    connection.scheduler().scheduleWithDynamicDelay(
      () -> {
        if (!settingsSource.get(AutoEatSettings.ENABLED)) {
          return;
        }

        var dataManager = connection.dataManager();

        var healthData = dataManager.healthData();
        if (healthData == null || healthData.food() >= 20) {
          return;
        }

        var inventoryManager = connection.inventoryManager();
        var playerInventory = inventoryManager.playerInventory();

        var edibleSlot = playerInventory.findMatchingSlotForAction(
          slot -> slot.item() != null && ItemTypeHelper.isGoodEdibleFood(slot.item()));
        if (edibleSlot.isEmpty()) {
          return;
        }

        var slot = edibleSlot.get();
        if (!inventoryManager.tryInventoryControl() || inventoryManager.lookingAtForeignContainer()) {
          return;
        }

        try {
          if (!playerInventory.isHeldItem(slot) && playerInventory.isHotbar(slot)) {
            inventoryManager.changeHeldItem(playerInventory.toHotbarIndex(slot));
          } else if (playerInventory.isMainInventory(slot)) {
            inventoryManager.openPlayerInventory();
            inventoryManager.leftClickSlot(slot);
            inventoryManager.leftClickSlot(playerInventory.getHeldItem());
            if (inventoryManager.cursorItem() != null) {
              inventoryManager.leftClickSlot(slot);
            }

            inventoryManager.closeInventory();
          }

          connection.botActionManager().useItemInHand(Hand.MAIN_HAND);

          // Wait before eating again
          TimeUtil.waitTime(2, TimeUnit.SECONDS);
        } finally {
          inventoryManager.unlockInventoryControl();
        }
      },
      settingsSource.getRandom(AutoEatSettings.DELAY).asLongSupplier(),
      TimeUnit.SECONDS);
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AutoEatSettings.class, "Auto Eat", this, "drumstick");
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class AutoEatSettings implements SettingsObject {
    public static final Property.Builder BUILDER = Property.builder("auto-eat");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable Auto Eat",
        "Eat available food automatically when hungry",
        true);
    public static final MinMaxPropertyLink DELAY =
      new MinMaxPropertyLink(
        BUILDER.ofInt(
          "min-delay",
          "Min delay (seconds)",
          "Minimum delay between eating",
          1,
          0,
          Integer.MAX_VALUE,
          1),
        BUILDER.ofInt(
          "max-delay",
          "Max delay (seconds)",
          "Maximum delay between eating",
          2,
          0,
          Integer.MAX_VALUE,
          1));
  }
}
