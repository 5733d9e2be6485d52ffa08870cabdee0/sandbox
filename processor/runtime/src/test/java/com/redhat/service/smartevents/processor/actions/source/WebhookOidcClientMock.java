package com.redhat.service.smartevents.processor.actions.source;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.auth.AbstractOidcClient;
import com.redhat.service.smartevents.infra.auth.OidcClientConstants;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClients;
import io.smallrye.mutiny.Uni;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ApplicationScoped
public class WebhookOidcClientMock extends AbstractOidcClient {

    private static final OidcClients OIDC_CLIENTS_MOCK = mock(OidcClients.class);

    static {
        when(OIDC_CLIENTS_MOCK.newClient(any())).thenReturn(Uni.createFrom().item(mock(OidcClient.class)));
    }

    public WebhookOidcClientMock() {
        super(OidcClientConstants.WEBHOOK_OIDC_CLIENT_NAME, OIDC_CLIENTS_MOCK);
    }

    @Override
    protected OidcClientConfig getOidcClientConfig() {
        return new OidcClientConfig();
    }

    @Override
    protected void scheduledLoop() {
    }
}
