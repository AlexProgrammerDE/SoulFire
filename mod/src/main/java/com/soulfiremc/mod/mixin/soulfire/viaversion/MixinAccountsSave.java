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

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.soulfiremc.server.account.service.BedrockData;
import com.soulfiremc.server.bot.BotConnection;
import com.viaversion.viafabricplus.save.impl.AccountsSave;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AccountsSave.class)
public class MixinAccountsSave {
  @Unique
  private static StepFullBedrockSession.FullBedrockSession soulfire$fromJson(JsonObject json) {
    return MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.fromJson(json);
  }

  @WrapMethod(method = "refreshAndGetBedrockAccount")
  private StepFullBedrockSession.FullBedrockSession refreshAndGetBedrockAccount(Operation<StepFullBedrockSession.FullBedrockSession> original) {
    return soulfire$fromJson(((BedrockData) BotConnection.CURRENT.get().minecraftAccount().accountData()).authChain());
  }
}
