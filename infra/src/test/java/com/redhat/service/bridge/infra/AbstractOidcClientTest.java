package com.redhat.service.bridge.infra;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.auth.AbstractOidcClient;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.OidcTokensNotInitializedException;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClientException;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractOidcClientTest {

    private static final String NAME = "test-sso";
    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";
    private OidcClient oidcClient;
    private Tokens tokens;
    private TestOidcClient client;

    private class TestOidcClient extends AbstractOidcClient {

        public TestOidcClient(String name, OidcClients oidcClients, Duration ssoConnectionTimeout) {
            super(name, oidcClients, ssoConnectionTimeout);
        }

        @Override
        public void init(OidcClientConfig oidcClientConfig) {
            super.init(oidcClientConfig);
        }

        @Override
        public void checkAndRefresh() {
            super.checkAndRefresh();
        }

        @Override
        public String getToken() {
            return super.getToken();
        }
    }

    @BeforeEach
    void init() {

        OidcClients oidcClients = mock(OidcClients.class);
        oidcClient = mock(OidcClient.class);
        tokens = mock(Tokens.class);
        when(tokens.getAccessToken()).thenReturn(ACCESS_TOKEN);
        when(tokens.getRefreshToken()).thenReturn(REFRESH_TOKEN);
        when(oidcClient.getTokens()).thenReturn(Uni.createFrom().item(tokens));
        when(oidcClient.refreshTokens(any(String.class))).thenReturn(Uni.createFrom().item(tokens));
        when(oidcClients.newClient(any(OidcClientConfig.class))).thenReturn(Uni.createFrom().item(oidcClient));

        client = new TestOidcClient(NAME, oidcClients, AbstractOidcClient.SSO_CONNECTION_TIMEOUT);
    }

    @Test
    public void tokensNotInitialized() {
        assertThatExceptionOfType(OidcTokensNotInitializedException.class).isThrownBy(() -> client.getToken());
    }

    @Test
    public void tokensAreInizialized() {
        client.init(new OidcClientConfig());
        assertThat(client.getToken()).isEqualTo(ACCESS_TOKEN);
    }

    @Test
    public void expiredTokenAreRefreshed() {
        // Given
        client.init(new OidcClientConfig());
        when(tokens.isAccessTokenExpired()).thenReturn(true);

        // When
        client.checkAndRefresh();

        // Then
        verify(oidcClient, times(1)).refreshTokens(any(String.class));
    }

    @Test
    public void tokenIsInRefreshInterval() {
        // Given
        client.init(new OidcClientConfig());
        when(tokens.isAccessTokenWithinRefreshInterval()).thenReturn(true);

        // When
        client.checkAndRefresh();

        // Then
        verify(oidcClient, times(1)).refreshTokens(any(String.class));
    }

    @Test
    public void expiredRefreshTokenAreRenewed() {
        // Given
        client.init(new OidcClientConfig());
        when(tokens.isAccessTokenExpired()).thenReturn(true);
        when(oidcClient.refreshTokens(any(String.class))).thenThrow(new OidcClientException());

        // When
        client.checkAndRefresh();

        // Then
        verify(oidcClient, times(2)).getTokens();
    }
}
