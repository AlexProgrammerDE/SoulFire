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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.pistonmaster.serverwrecker.proxy.SWProxy;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpHelper {
    public static CloseableHttpClient createHttpClient(List<Header> headers, SWProxy proxyData) {
        HttpClientBuilder httpBuilder = HttpClientBuilder.create()
                .setDefaultHeaders(headers);

        int timeout = 5;
        RequestConfig.Builder requestBuilder = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000);

        if (proxyData != null) {
            HttpHost proxy = new HttpHost(proxyData.host(), proxyData.port());

            if (proxyData.hasCredentials()) {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(proxyData.username(), proxyData.password());

                AuthScope authScope = new AuthScope(proxy);

                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(authScope, credentials);

                httpBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            requestBuilder.setProxy(proxy);
        }

        httpBuilder.setDefaultRequestConfig(requestBuilder.build());

        return httpBuilder.build();
    }
}
