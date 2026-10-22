/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.task;

import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.ControlPriority;
import com.soulfiremc.server.bot.ControlResource;
import com.soulfiremc.server.bot.ControlStopReason;
import com.soulfiremc.server.bot.ControlTask;
import com.soulfiremc.server.util.SFInventoryHelpers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

/// Holds use until one meal is confirmed, then releases it before another can start.
public final class EatMealTask implements ControlTask {
  private static final long CONFIRMATION_GRACE_MILLIS = 1_000;
  private static final Set<ControlResource> RESOURCES = Set.of(
    ControlResource.MAIN_HAND, ControlResource.OFF_HAND,
    ControlResource.INVENTORY, ControlResource.CONTAINER);
  private final BotConnection bot;
  private final LocalPlayer player;
  private final InteractionHand hand;
  private final ControlTask delegate;
  private ItemStack meal = ItemStack.EMPTY;
  private int initialFoodLevel;
  private boolean consuming;
  private boolean confirmed;
  private boolean cleanedUp;
  private boolean interrupted;

  public EatMealTask(BotConnection bot, int slot, boolean restoreSelectedSlot, ControlPriority priority) {
    this.bot = bot;
    player = Objects.requireNonNull(bot.minecraft().player, "Bot player is unavailable");
    var gameMode = Objects.requireNonNull(bot.minecraft().gameMode, "Bot game mode is unavailable");
    var originalHotbar = player.getInventory().getSelectedSlot();
    var stack = player.inventoryMenu.getSlot(slot).getItem();
    var consumable = stack.get(DataComponents.CONSUMABLE);
    var consumeMillis = consumable == null ? 2_000 : Math.max(250, Math.round(consumable.consumeSeconds() * 1_000));
    var steps = new ArrayList<ControlTask.Step>();
    var swappedFromInventory = false;
    var selectedDifferentHotbar = false;
    if (slot == SFInventoryHelpers.getSelectedSlot(player.getInventory())) {
      hand = InteractionHand.MAIN_HAND;
    } else if (slot == InventoryMenu.SHIELD_SLOT) {
      hand = InteractionHand.OFF_HAND;
    } else if (SFInventoryHelpers.isSelectableHotbarSlot(slot)) {
      hand = InteractionHand.MAIN_HAND;
      selectedDifferentHotbar = true;
      steps.add(ControlTask.action(() -> player.getInventory().setSelectedSlot(SFInventoryHelpers.toHotbarIndex(slot))));
      steps.add(ControlTask.waitMillis(50));
    } else {
      hand = InteractionHand.MAIN_HAND;
      swappedFromInventory = true;
      steps.add(ControlTask.action(() -> TaskInventorySupport.swapWithHotbar(bot, slot, originalHotbar)));
      steps.add(ControlTask.waitMillis(100));
    }
    steps.add(ControlTask.action(() -> {
      meal = player.getItemInHand(hand).copy();
      initialFoodLevel = player.getFoodData().getFoodLevel();
      consuming = true;
      bot.minecraft().options.keyUse.setDown(true);
      if (!(gameMode.useItem(player, hand) instanceof InteractionResult.Success)) {
        throw new IllegalStateException("The selected meal could not be used");
      }
    }));
    steps.add(ControlTask.waitMillis(consumeMillis + CONFIRMATION_GRACE_MILLIS));
    steps.add(ControlTask.action(() -> {
      if (!confirmed) {
        throw new IllegalStateException("No consumption was confirmed after using the selected meal");
      }
    }));
    if (restoreSelectedSlot && swappedFromInventory) {
      steps.add(ControlTask.action(() -> TaskInventorySupport.swapWithHotbar(bot, slot, originalHotbar)));
      steps.add(ControlTask.waitMillis(100));
    } else if (restoreSelectedSlot && selectedDifferentHotbar) {
      steps.add(ControlTask.action(() -> player.getInventory().setSelectedSlot(originalHotbar)));
    }
    delegate = ControlTask.sequence("Eat meal", priority, RESOURCES, steps);
  }

  @Override
  public void tick() {
    try {
      if (interrupted || bot.minecraft().player != player || player.isDeadOrDying()) {
        throw new IllegalStateException("The meal was interrupted before completion");
      }
      if (consuming && !confirmed && consumptionConfirmed(meal, player.getItemInHand(hand), initialFoodLevel, player.getFoodData().getFoodLevel())) {
        confirmed = true;
        stopHeldUse();
      }
      delegate.tick();
    } catch (RuntimeException | Error cause) {
      cleanUp();
      throw cause;
    }
    if (delegate.isDone()) {
      cleanUp();
    }
  }

  static boolean consumptionConfirmed(ItemStack before, ItemStack after, int initialFoodLevel, int foodLevel) {
    return foodLevel > initialFoodLevel || after.getCount() < before.getCount() || !ItemStack.isSameItem(before, after);
  }

  public boolean consumed() {
    return confirmed;
  }

  @Override
  public boolean isDone() {
    return delegate.isDone();
  }

  @Override
  public ControlPriority priority() {
    return delegate.priority();
  }

  @Override
  public Set<ControlResource> resources() {
    return RESOURCES;
  }

  @Override
  public void onSuspended() {
    if (confirmed) {
      delegate.onStopped(ControlStopReason.COMPLETED, null);
    } else {
      interrupted = true;
    }
    cleanUp();
  }

  @Override
  public void onStopped(ControlStopReason reason, @Nullable Throwable cause) {
    try {
      delegate.onStopped(reason, cause);
    } finally {
      cleanUp();
    }
  }

  @Override
  public @Nullable String description() {
    return delegate.description();
  }

  private void cleanUp() {
    if (cleanedUp) {
      return;
    }
    cleanedUp = true;
    stopHeldUse();
  }

  private void stopHeldUse() {
    bot.minecraft().options.keyUse.setDown(false);
    var currentPlayer = bot.minecraft().player;
    var gameMode = bot.minecraft().gameMode;
    if (currentPlayer != null && gameMode != null && currentPlayer.isUsingItem()) {
      gameMode.releaseUsingItem(currentPlayer);
    }
  }
}
