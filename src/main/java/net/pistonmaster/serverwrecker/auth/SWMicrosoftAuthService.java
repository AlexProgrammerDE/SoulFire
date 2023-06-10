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
package net.pistonmaster.serverwrecker.auth;

import net.pistonmaster.serverwrecker.proxy.SWProxy;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.mcauth.step.java.StepMCToken;
import net.raphimc.mcauth.step.msa.StepCredentialsMsaCode;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SWMicrosoftAuthService implements MCAuthService {
    private static CloseableHttpClient createHttpClient(SWProxy proxyData) {
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en"));
        headers.add(new BasicHeader(HttpHeaders.USER_AGENT, "MinecraftAuth/2.0.0"));

        return HttpHelper.createHttpClient(headers, proxyData);
    }

    public JavaAccount login(String email, String password, SWProxy proxyData) throws IOException {
        try (CloseableHttpClient httpClient = createHttpClient(proxyData)) {
            StepMCProfile.MCProfile mcProfile = MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(httpClient,
                    new StepCredentialsMsaCode.MsaCredentials(email, password));
            StepMCToken.MCToken mcToken = mcProfile.prevResult().prevResult();
            return new JavaAccount(AuthType.MICROSOFT, mcProfile.name(), mcProfile.id(), mcToken.access_token(), mcToken.expireTimeMs(), true);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
