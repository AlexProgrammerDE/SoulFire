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

import com.soulfiremc.grpc.generated.BotTaskProgress;
import com.soulfiremc.grpc.generated.ItemSelector;
import com.soulfiremc.grpc.generated.VillagerTradeTask;
import com.soulfiremc.grpc.generated.VillagerTradeTaskResult;
import com.soulfiremc.mod.mixin.soulfire.MerchantScreenAccessor;
import com.soulfiremc.server.api.BotTaskExecution;
import com.soulfiremc.server.api.BotTaskProvider;
import com.soulfiremc.server.bot.ControlPriority;
import com.soulfiremc.server.bot.ControlResource;
import com.soulfiremc.server.bot.ControlStopReason;
import com.soulfiremc.server.bot.ControlTask;
import com.soulfiremc.server.grpc.InventoryServiceImpl;
import com.soulfiremc.server.grpc.MinecraftDomainMapper;
import com.soulfiremc.server.util.SFInventoryHelpers;
import io.grpc.Status;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/// Executes an exact number of offers from the currently open merchant menu.
public final class VillagerTradeTaskProvider
  implements BotTaskProvider<VillagerTradeTask> {
  private static final int MAX_TRADE_OPERATIONS = 4_096;
  private static final int MENU_TIMEOUT_TICKS = 100;
  private static final Set<ControlResource> RESOURCES = Set.of(
    ControlResource.INVENTORY,
    ControlResource.CONTAINER
  );

  @Override
  public VillagerTradeTask inputPrototype() {
    return VillagerTradeTask.getDefaultInstance();
  }

  @Override
  public String summary(VillagerTradeTask input) {
    return "Execute villager offer " + input.getOfferIndex()
      + " exactly " + Math.max(1, input.getCount()) + " time(s)";
  }

  @Override
  public Set<ControlResource> resources(VillagerTradeTask input) {
    return RESOURCES;
  }

  @Override
  public BotTaskExecution start(
    BotTaskContext context,
    VillagerTradeTask input
  ) {
    var count = input.getCount() <= 0 ? 1 : input.getCount();
    if (count > MAX_TRADE_OPERATIONS) {
      throw Status.INVALID_ARGUMENT
        .withDescription(
          "count may not exceed " + MAX_TRADE_OPERATIONS
        )
        .asRuntimeException();
    }
    var player = Objects.requireNonNull(
      context.bot().minecraft().player,
      "Bot player is not available"
    );
    if (!(player.containerMenu instanceof MerchantMenu menu)) {
      throw Status.FAILED_PRECONDITION
        .withDescription(
          "Open a villager merchant menu before starting a trade task"
        )
        .asRuntimeException();
    }
    if (!menu.getCarried().isEmpty()) {
      throw Status.FAILED_PRECONDITION
        .withDescription(
          "The bot must not be carrying an item on the inventory cursor"
        )
        .asRuntimeException();
    }
    var offerIndex = Math.toIntExact(input.getOfferIndex());
    if (offerIndex < 0 || offerIndex >= menu.getOffers().size()) {
      throw Status.INVALID_ARGUMENT
        .withDescription("offer_index is outside the current merchant offers")
        .asRuntimeException();
    }
    var offer = menu.getOffers().get(offerIndex);
    if (
      input.hasExpectedResult()
      && !InventoryServiceImpl.matches(
        offer.getResult(),
        input.getExpectedResult()
      )
    ) {
      throw Status.ABORTED
        .withDescription(
          "The selected merchant offer no longer has the expected result"
        )
        .asRuntimeException();
    }

    var result = new CompletableFuture<VillagerTradeTaskResult>();
    return new BotTaskExecution(
      new TradeControl(
        context,
        menu,
        offerIndex,
        count,
        input.getCloseWhenDone(),
        input.hasExpectedResult() ? input.getExpectedResult() : null,
        result
      ),
      result
    );
  }

  private static final class TradeControl implements ControlTask {
    private final BotTaskContext context;
    private @Nullable MerchantMenu openedMenu;
    private final int offerIndex;
    private final int targetCount;
    private final boolean closeWhenDone;
    private final @Nullable ItemSelector expectedResult;
    private final CompletableFuture<VillagerTradeTaskResult> result;
    private final List<ItemStack> outputs = new ArrayList<>();
    private Stage stage = Stage.SELECT_OFFER;
    private int stageTicks;
    private int completedTrades;
    private boolean active;

    private TradeControl(
      BotTaskContext context,
      MerchantMenu openedMenu,
      int offerIndex,
      int targetCount,
      boolean closeWhenDone,
      @Nullable ItemSelector expectedResult,
      CompletableFuture<VillagerTradeTaskResult> result
    ) {
      this.context = context;
      this.openedMenu = openedMenu;
      this.offerIndex = offerIndex;
      this.targetCount = targetCount;
      this.closeWhenDone = closeWhenDone;
      this.expectedResult = expectedResult;
      this.result = result;
    }

    @Override
    public void tick() {
      if (result.isDone()) {
        return;
      }
      try {
        if (completedTrades >= targetCount) {
          complete();
          return;
        }
        switch (stage) {
          case SELECT_OFFER -> selectOffer();
          case WAIT_FOR_RESULT -> waitForResult();
          case TAKE_RESULT -> takeResult();
          case DEPOSIT_RESULT -> depositResult();
        }
      } catch (Throwable throwable) {
        result.completeExceptionally(throwable);
      }
    }

    private void selectOffer() {
      var menu = requireMenu();
      var offer = requireOffer(menu);
      if (offer.isOutOfStock()) {
        throw Status.FAILED_PRECONDITION
          .withDescription(
            "Merchant offer became out of stock after "
              + completedTrades + " completed trade(s)"
          )
          .asRuntimeException();
      }
      if (
        expectedResult != null
        && !InventoryServiceImpl.matches(offer.getResult(), expectedResult)
      ) {
        throw Status.ABORTED
          .withDescription(
            "Merchant offer changed while the trade task was running"
          )
          .asRuntimeException();
      }
      if (!hasOutputCapacity(menu, offer.getResult())) {
        throw Status.RESOURCE_EXHAUSTED
          .withDescription("Inventory has no room for the trade result")
          .asRuntimeException();
      }
      var minecraft = context.bot().minecraft();
      var connection = Objects.requireNonNull(
        minecraft.getConnection(),
        "Bot connection is not available"
      );
      if (minecraft.gui.screen() instanceof MerchantScreen screen && screen.getMenu() == menu) {
        ((MerchantScreenAccessor) screen).soulfire$setSelectedTrade(offerIndex);
      }
      connection.send(new ServerboundSelectTradePacket(offerIndex));
      menu.setSelectionHint(offerIndex);
      menu.tryMoveItems(offerIndex);
      transition(Stage.WAIT_FOR_RESULT, "Preparing villager trade");
    }

    private void waitForResult() {
      var menu = requireMenu();
      var offer = requireOffer(menu);
      var output = menu.getSlot(2).getItem();
      if (!output.isEmpty()) {
        if (!ItemStack.isSameItemSameComponents(output, offer.getResult())) {
          throw Status.ABORTED
            .withDescription(
              "Merchant output changed while the trade task was running"
            )
            .asRuntimeException();
        }
        outputs.add(output.copy());
        transition(Stage.TAKE_RESULT, "Taking villager trade result");
        return;
      }
      stageTicks++;
      if (stageTicks > MENU_TIMEOUT_TICKS) {
        throw Status.FAILED_PRECONDITION
          .withDescription(
            "Trade result did not appear; costs may be missing or mismatched"
          )
          .asRuntimeException();
      }
    }

    private void takeResult() {
      var player = requirePlayer();
      var menu = requireMenu();
      Objects.requireNonNull(
        context.bot().minecraft().gameMode,
        "Bot game mode is not available"
      ).handleContainerInput(
        menu.containerId,
        2,
        0,
        ContainerInput.PICKUP,
        player
      );
      transition(Stage.DEPOSIT_RESULT, "Storing villager trade result");
    }

    private void depositResult() {
      var menu = requireMenu();
      var carried = menu.getCarried();
      if (carried.isEmpty()) {
        completedTrades++;
        transition(Stage.SELECT_OFFER, completedTrades >= targetCount
          ? "Villager trading complete"
          : "Preparing next villager trade");
        return;
      }
      var destination = SFInventoryHelpers.playerInventorySlots(menu)
        .filter(slot -> canDeposit(menu, slot, carried))
        .findFirst();
      if (destination.isEmpty()) {
        throw Status.RESOURCE_EXHAUSTED
          .withDescription("Inventory has no room for the trade result")
          .asRuntimeException();
      }
      Objects.requireNonNull(
        context.bot().minecraft().gameMode,
        "Bot game mode is not available"
      ).handleContainerInput(
        menu.containerId,
        destination.getAsInt(),
        0,
        ContainerInput.PICKUP,
        requirePlayer()
      );
      stageTicks++;
      if (stageTicks > MENU_TIMEOUT_TICKS) {
        throw Status.DEADLINE_EXCEEDED
          .withDescription("Timed out storing the trade result")
          .asRuntimeException();
      }
    }

    private MerchantMenu requireMenu() {
      var menu = openedMenu;
      if (menu != null && requirePlayer().containerMenu == menu) {
        return menu;
      }
      throw Status.ABORTED
        .withDescription("The merchant menu changed or was closed")
        .asRuntimeException();
    }

    private net.minecraft.world.item.trading.MerchantOffer requireOffer(
      MerchantMenu menu
    ) {
      if (offerIndex >= menu.getOffers().size()) {
        throw Status.ABORTED
          .withDescription("The selected merchant offer no longer exists")
          .asRuntimeException();
      }
      return menu.getOffers().get(offerIndex);
    }

    private net.minecraft.client.player.LocalPlayer requirePlayer() {
      return Objects.requireNonNull(
        context.bot().minecraft().player,
        "Bot player is not available"
      );
    }

    private static boolean hasOutputCapacity(
      MerchantMenu menu,
      ItemStack output
    ) {
      var capacity = SFInventoryHelpers.playerInventorySlots(menu)
        .map(slotIndex -> {
          var slot = menu.getSlot(slotIndex);
          if (!slot.mayPlace(output)) {
            return 0;
          }
          var existing = slot.getItem();
          if (existing.isEmpty()) {
            return Math.min(output.getMaxStackSize(), slot.getMaxStackSize(output));
          }
          if (!ItemStack.isSameItemSameComponents(existing, output)) {
            return 0;
          }
          return Math.max(
            0,
            Math.min(existing.getMaxStackSize(), slot.getMaxStackSize(existing))
              - existing.getCount()
          );
        })
        .sum();
      return capacity >= output.getCount();
    }

    private static boolean canDeposit(
      AbstractContainerMenu menu,
      int slotIndex,
      ItemStack carried
    ) {
      var slot = menu.getSlot(slotIndex);
      if (!slot.mayPlace(carried)) {
        return false;
      }
      var existing = slot.getItem();
      return existing.isEmpty()
        || ItemStack.isSameItemSameComponents(existing, carried)
        && existing.getCount() < Math.min(
        existing.getMaxStackSize(),
        slot.getMaxStackSize(existing)
      );
    }

    private void transition(Stage next, String message) {
      stage = next;
      stageTicks = 0;
      context.reportProgress(progress(message));
    }

    private BotTaskProgress progress(String message) {
      return BotTaskProgress.newBuilder()
        .setMessage(message)
        .setCurrent(completedTrades)
        .setTotal(targetCount)
        .setFraction(Math.min(
          1.0,
          (double) completedTrades / targetCount
        ))
        .build();
    }

    private void complete() {
      if (closeWhenDone) {
        requirePlayer().closeContainer();
      }
      result.complete(VillagerTradeTaskResult.newBuilder()
        .addAllOutputs(outputs.stream()
          .map(MinecraftDomainMapper::item)
          .toList())
        .setCompletedTrades(completedTrades)
        .build());
    }

    @Override
    public boolean isDone() {
      return result.isDone();
    }

    @Override
    public ControlPriority priority() {
      return ControlPriority.HIGH;
    }

    @Override
    public Set<ControlResource> resources() {
      return RESOURCES;
    }

    @Override
    public void onStarted() {
      active = true;
    }

    @Override
    public void onSuspended() {
      active = false;
    }

    @Override
    public void onResumed() {
      active = true;
    }

    @Override
    public void onStopped(
      ControlStopReason reason,
      @Nullable Throwable cause
    ) {
      try {
        var menu = openedMenu;
        openedMenu = null;
        if (active) {
          active = false;
          var player = context.bot().minecraft().player;
          if (player != null
            && menu != null
            && player.containerMenu == menu
            && (closeWhenDone || !menu.getCarried().isEmpty())) {
            player.closeContainer();
          }
        }
      } finally {
        if (reason != ControlStopReason.COMPLETED && !result.isDone()) {
          result.cancel(true);
        }
      }
    }

    @Override
    public String description() {
      return "Trade with villager";
    }
  }

  private enum Stage {
    SELECT_OFFER,
    WAIT_FOR_RESULT,
    TAKE_RESULT,
    DEPOSIT_RESULT
  }
}
