package com.fwmotion.threescale.cms;

import com.fwmotion.threescale.cms.support.ApiClientBuilder;
import com.redhat.threescale.rest.cms.XmlEnabledApiClient;
import com.redhat.threescale.rest.cms.auth.ApiKeyAuth;
import com.redhat.threescale.rest.cms.auth.Authentication;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class ThreescaleCmsClientFactory implements AutoCloseable {

    private String baseUrl;
    private boolean useInsecureConnections;
    private String providerKey;
    private String accessToken;

    private CloseableHttpClient httpClient;

    private void tryCloseHttpClient() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                // TODO: Create ThreescaleCmsException and throw it instead of IllegalStateException
                throw new IllegalStateException("Couldn't close old HTTP client", e);
            }
        }
        httpClient = null;
    }

    private CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            if (isUseInsecureConnections()) {
                try {
                    httpClient = HttpClients.custom()
                        .setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContexts.custom()
                            .loadTrustMaterial(null, new TrustAllStrategy())
                            .build(),
                            NoopHostnameVerifier.INSTANCE))
                        .build();
                } catch (NoSuchAlgorithmException | KeyManagementException |
                         KeyStoreException e) {
                    // TODO: Create ThreescaleCmsException and throw it instead of IllegalStateException
                    throw new IllegalStateException("Unable to create insecure HttpClient", e);
                }
            } else {
                httpClient = HttpClients.createDefault();
            }
        }

        return httpClient;
    }

    private XmlEnabledApiClient newApiClient() {
        XmlEnabledApiClient apiClient = ApiClientBuilder.buildApiClient(getHttpClient());

        apiClient.setBasePath(baseUrl);

        if (providerKey != null) {
            Authentication providerKeyAuth = apiClient.getAuthentication("provider_key");
            ((ApiKeyAuth) providerKeyAuth).setApiKey(providerKey);
        } else if (accessToken != null) {
            Authentication accessTokenAuth = apiClient.getAuthentication("access_token");
            ((ApiKeyAuth) accessTokenAuth).setApiKey(accessToken);
        } else {
            // TODO: Create ThreescaleCmsException and throw it instead of IllegalStateException
            throw new IllegalStateException("Authentication not set for 3scale CMS client; must provide one of: providerKey, accessToken");
        }

        return apiClient;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isUseInsecureConnections() {
        return useInsecureConnections;
    }

    public void setUseInsecureConnections(boolean useInsecureConnections) {
        if (this.useInsecureConnections != useInsecureConnections) {
            tryCloseHttpClient();
        }
        this.useInsecureConnections = useInsecureConnections;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
        this.accessToken = null;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        this.providerKey = null;
    }

    @Nonnull
    public ThreescaleCmsClient getThreescaleCmsClient() {
        return new ThreescaleCmsClientImpl(newApiClient());
    }

    @Override
    public void close() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
