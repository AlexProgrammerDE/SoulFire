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
package com.soulfiremc.mod.mixin.soulfire.modfixes;

import com.mojang.brigadier.CommandDispatcher;
import com.soulfiremc.server.api.metadata.MetadataKey;
import com.soulfiremc.server.bot.BotConnection;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ClientCommandInternals.class)
public class MixinClientCommandInternals {
  @Unique
  private static final MetadataKey<CommandDispatcher<FabricClientCommandSource>> ACTIVE_COMMAND_DISPATCHER_KEY = MetadataKey.of("soulfire", "active_command_dispatcher_modfix", CommandDispatcher.class);

  @Redirect(method = "*", at = @At(value = "FIELD", target = "Lnet/fabricmc/fabric/impl/command/client/ClientCommandInternals;activeDispatcher:Lcom/mojang/brigadier/CommandDispatcher;", opcode = Opcodes.GETSTATIC), remap = false)
  private static CommandDispatcher<FabricClientCommandSource> getActiveDispatcher() {
    return BotConnection.CURRENT.get().metadata().get(ACTIVE_COMMAND_DISPATCHER_KEY);
  }

  @Redirect(method = "*", at = @At(value = "FIELD", target = "Lnet/fabricmc/fabric/impl/command/client/ClientCommandInternals;activeDispatcher:Lcom/mojang/brigadier/CommandDispatcher;", opcode = Opcodes.PUTSTATIC), remap = false)
  private static void setActiveDispatcher(CommandDispatcher<FabricClientCommandSource> addon) {
    var metadataHolder = BotConnection.CURRENT.get().metadata();
    if (addon == null) {
      metadataHolder.remove(ACTIVE_COMMAND_DISPATCHER_KEY);
    } else {
      metadataHolder.set(ACTIVE_COMMAND_DISPATCHER_KEY, addon);
    }
  }
}
