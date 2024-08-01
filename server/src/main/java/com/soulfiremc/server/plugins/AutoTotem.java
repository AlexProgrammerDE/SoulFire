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
import com.soulfiremc.server.data.ItemType;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.TimeUtil;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;

public class AutoTotem implements InternalPlugin {
  public static final PluginInfo PLUGIN_INFO = new PluginInfo(
    "auto-totem",
    "1.0.0",
    "Automatically puts totems in the offhand slot",
    "AlexProgrammerDE",
    "GPL-3.0"
  );

  public static void onJoined(BotJoinedEvent event) {
    var connection = event.connection();
    var settingsHolder = connection.settingsHolder();
    if (!settingsHolder.get(AutoTotemSettings.ENABLED)) {
      return;
    }

    connection.scheduler().scheduleWithRandomDelay(
      () -> {
        var dataManager = connection.dataManager();
        var inventoryManager = dataManager.inventoryManager();
        var playerInventory = inventoryManager.playerInventory();
        var offhandSlot = playerInventory.getOffhand();

        // We only want to use totems if there are no items in the offhand
        if (offhandSlot.item() != null) {
          return;
        }

        var totemSlot = playerInventory.findMatchingSlotForAction(
          slot -> slot.item() != null && slot.item().type() == ItemType.TOTEM_OF_UNDYING);
        if (totemSlot.isEmpty()) {
          return;
        }

        var slot = totemSlot.get();
        if (!inventoryManager.tryInventoryControl()) {
          return;
        }

        try {
          inventoryManager.leftClickSlot(slot);
          TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
          inventoryManager.leftClickSlot(offhandSlot);
          TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);

          if (inventoryManager.cursorItem() != null) {
            inventoryManager.leftClickSlot(slot);
            TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
          }
        } finally {
          inventoryManager.unlockInventoryControl();
        }
      },
      settingsHolder.get(AutoTotemSettings.DELAY.min()),
      settingsHolder.get(AutoTotemSettings.DELAY.max()),
      TimeUnit.SECONDS);
  }

  @EventHandler
  public static void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AutoTotemSettings.class, "Auto Totem", PLUGIN_INFO);
  }

  @Override
  public PluginInfo pluginInfo() {
    return PLUGIN_INFO;
  }

  @Override
  public void onServer(SoulFireServer soulFireServer) {
    soulFireServer.registerListeners(AutoTotem.class);
    PluginHelper.registerBotEventConsumer(soulFireServer, BotJoinedEvent.class, AutoTotem::onJoined);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class AutoTotemSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("auto-totem");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable Auto Totem",
        new String[] {"--auto-totem"},
        "Always put available totems in the offhand slot",
        true);
    public static final MinMaxPropertyLink DELAY =
      new MinMaxPropertyLink(
        BUILDER.ofInt(
          "min-delay",
          "Min delay (seconds)",
          new String[] {"--totem-min-delay"},
          "Minimum delay between using totems",
          1,
          0,
          Integer.MAX_VALUE,
          1),
        BUILDER.ofInt(
          "max-delay",
          "Max delay (seconds)",
          new String[] {"--totem-max-delay"},
          "Maximum delay between using totems",
          2,
          0,
          Integer.MAX_VALUE,
          1));
  }
}
