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
package com.soulfiremc.mod.mixin.soulfire.botfixes;

import com.soulfiremc.server.bot.BotConnection;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/// Built-in registry holders are shared, but their default components can reference
/// dynamic registries received separately by each bot during configuration.
@Mixin(Holder.Reference.class)
public class MixinHolderComponents {
  @Shadow private DataComponentMap components;

  @Redirect(method = {"components", "areComponentsBound"}, at = @At(value = "FIELD",
    target = "Lnet/minecraft/core/Holder$Reference;components:Lnet/minecraft/core/component/DataComponentMap;",
    opcode = Opcodes.GETFIELD))
  private DataComponentMap readComponents(Holder.Reference<?> holder) {
    var connection = BotConnection.currentOptional().orElse(null);
    return connection == null ? components : connection.registryComponents().getOrDefault(holder, components);
  }

  @Redirect(method = "bindComponents", at = @At(value = "FIELD",
    target = "Lnet/minecraft/core/Holder$Reference;components:Lnet/minecraft/core/component/DataComponentMap;",
    opcode = Opcodes.PUTFIELD))
  private void bindComponents(Holder.Reference<?> holder, DataComponentMap value) {
    var connection = BotConnection.currentOptional().orElse(null);
    if (connection == null) {
      components = value;
    } else {
      connection.registryComponents().put(holder, value);
    }
  }
}
