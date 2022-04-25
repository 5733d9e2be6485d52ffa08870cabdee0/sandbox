package com.redhat.service.smartevents.processor.actions;

import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.auth.AbstractOidcClient;
import com.redhat.service.smartevents.infra.auth.OidcClientConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.TechnicalBearerTokenNotConfiguredException;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public abstract class AbstractWebClientInvokerBuilder implements ActionInvokerBuilder {

    @Inject
    Instance<AbstractOidcClient> oidcClients;

    @Inject
    Vertx vertx;

    protected WebClient webClient;

    @PostConstruct
    private void onPostConstruct() {
        webClient = WebClient.create(vertx, new WebClientOptions().setLogActivity(true));
    }

    protected AbstractOidcClient getOidcClient() {
        return oidcClients.stream()
                .filter(x -> Objects.equals(x.getName(), OidcClientConstants.WEBHOOK_OIDC_CLIENT_NAME))
                .findFirst()
                .orElseThrow(() -> new TechnicalBearerTokenNotConfiguredException("A webhook action needed the webhook oidc client bean but it was not configured."));
    }
}
