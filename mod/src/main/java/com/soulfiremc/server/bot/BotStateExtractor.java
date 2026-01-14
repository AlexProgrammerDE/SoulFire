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
package com.soulfiremc.server.bot;

import com.soulfiremc.grpc.generated.*;
import net.minecraft.world.entity.player.Inventory;

/**
 * Utility class to extract runtime state from a BotConnection for the gRPC API.
 */
public final class BotStateExtractor {
  private BotStateExtractor() {}

  /**
   * Extract the runtime state of a bot.
   *
   * @param bot              The bot connection
   * @param includeInventory Whether to include inventory (more expensive)
   * @return The runtime state proto
   */
  public static BotRuntimeState extractRuntimeState(BotConnection bot, boolean includeInventory) {
    var builder = BotRuntimeState.newBuilder()
      .setBotId(bot.botId().toString())
      .setStateTimestampMs(System.currentTimeMillis());

    if (bot.isDisconnected()) {
      builder.setConnectionState(BotConnectionState.BOT_OFFLINE);
      return builder.build();
    }

    var mc = bot.minecraft();
    var player = mc.player;

    if (player == null) {
      builder.setConnectionState(BotConnectionState.BOT_CONNECTING);
      return builder.build();
    }

    // Player exists, we're at least connected
    if (mc.level == null) {
      builder.setConnectionState(BotConnectionState.BOT_CONNECTED);
    } else {
      builder.setConnectionState(BotConnectionState.BOT_PLAYING);
    }

    // Set connected time if available
    if (bot.connectedSinceMs() > 0) {
      builder.setConnectedSinceMs(bot.connectedSinceMs());
    }

    // Position
    builder.setPosition(Vec3d.newBuilder()
      .setX(player.getX())
      .setY(player.getY())
      .setZ(player.getZ())
      .build());

    // Rotation (POV)
    builder.setRotation(Rotation.newBuilder()
      .setYaw(player.getYRot())
      .setPitch(player.getXRot())
      .build());

    // Health and food
    builder.setVitals(VitalStats.newBuilder()
      .setHealth(player.getHealth())
      .setMaxHealth(player.getMaxHealth())
      .setFood(player.getFoodData().getFoodLevel())
      .setSaturation(player.getFoodData().getSaturationLevel())
      .build());

    // Dimension
    if (player.level() != null) {
      builder.setDimension(player.level().dimension().identifier().toString());
    }

    // Experience
    builder.setExperienceLevel(player.experienceLevel);
    builder.setExperienceProgress(player.experienceProgress);

    // Movement state
    builder.setOnGround(player.onGround());
    builder.setIsSprinting(player.isSprinting());
    builder.setIsSneaking(player.isShiftKeyDown());

    // Game mode
    if (mc.gameMode != null) {
      builder.setGameMode(mc.gameMode.getPlayerMode().getName());
    }

    // Inventory (optional, expensive)
    if (includeInventory) {
      builder.setInventory(extractInventory(player.getInventory()));
    }

    return builder.build();
  }

  /**
   * Determine the connection state of a bot.
   */
  public static BotConnectionState getConnectionState(BotConnection bot) {
    if (bot.isDisconnected()) {
      return BotConnectionState.BOT_OFFLINE;
    }

    var mc = bot.minecraft();
    if (mc.player == null) {
      return BotConnectionState.BOT_CONNECTING;
    }

    if (mc.level == null) {
      return BotConnectionState.BOT_CONNECTED;
    }

    return BotConnectionState.BOT_PLAYING;
  }

  private static InventorySnapshot extractInventory(Inventory inv) {
    var builder = InventorySnapshot.newBuilder()
      .setSelectedSlot(inv.getSelectedSlot());

    for (int i = 0; i < inv.getContainerSize(); i++) {
      var stack = inv.getItem(i);
      if (!stack.isEmpty()) {
        var slotBuilder = InventorySlot.newBuilder()
          .setSlot(i)
          .setItemId(stack.getItemHolder().getRegisteredName())
          .setCount(stack.getCount());

        // Add display name if present
        if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
          var name = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
          if (name != null) {
            slotBuilder.setDisplayName(name.getString());
          }
        }

        builder.addSlots(slotBuilder.build());
      }
    }

    return builder.build();
  }
}
