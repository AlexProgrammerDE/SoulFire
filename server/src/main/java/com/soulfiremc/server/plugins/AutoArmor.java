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
import com.soulfiremc.server.data.ArmorType;
import com.soulfiremc.server.protocol.bot.ControllingTask;
import com.soulfiremc.server.protocol.bot.container.InventoryManager;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.pf4j.Extension;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Extension
public class AutoArmor extends InternalPlugin {
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

    var equipmentSlot = inventory.getEquipmentSlot(armorType.toEquipmentSlot()).orElseThrow();
    var equipmentSlotItem = equipmentSlot.item();
    if (equipmentSlotItem != null) {
      var targetIndex = armorType.itemTypes().indexOf(equipmentSlotItem.type());
      var bestIndex = armorType.itemTypes().indexOf(bestItem.type());

      if (targetIndex >= bestIndex) {
        return;
      }
    }

    if (inventoryManager.lookingAtForeignContainer()) {
      return;
    }

    inventoryManager.connection().botControl().maybeRegister(ControllingTask.staged(List.of(
      new ControllingTask.RunnableStage(inventoryManager::openPlayerInventory),
      new ControllingTask.RunnableStage(() -> inventoryManager.leftClickSlot(bestItemSlot)),
      new ControllingTask.WaitDelayStage(() -> 50L),
      new ControllingTask.RunnableStage(() -> inventoryManager.leftClickSlot(equipmentSlot)),
      new ControllingTask.WaitDelayStage(() -> 50L),
      new ControllingTask.RunnableStage(() -> {
        if (inventoryManager.cursorItem() != null) {
          inventoryManager.leftClickSlot(bestItemSlot);
        }
      }),
      new ControllingTask.WaitDelayStage(() -> 50L),
      new ControllingTask.RunnableStage(inventoryManager::closeInventory)
    )));
  }

  @EventHandler
  public static void onJoined(BotJoinedEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    connection.scheduler().scheduleWithDynamicDelay(
      () -> {
        if (!settingsSource.get(AutoArmorSettings.ENABLED)) {
          return;
        }

        for (var type : ArmorType.VALUES) {
          putOn(connection.inventoryManager(), type);
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
