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

public final class SWJavaMicrosoftAuthService implements MCAuthService<SWJavaMicrosoftAuthService.JavaMicrosoftAuthData> {
    @Override
    public MinecraftAccount login(JavaMicrosoftAuthData data, SWProxy proxyData) throws IOException {
        try {
            var fullJavaSession = MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(HttpHelper.createLenniMCAuthHttpClient(proxyData),
                    new StepCredentialsMsaCode.MsaCredentials(data.email, data.password));
            var mcProfile = fullJavaSession.getMcProfile();
            var mcToken = mcProfile.getMcToken();
            return new MinecraftAccount(AuthType.MICROSOFT_JAVA, mcProfile.getName(), new OnlineJavaData(mcProfile.getId(), mcToken.getAccessToken(), mcToken.getExpireTimeMs()), true);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public JavaMicrosoftAuthData createData(String data) {
        var split = data.split(":");

        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid data!");
        }

        var email = split[0].trim();
        var password = split[1].trim();
        if (!EmailValidator.getInstance().isValid(email)) {
            throw new IllegalArgumentException("Invalid email!");
        }

        return new JavaMicrosoftAuthData(email, password);
    }

    public record JavaMicrosoftAuthData(String email, String password) {
    }
}
