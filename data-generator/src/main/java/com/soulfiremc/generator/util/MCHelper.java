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
package com.soulfiremc.generator.util;

import io.netty.buffer.Unpooled;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.function.Consumer;

public class MCHelper {
  private MCHelper() {}

  public static ServerLevel getLevel() {
    return getServer().overworld();
  }

  @SuppressWarnings("deprecation")
  public static DedicatedServer getServer() {
    return (DedicatedServer) FabricLoader.getInstance().getGameInstance();
  }

  @SuppressWarnings("DataFlowIssue")
  public static GameTestHelper getGameTestHelper() {
    return new GameTestHelper(null) {
      @Override
      public @NotNull ServerLevel getLevel() {
        return MCHelper.getLevel();
      }
    };
  }

  public static <T extends Entity> T createEntity(EntityType<T> entityType) {
    if (entityType == EntityType.PLAYER) {
      return entityType.tryCast(MCHelper.getGameTestHelper().makeMockPlayer(GameType.DEFAULT_MODE));
    }

    return entityType.create(MCHelper.getLevel(), EntitySpawnReason.COMMAND);
  }

  public static String serializeToBase64(Consumer<RegistryFriendlyByteBuf> consumer) {
    var buf = Unpooled.buffer();
    var registryBuf = new RegistryFriendlyByteBuf(buf, MCHelper.getLevel().registryAccess());
    consumer.accept(registryBuf);
    var bytes = new byte[buf.readableBytes()];
    buf.readBytes(bytes);
    buf.release();
    return Base64.getEncoder().encodeToString(bytes);
  }
}
