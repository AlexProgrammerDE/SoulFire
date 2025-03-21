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
import com.soulfiremc.server.protocol.bot.ControllingTask;
import com.soulfiremc.server.protocol.bot.container.PlayerInventoryMenu;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import com.soulfiremc.server.util.SFItemHelpers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.pf4j.Extension;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Extension
public final class AutoEat extends InternalPlugin {
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
        if (!dataManager.joinedWorld()) {
          return;
        }

        var localPlayer = dataManager.localPlayer();
        if (localPlayer.getFoodData().hasEnoughFood()) {
          return;
        }

        var playerInventory = localPlayer.inventoryMenu;

        var edibleSlot = playerInventory.findMatchingSlotForAction(
          slot -> SFItemHelpers.isGoodEdibleFood(slot.item()));
        if (edibleSlot.isEmpty()) {
          return;
        }

        var slot = edibleSlot.get();
        if (localPlayer.hasContainerOpen()) {
          return;
        }

        if (playerInventory.isHeldItem(slot)) {
          connection.botControl().maybeRegister(ControllingTask.staged(List.of(
            new ControllingTask.RunnableStage(() -> connection.dataManager().gameModeState().useItemInHand(Hand.MAIN_HAND))
          )));
        } else if (PlayerInventoryMenu.isOffhandSlot(slot)) {
          connection.botControl().maybeRegister(ControllingTask.staged(List.of(
            new ControllingTask.RunnableStage(() -> connection.dataManager().gameModeState().useItemInHand(Hand.OFF_HAND))
          )));
        } else if (PlayerInventoryMenu.isHotbarSlot(slot)) {
          connection.botControl().maybeRegister(ControllingTask.staged(List.of(
            new ControllingTask.RunnableStage(() -> localPlayer.inventory().selected = PlayerInventoryMenu.toHotbarIndex(slot)),
            new ControllingTask.WaitDelayStage(() -> 50L),
            new ControllingTask.RunnableStage(() -> connection.dataManager().gameModeState().useItemInHand(Hand.MAIN_HAND))
          )));
        } else {
          connection.botControl().maybeRegister(ControllingTask.staged(List.of(
            new ControllingTask.RunnableStage(localPlayer::openPlayerInventory),
            new ControllingTask.RunnableStage(() -> localPlayer.inventoryMenu.leftClick(slot)),
            new ControllingTask.WaitDelayStage(() -> 50L),
            new ControllingTask.RunnableStage(() -> localPlayer.inventoryMenu.leftClick(playerInventory.getSelectedSlot())),
            new ControllingTask.WaitDelayStage(() -> 50L),
            new ControllingTask.RunnableStage(() -> {
              if (!localPlayer.inventoryMenu.getCarried().isEmpty()) {
                localPlayer.inventoryMenu.leftClick(slot);
              }
            }),
            new ControllingTask.WaitDelayStage(() -> 50L),
            new ControllingTask.RunnableStage(localPlayer::closeContainer),
            new ControllingTask.WaitDelayStage(() -> 50L),
            new ControllingTask.RunnableStage(() -> connection.dataManager().gameModeState().useItemInHand(Hand.MAIN_HAND))
          )));
        }
      },
      settingsSource.getRandom(AutoEatSettings.DELAY).asLongSupplier(),
      TimeUnit.SECONDS);
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(AutoEatSettings.class, "Auto Eat", this, "drumstick", AutoEatSettings.ENABLED);
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
        .minValue(0)
        .maxValue(Integer.MAX_VALUE)
        .minEntry(ImmutableMinMaxPropertyEntry.builder()
          .uiName("Min delay (seconds)")
          .description("Minimum delay between eating")
          .defaultValue(1)
          .build())
        .maxEntry(ImmutableMinMaxPropertyEntry.builder()
          .uiName("Max delay (seconds)")
          .description("Maximum delay between eating")
          .defaultValue(2)
          .build())
        .build();
  }
}
