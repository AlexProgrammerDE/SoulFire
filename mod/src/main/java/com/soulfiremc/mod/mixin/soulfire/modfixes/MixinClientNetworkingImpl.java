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
package com.soulfiremc.mod.mixin.soulfire.modfixes;

import com.soulfiremc.server.api.metadata.MetadataKey;
import com.soulfiremc.server.bot.BotConnection;
import net.fabricmc.fabric.impl.networking.client.ClientConfigurationNetworkAddon;
import net.fabricmc.fabric.impl.networking.client.ClientNetworkingImpl;
import net.fabricmc.fabric.impl.networking.client.ClientPlayNetworkAddon;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ClientNetworkingImpl.class)
public class MixinClientNetworkingImpl {
  @Unique
  private static final MetadataKey<ClientPlayNetworkAddon> PLAY_ADDON_KEY = MetadataKey.of("soulfire", "client_play_addon_modfix", ClientPlayNetworkAddon.class);
  @Unique
  private static final MetadataKey<ClientConfigurationNetworkAddon> CONFIGURATION_ADDON_KEY = MetadataKey.of("soulfire", "client_configuration_addon_modfix", ClientConfigurationNetworkAddon.class);

  @Redirect(method = "*", at = @At(value = "FIELD", target = "Lnet/fabricmc/fabric/impl/networking/client/ClientNetworkingImpl;currentPlayAddon:Lnet/fabricmc/fabric/impl/networking/client/ClientPlayNetworkAddon;", opcode = Opcodes.GETSTATIC), remap = false)
  private static ClientPlayNetworkAddon getCurrentPlayAddon() {
    return BotConnection.CURRENT.get().metadata().get(PLAY_ADDON_KEY);
  }

  @Redirect(method = "*", at = @At(value = "FIELD", target = "Lnet/fabricmc/fabric/impl/networking/client/ClientNetworkingImpl;currentPlayAddon:Lnet/fabricmc/fabric/impl/networking/client/ClientPlayNetworkAddon;", opcode = Opcodes.PUTSTATIC), remap = false)
  private static void setCurrentPlayAddon(ClientPlayNetworkAddon addon) {
    var metadataHolder = BotConnection.CURRENT.get().metadata();
    if (addon == null) {
      metadataHolder.remove(PLAY_ADDON_KEY);
    } else {
      metadataHolder.set(PLAY_ADDON_KEY, addon);
    }
  }

  @Redirect(method = "*", at = @At(value = "FIELD", target = "Lnet/fabricmc/fabric/impl/networking/client/ClientNetworkingImpl;currentConfigurationAddon:Lnet/fabricmc/fabric/impl/networking/client/ClientConfigurationNetworkAddon;", opcode = Opcodes.GETSTATIC), remap = false)
  private static ClientConfigurationNetworkAddon getCurrentConfigurationAddon() {
    return BotConnection.CURRENT.get().metadata().get(CONFIGURATION_ADDON_KEY);
  }

  @Redirect(method = "*", at = @At(value = "FIELD", target = "Lnet/fabricmc/fabric/impl/networking/client/ClientNetworkingImpl;currentConfigurationAddon:Lnet/fabricmc/fabric/impl/networking/client/ClientConfigurationNetworkAddon;", opcode = Opcodes.PUTSTATIC), remap = false)
  private static void setCurrentConfigurationAddon(ClientConfigurationNetworkAddon addon) {
    var metadataHolder = BotConnection.CURRENT.get().metadata();
    if (addon == null) {
      metadataHolder.remove(CONFIGURATION_ADDON_KEY);
    } else {
      metadataHolder.set(CONFIGURATION_ADDON_KEY, addon);
    }
  }
}
