package net.pistonmaster.serverwrecker.auth;

import net.pistonmaster.serverwrecker.common.ProxyRequestData;
import net.raphimc.mcauth.MinecraftAuth;
import net.raphimc.mcauth.step.java.StepMCProfile;
import net.raphimc.mcauth.step.msa.StepCredentialsMsaCode;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

public class JavaAuthService implements MCAuthService {
    public JavaAccount login(String email, String password, ProxyRequestData proxyRequestData) throws IOException {
        try (CloseableHttpClient httpClient = createHttpClient(proxyRequestData)) {
            StepMCProfile.MCProfile mcProfile = MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(httpClient,
                    new StepCredentialsMsaCode.MsaCredentials(email, password));
            return new JavaAccount(mcProfile.name(), mcProfile.id(), mcProfile.prevResult().prevResult().access_token());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static CloseableHttpClient createHttpClient(ProxyRequestData proxyRequestData) {
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType()));
        headers.add(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en"));
        headers.add(new BasicHeader(HttpHeaders.USER_AGENT, "MinecraftAuth/2.0.0"));

        HttpClientBuilder httpBuilder = HttpClientBuilder.create()
                .setDefaultHeaders(headers);

        int timeout = 5;
        RequestConfig.Builder requestBuilder = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000);

        if (proxyRequestData != null) {
            HttpHost proxy = new HttpHost(proxyRequestData.getAddress().getHostName(), proxyRequestData.getAddress().getPort());

            if (proxyRequestData.hasCredentials()) {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(proxyRequestData.getUsername(), proxyRequestData.getPassword());

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
