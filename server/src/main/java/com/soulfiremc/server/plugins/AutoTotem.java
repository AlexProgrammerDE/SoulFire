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
import com.soulfiremc.server.protocol.bot.ControllingTask;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.pf4j.Extension;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Extension
public class AutoTotem extends InternalPlugin {
  public AutoTotem() {
    super(new PluginInfo(
      "auto-totem",
      "1.0.0",
      "Automatically puts totems in the offhand slot",
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
        if (inventoryManager.lookingAtForeignContainer()) {
          return;
        }

        inventoryManager.connection().botControl().maybeRegister(ControllingTask.staged(List.of(
          new ControllingTask.RunnableStage(inventoryManager::openPlayerInventory),
          new ControllingTask.RunnableStage(() -> inventoryManager.leftClickSlot(slot)),
          new ControllingTask.WaitDelayStage(() -> 50L),
          new ControllingTask.RunnableStage(() -> inventoryManager.leftClickSlot(offhandSlot)),
          new ControllingTask.WaitDelayStage(() -> 50L),
          new ControllingTask.RunnableStage(() -> {
            if (inventoryManager.cursorItem() != null) {
              inventoryManager.leftClickSlot(slot);
            }
          }),
          new ControllingTask.WaitDelayStage(() -> 50L),
          new ControllingTask.RunnableStage(inventoryManager::closeInventory)
        )));
      },
      settingsSource.getRandom(AutoTotemSettings.DELAY).asLongSupplier(),
      TimeUnit.SECONDS);
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(AutoTotemSettings.class, "Auto Totem", this, "cross", AutoTotemSettings.ENABLED);
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
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .minEntry(ImmutableMinMaxPropertyEntry.builder()
        .uiName("Min delay (seconds)")
        .description("Minimum delay between using totems")
        .defaultValue(1)
        .build())
      .maxEntry(ImmutableMinMaxPropertyEntry.builder()
        .uiName("Max delay (seconds)")
        .description("Maximum delay between using totems")
        .defaultValue(2)
        .build())
      .build();
  }
}
