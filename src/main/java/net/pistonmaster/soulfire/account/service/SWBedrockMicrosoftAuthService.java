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
package net.pistonmaster.soulfire.account.service;

import net.pistonmaster.soulfire.account.AuthType;
import net.pistonmaster.soulfire.account.HttpHelper;
import net.pistonmaster.soulfire.account.MinecraftAccount;
import net.pistonmaster.soulfire.proxy.SWProxy;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import org.apache.commons.validator.routines.EmailValidator;

import java.io.IOException;

public final class SWBedrockMicrosoftAuthService implements MCAuthService<SWBedrockMicrosoftAuthService.BedrockMicrosoftAuthData> {
    @Override
    public MinecraftAccount login(BedrockMicrosoftAuthData data, SWProxy proxyData) throws IOException {
        try {
            var fullBedrockSession = MinecraftAuth.BEDROCK_CREDENTIALS_LOGIN.getFromInput(HttpHelper.createLenniMCAuthHttpClient(proxyData),
                    new StepCredentialsMsaCode.MsaCredentials(data.email, data.password));

            var mcChain = fullBedrockSession.getMcChain();
            var xblXsts = mcChain.getXblXsts();
            var deviceId = xblXsts.getInitialXblSession().getXblDeviceToken().getId();
            var playFabId = fullBedrockSession.getPlayFabToken().getPlayFabId();
            return new MinecraftAccount(AuthType.MICROSOFT_BEDROCK, mcChain.getDisplayName(),
                    new BedrockData(
                            mcChain.getMojangJwt(),
                            mcChain.getIdentityJwt(),
                            mcChain.getPublicKey(),
                            mcChain.getPrivateKey(),
                            deviceId,
                            playFabId
                    ),
                    true);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public BedrockMicrosoftAuthData createData(String data) {
        var split = data.split(":");

        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid data!");
        }

        var email = split[0].trim();
        var password = split[1].trim();
        if (!EmailValidator.getInstance().isValid(email)) {
            throw new IllegalArgumentException("Invalid email!");
        }

        return new BedrockMicrosoftAuthData(email, password);
    }

    public record BedrockMicrosoftAuthData(String email, String password) {
    }
}
