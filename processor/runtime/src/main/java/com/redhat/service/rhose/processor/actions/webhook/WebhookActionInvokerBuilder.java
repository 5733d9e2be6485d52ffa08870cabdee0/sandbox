package com.redhat.service.rhose.processor.actions.webhook;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.rhose.infra.auth.AbstractOidcClient;
import com.redhat.service.rhose.infra.auth.OidcClientConstants;
import com.redhat.service.rhose.infra.exceptions.definitions.platform.TechnicalBearerTokenNotConfiguredException;
import com.redhat.service.rhose.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.rhose.infra.models.actions.BaseAction;
import com.redhat.service.rhose.infra.models.dto.ProcessorDTO;
import com.redhat.service.rhose.processor.actions.ActionInvoker;
import com.redhat.service.rhose.processor.actions.ActionInvokerBuilder;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class WebhookActionInvokerBuilder implements WebhookAction,
        ActionInvokerBuilder {

    private WebClient client;

    @Inject
    Instance<AbstractOidcClient> oidcClients;

    @Inject
    Vertx vertx;

    @PostConstruct
    private void onPostConstruct() {
        client = WebClient.create(vertx, new WebClientOptions().setLogActivity(true));
    }

    @Override
    public ActionInvoker build(ProcessorDTO processor, BaseAction baseAction) {
        String endpoint = Optional.ofNullable(baseAction.getParameters().get(ENDPOINT_PARAM))
                .orElseThrow(() -> buildNoEndpointException(processor));
        if (baseAction.getParameters().containsKey(USE_TECHNICAL_BEARER_TOKEN_PARAM)
                && baseAction.getParameters().get(USE_TECHNICAL_BEARER_TOKEN_PARAM).equals("true")) {
            AbstractOidcClient abstractOidcClient =
                    oidcClients.stream()
                            .filter(x -> Objects.equals(x.getName(), OidcClientConstants.WEBHOOK_OIDC_CLIENT_NAME))
                            .findFirst()
                            .orElseThrow(() -> new TechnicalBearerTokenNotConfiguredException("A webhook action needed the webhook oidc client bean but it was not configured."));
            return new WebhookActionInvoker(endpoint, client, abstractOidcClient);
        }
        return new WebhookActionInvoker(endpoint, client);
    }

    private ActionProviderException buildNoEndpointException(ProcessorDTO processor) {
        String message = String.format("There is no endpoint specified in the parameters for Action on Processor '%s' on Bridge '%s'",
                processor.getId(), processor.getBridgeId());
        return new ActionProviderException(message);
    }
}
