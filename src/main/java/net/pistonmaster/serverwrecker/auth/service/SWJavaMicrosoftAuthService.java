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
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.mcauth.step.java.StepMCToken;
import net.raphimc.mcauth.step.msa.StepCredentialsMsaCode;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;

public class SWJavaMicrosoftAuthService implements MCAuthService {
    public MinecraftAccount login(String email, String password, SWProxy proxyData) throws IOException {
        try (CloseableHttpClient httpClient = HttpHelper.createMCAuthHttpClient(proxyData)) {
            StepMCProfile.MCProfile mcProfile = MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(httpClient,
                    new StepCredentialsMsaCode.MsaCredentials(email, password));
            StepMCToken.MCToken mcToken = mcProfile.prevResult().prevResult();
            return new MinecraftAccount(AuthType.MICROSOFT_JAVA, mcProfile.name(), new JavaData(mcProfile.id(), mcToken.access_token(), mcToken.expireTimeMs()), true);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
