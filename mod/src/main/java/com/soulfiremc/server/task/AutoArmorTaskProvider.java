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

import com.soulfiremc.grpc.generated.AutoArmorCompletionReason;
import com.soulfiremc.grpc.generated.AutoArmorTask;
import com.soulfiremc.grpc.generated.AutoArmorTaskResult;
import com.soulfiremc.grpc.generated.BotTaskProgress;
import com.soulfiremc.server.api.BotTaskExecution;
import com.soulfiremc.server.api.BotTaskProvider;
import com.soulfiremc.server.bot.CompletableControlTask;
import com.soulfiremc.server.bot.ControlPriority;
import com.soulfiremc.server.bot.ControlResource;
import com.soulfiremc.server.bot.ControlStopReason;
import com.soulfiremc.server.bot.ControlTask;
import com.soulfiremc.server.util.SFItemHelpers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/// Core resource-aware provider that equips the strongest available armor.
public final class AutoArmorTaskProvider
  implements BotTaskProvider<AutoArmorTask> {
  private static final int DEFAULT_CHECK_INTERVAL_TICKS = 20;
  private static final int MAX_CHECK_INTERVAL_TICKS = 1_200;
  private static final Set<ControlResource> TASK_RESOURCES = Set.of();
  private static final Set<ControlResource> EQUIP_RESOURCES = Set.of(
    ControlResource.INVENTORY,
    ControlResource.CONTAINER
  );
  private static final Map<EquipmentSlot, Integer> ARMOR_SLOTS = Map.of(
    EquipmentSlot.HEAD,
    InventoryMenu.ARMOR_SLOT_START,
    EquipmentSlot.CHEST,
    InventoryMenu.ARMOR_SLOT_START + 1,
    EquipmentSlot.LEGS,
    InventoryMenu.ARMOR_SLOT_START + 2,
    EquipmentSlot.FEET,
    InventoryMenu.ARMOR_SLOT_START + 3
  );

  @Override
  public AutoArmorTask inputPrototype() {
    return AutoArmorTask.getDefaultInstance();
  }

  @Override
  public String summary(AutoArmorTask input) {
    return input.getMaximumEquips() == 0
      ? "Keep the strongest available armor equipped"
      : "Equip up to " + input.getMaximumEquips() + " armor upgrades";
  }

  @Override
  public Set<ControlResource> resources(AutoArmorTask input) {
    return TASK_RESOURCES;
  }

  @Override
  public BotTaskExecution start(
    BotTaskContext context,
    AutoArmorTask input
  ) {
    var interval = input.getCheckIntervalTicks() == 0
      ? DEFAULT_CHECK_INTERVAL_TICKS
      : Math.min(
        input.getCheckIntervalTicks(),
        MAX_CHECK_INTERVAL_TICKS
      );
    var result = new CompletableFuture<AutoArmorTaskResult>();
    return new BotTaskExecution(
      new AutoArmorControl(
        context,
        interval,
        input.getMaximumEquips(),
        input.getCompleteWhenNoUpgrade(),
        result
      ),
      result
    );
  }

  private static final class AutoArmorControl implements ControlTask {
    private final BotTaskContext context;
    private final int checkIntervalTicks;
    private final int maximumEquips;
    private final boolean completeWhenNoUpgrade;
    private final CompletableFuture<AutoArmorTaskResult> result;
    private @Nullable CompletableControlTask activeEquip;
    private int equips;
    private int ticks;

    private AutoArmorControl(
      BotTaskContext context,
      int checkIntervalTicks,
      int maximumEquips,
      boolean completeWhenNoUpgrade,
      CompletableFuture<AutoArmorTaskResult> result
    ) {
      this.context = context;
      this.checkIntervalTicks = checkIntervalTicks;
      this.maximumEquips = maximumEquips;
      this.completeWhenNoUpgrade = completeWhenNoUpgrade;
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

      var player = context.bot().minecraft().player;
      if (
        player == null
          || player.hasContainerOpen()
          || !player.inventoryMenu.getCarried().isEmpty()
      ) {
        return;
      }
      var upgrade = findUpgrade(player.inventoryMenu);
      if (upgrade.isEmpty()) {
        report("Best available armor is equipped");
        if (completeWhenNoUpgrade) {
          complete(
            AutoArmorCompletionReason
              .AUTO_ARMOR_COMPLETION_REASON_NO_UPGRADE
          );
        }
        return;
      }

      var selected = upgrade.orElseThrow();
      var equip = new CompletableControlTask(createEquip(
        context,
        selected.sourceSlot(),
        selected.equipmentSlot()
      ));
      if (context.bot().botControl().submit(equip)) {
        activeEquip = equip;
        report("Equipping armor upgrade");
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
        report("Armor upgraded");
        if (maximumEquips > 0 && equips >= maximumEquips) {
          complete(
            AutoArmorCompletionReason
              .AUTO_ARMOR_COMPLETION_REASON_EQUIP_LIMIT_REACHED
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

    private void complete(AutoArmorCompletionReason reason) {
      result.complete(AutoArmorTaskResult.newBuilder()
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
      return "Auto armor";
    }
  }

  private static Optional<ArmorUpgrade> findUpgrade(
    InventoryMenu inventory
  ) {
    for (var equipmentSlot : ARMOR_SLOTS.keySet()) {
      var equippedSlot = ARMOR_SLOTS.get(equipmentSlot);
      var equippedScore = SFItemHelpers.armorScore(
        inventory.getSlot(equippedSlot).getItem(),
        equipmentSlot
      );
      var best = IntStream.range(
          InventoryMenu.INV_SLOT_START,
          InventoryMenu.USE_ROW_SLOT_END
        )
        .mapToObj(inventory::getSlot)
        .filter(slot -> {
          var equippable = slot.getItem().get(DataComponents.EQUIPPABLE);
          return equippable != null && equippable.slot() == equipmentSlot;
        })
        .max((left, right) -> Double.compare(
          SFItemHelpers.armorScore(left.getItem(), equipmentSlot),
          SFItemHelpers.armorScore(right.getItem(), equipmentSlot)
        ));
      if (best.isPresent()
        && SFItemHelpers.armorScore(best.orElseThrow().getItem(), equipmentSlot)
        > equippedScore) {
        return Optional.of(new ArmorUpgrade(
          best.orElseThrow().index,
          equippedSlot
        ));
      }
    }
    return Optional.empty();
  }

  private static ControlTask createEquip(
    BotTaskContext context,
    int sourceSlot,
    int equipmentSlot
  ) {
    var player = context.bot().minecraft().player;
    if (player == null || context.bot().minecraft().gameMode == null) {
      return ControlTask.once(
        "Auto armor unavailable",
        EQUIP_RESOURCES,
        () -> {
          throw new IllegalStateException(
            "Bot player or game mode is unavailable"
          );
        }
      );
    }
    return TaskInventorySupport.swapSlotsViaSelectedHotbar(
      context.bot(),
      "SDK auto armor equip",
      ControlPriority.HIGH,
      EQUIP_RESOURCES,
      sourceSlot,
      equipmentSlot
    );
  }

  private record ArmorUpgrade(int sourceSlot, int equipmentSlot) {}
}
