/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.auth.service;

import net.pistonmaster.serverwrecker.auth.AuthType;
import net.pistonmaster.serverwrecker.auth.HttpHelper;
import net.pistonmaster.serverwrecker.auth.MinecraftAccount;
import net.pistonmaster.serverwrecker.proxy.SWProxy;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.bedrock.StepMCChain;
import net.raphimc.mcauth.step.msa.StepCredentialsMsaCode;
import net.raphimc.mcauth.step.xbl.StepXblXstsToken;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.UUID;

public class SWBedrockMicrosoftAuthService implements MCAuthService {
    public MinecraftAccount login(String email, String password, SWProxy proxyData) throws IOException {
        try (CloseableHttpClient httpClient = HttpHelper.createMCAuthHttpClient(proxyData)) {
            StepMCChain.MCChain mcChain = MinecraftAuth.BEDROCK_CREDENTIALS_LOGIN.getFromInput(httpClient,
                    new StepCredentialsMsaCode.MsaCredentials(email, password));

            StepXblXstsToken.XblXsts<?> xblXsts = mcChain.prevResult();
            UUID deviceId = xblXsts.initialXblSession().prevResult2().id();
            return new MinecraftAccount(AuthType.MICROSOFT_BEDROCK, mcChain.displayName(),
                    new BedrockData(
                            mcChain.mojangJwt(),
                            mcChain.identityJwt(),
                            mcChain.publicKey(),
                            mcChain.privateKey(),
                            deviceId,
                            "" // PlayFab token is pretty much never verified
                    ),
                    true);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
