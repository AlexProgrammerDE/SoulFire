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
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableMinMaxProperty;
import com.soulfiremc.server.settings.property.MinMaxProperty;
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
      "GPL-3.0",
      "https://soulfiremc.com"
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
    private static final String NAMESPACE = "auto-eat";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Auto Eat")
        .description("Eat available food automatically when hungry")
        .defaultValue(true)
        .build();
    public static final MinMaxProperty DELAY =
      ImmutableMinMaxProperty.builder()
        .namespace(NAMESPACE)
        .key("delay")
        .minUiName("Min delay (seconds)")
        .maxUiName("Max delay (seconds)")
        .minDescription("Minimum delay between eating")
        .maxDescription("Maximum delay between eating")
        .minDefaultValue(1)
        .maxDefaultValue(2)
        .minValue(0)
        .maxValue(Integer.MAX_VALUE)
        .stepValue(1)
        .build();
  }
}
