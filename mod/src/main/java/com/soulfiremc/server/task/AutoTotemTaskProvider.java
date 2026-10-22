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

import com.soulfiremc.grpc.generated.AutoTotemCompletionReason;
import com.soulfiremc.grpc.generated.AutoTotemTask;
import com.soulfiremc.grpc.generated.AutoTotemTaskResult;
import com.soulfiremc.grpc.generated.BotTaskProgress;
import com.soulfiremc.server.api.BotTaskExecution;
import com.soulfiremc.server.api.BotTaskProvider;
import com.soulfiremc.server.bot.CompletableControlTask;
import com.soulfiremc.server.bot.ControlPriority;
import com.soulfiremc.server.bot.ControlResource;
import com.soulfiremc.server.bot.ControlStopReason;
import com.soulfiremc.server.bot.ControlTask;
import com.soulfiremc.server.util.SFInventoryHelpers;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/// Core resource-aware provider that keeps a totem in the bot's offhand.
public final class AutoTotemTaskProvider
  implements BotTaskProvider<AutoTotemTask> {
  private static final int DEFAULT_CHECK_INTERVAL_TICKS = 20;
  private static final int MAX_CHECK_INTERVAL_TICKS = 1_200;
  private static final Set<ControlResource> TASK_RESOURCES = Set.of();
  private static final Set<ControlResource> EQUIP_RESOURCES = Set.of(
    ControlResource.OFF_HAND,
    ControlResource.INVENTORY,
    ControlResource.CONTAINER
  );

  @Override
  public AutoTotemTask inputPrototype() {
    return AutoTotemTask.getDefaultInstance();
  }

  @Override
  public String summary(AutoTotemTask input) {
    return input.getMaximumEquips() == 0
      ? "Keep a totem in the offhand"
      : "Equip up to " + input.getMaximumEquips() + " offhand totems";
  }

  @Override
  public Set<ControlResource> resources(AutoTotemTask input) {
    return TASK_RESOURCES;
  }

  @Override
  public BotTaskExecution start(
    BotTaskContext context,
    AutoTotemTask input
  ) {
    var interval = input.getCheckIntervalTicks() == 0
      ? DEFAULT_CHECK_INTERVAL_TICKS
      : Math.min(
        input.getCheckIntervalTicks(),
        MAX_CHECK_INTERVAL_TICKS
      );
    var result = new CompletableFuture<AutoTotemTaskResult>();
    return new BotTaskExecution(
      new AutoTotemControl(
        context,
        interval,
        input.getMaximumEquips(),
        input.getCompleteWhenNoTotem(),
        input.getReplaceOccupiedOffhand(),
        result
      ),
      result
    );
  }

  private static final class AutoTotemControl implements ControlTask {
    private final BotTaskContext context;
    private final int checkIntervalTicks;
    private final int maximumEquips;
    private final boolean completeWhenNoTotem;
    private final boolean replaceOccupiedOffhand;
    private final CompletableFuture<AutoTotemTaskResult> result;
    private @Nullable CompletableControlTask activeEquip;
    private int equips;
    private int ticks;

    private AutoTotemControl(
      BotTaskContext context,
      int checkIntervalTicks,
      int maximumEquips,
      boolean completeWhenNoTotem,
      boolean replaceOccupiedOffhand,
      CompletableFuture<AutoTotemTaskResult> result
    ) {
      this.context = context;
      this.checkIntervalTicks = checkIntervalTicks;
      this.maximumEquips = maximumEquips;
      this.completeWhenNoTotem = completeWhenNoTotem;
      this.replaceOccupiedOffhand = replaceOccupiedOffhand;
      this.result = result;
    }

    @Override
    public void tick() {
      if (result.isDone()) {
        return;
      }
      ticks++;
      if (finishActiveEquip()) {
        return;
      }
      if (activeEquip != null || ticks % checkIntervalTicks != 0) {
        return;
      }

      var bot = context.bot();
      var player = bot.minecraft().player;
      if (player == null || player.hasContainerOpen()
        || !player.inventoryMenu.getCarried().isEmpty()) {
        return;
      }
      var offhand = player.inventoryMenu
        .getSlot(InventoryMenu.SHIELD_SLOT)
        .getItem();
      if (offhand.is(Items.TOTEM_OF_UNDYING)) {
        report("Totem equipped");
        return;
      }
      if (!offhand.isEmpty() && !replaceOccupiedOffhand) {
        report("Offhand is occupied");
        return;
      }

      var source = SFInventoryHelpers.findMatchingSlotForAction(
        player.getInventory(),
        player.inventoryMenu,
        stack -> stack.is(Items.TOTEM_OF_UNDYING)
      );
      if (source.isEmpty()) {
        report("No totem available");
        if (completeWhenNoTotem) {
          complete(
            AutoTotemCompletionReason
              .AUTO_TOTEM_COMPLETION_REASON_NO_TOTEM
          );
        }
        return;
      }

      var equip = new CompletableControlTask(
        createEquip(context, source.getAsInt())
      );
      if (bot.botControl().submit(equip)) {
        activeEquip = equip;
        report("Equipping totem");
      }
    }

    private boolean finishActiveEquip() {
      var equip = activeEquip;
      if (equip == null || !equip.completion().isDone()) {
        return false;
      }
      activeEquip = null;
      if (!equip.completion().isCompletedExceptionally()
        && equip.completion().join() == ControlStopReason.COMPLETED) {
        equips++;
        report("Totem equipped");
        if (maximumEquips > 0 && equips >= maximumEquips) {
          complete(
            AutoTotemCompletionReason
              .AUTO_TOTEM_COMPLETION_REASON_EQUIP_LIMIT_REACHED
          );
          return true;
        }
      }
      return false;
    }

    private void report(String message) {
      var progress = BotTaskProgress.newBuilder()
        .setMessage(message)
        .setCurrent(equips);
      if (maximumEquips > 0) {
        progress
          .setTotal(maximumEquips)
          .setFraction(Math.min(
            1.0,
            (double) equips / maximumEquips
          ));
      }
      context.reportProgress(progress.build());
    }

    private void complete(AutoTotemCompletionReason reason) {
      result.complete(AutoTotemTaskResult.newBuilder()
        .setReason(reason)
        .setEquips(equips)
        .build());
    }

    @Override
    public boolean isDone() {
      return result.isDone();
    }

    @Override
    public Set<ControlResource> resources() {
      return TASK_RESOURCES;
    }

    @Override
    public void onStopped(
      ControlStopReason reason,
      @Nullable Throwable cause
    ) {
      var equip = activeEquip;
      activeEquip = null;
      if (equip != null) {
        context.bot().botControl().cancel(equip);
      }
      if (reason != ControlStopReason.COMPLETED && !result.isDone()) {
        result.cancel(true);
      }
    }

    @Override
    public String description() {
      return "Auto totem";
    }
  }

  private static ControlTask createEquip(
    BotTaskContext context,
    int sourceSlot
  ) {
    var player = context.bot().minecraft().player;
    var gameMode = context.bot().minecraft().gameMode;
    if (player == null || gameMode == null) {
      return ControlTask.once(
        "Auto totem unavailable",
        EQUIP_RESOURCES,
        () -> {
          throw new IllegalStateException(
            "Bot player or game mode is unavailable"
          );
        }
      );
    }
    return ControlTask.sequence(
      "SDK auto totem equip",
      ControlPriority.HIGH,
      EQUIP_RESOURCES,
      List.of(
        ControlTask.action(player::sendOpenInventory),
        ControlTask.action(() -> click(context, sourceSlot)),
        ControlTask.waitMillis(50L),
        ControlTask.action(() ->
          click(context, InventoryMenu.SHIELD_SLOT)),
        ControlTask.waitMillis(50L),
        ControlTask.action(() -> {
          if (!player.inventoryMenu.getCarried().isEmpty()) {
            click(context, sourceSlot);
          }
        }),
        ControlTask.waitMillis(50L),
        ControlTask.action(player::closeContainer)
      )
    );
  }

  private static void click(BotTaskContext context, int slot) {
    var player = context.bot().minecraft().player;
    var gameMode = context.bot().minecraft().gameMode;
    if (player == null || gameMode == null) {
      throw new IllegalStateException(
        "Bot player or game mode is unavailable"
      );
    }
    gameMode.handleContainerInput(
      player.inventoryMenu.containerId,
      slot,
      0,
      ContainerInput.PICKUP,
      player
    );
  }
}
