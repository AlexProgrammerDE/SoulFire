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
import com.soulfiremc.server.data.ItemType;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableMinMaxProperty;
import com.soulfiremc.server.settings.property.MinMaxProperty;
import com.soulfiremc.server.util.TimeUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.pf4j.Extension;

import java.util.concurrent.TimeUnit;

@Extension
public class AutoTotem extends InternalPlugin {
  public AutoTotem() {
    super(new PluginInfo(
      "auto-totem",
      "1.0.0",
      "Automatically puts totems in the offhand slot",
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
        if (!settingsSource.get(AutoTotemSettings.ENABLED)) {
          return;
        }

        var inventoryManager = connection.inventoryManager();
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
        if (!inventoryManager.tryInventoryControl() || inventoryManager.lookingAtForeignContainer()) {
          return;
        }

        try {
          inventoryManager.openPlayerInventory();
          inventoryManager.leftClickSlot(slot);
          TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
          inventoryManager.leftClickSlot(offhandSlot);
          TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);

          if (inventoryManager.cursorItem() != null) {
            inventoryManager.leftClickSlot(slot);
            TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
          }

          inventoryManager.closeInventory();
        } finally {
          inventoryManager.unlockInventoryControl();
        }
      },
      settingsSource.getRandom(AutoTotemSettings.DELAY).asLongSupplier(),
      TimeUnit.SECONDS);
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AutoTotemSettings.class, "Auto Totem", this, "cross");
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class AutoTotemSettings implements SettingsObject {
    private static final String NAMESPACE = "auto-totem";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Auto Totem")
        .description("Always put available totems in the offhand slot")
        .defaultValue(true)
        .build();
    public static final MinMaxProperty DELAY = ImmutableMinMaxProperty.builder()
      .namespace(NAMESPACE)
      .key("delay")
      .minUiName("Min delay (seconds)")
      .maxUiName("Max delay (seconds)")
      .minDescription("Minimum delay between using totems")
      .maxDescription("Maximum delay between using totems")
      .minDefaultValue(1)
      .maxDefaultValue(2)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .stepValue(1)
      .build();
  }
}
