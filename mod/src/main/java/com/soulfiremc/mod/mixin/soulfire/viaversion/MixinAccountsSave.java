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
package com.soulfiremc.mod.mixin.soulfire.viaversion;

import com.soulfiremc.server.account.service.BedrockData;
import com.soulfiremc.server.bot.BotConnection;
import com.viaversion.viafabricplus.save.impl.AccountsSave;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AccountsSave.class)
public class MixinAccountsSave {
  @Inject(method = "refreshAndGetBedrockAccount", at = @At("HEAD"), remap = false, cancellable = true)
  private void refreshAndGetBedrockAccount(CallbackInfoReturnable<StepFullBedrockSession.FullBedrockSession> cir) {
    cir.setReturnValue(MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.fromJson(((BedrockData) BotConnection.CURRENT.get().minecraftAccount().accountData()).authChain()));
  }
}
