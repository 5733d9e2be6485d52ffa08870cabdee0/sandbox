package com.redhat.service.smartevents.processor.actions.input;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.auth.AbstractOidcClient;
import com.redhat.service.smartevents.infra.auth.OidcClientConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.TechnicalBearerTokenNotConfiguredException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.ActionInvokerBuilder;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class InputActionInvokerBuilder implements InputAction, ActionInvokerBuilder {

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
    public ActionInvoker build(ProcessorDTO processor, Action action) {
        String endpoint = Optional.ofNullable(action.getParameters().get(ENDPOINT_PARAM))
                .orElseThrow(() -> buildNoEndpointException(processor));
        String cloudEventType = action.getParameters().get(CLOUD_EVENT_TYPE);

        AbstractOidcClient abstractOidcClient = oidcClients.stream()
                .filter(x -> Objects.equals(x.getName(), OidcClientConstants.WEBHOOK_OIDC_CLIENT_NAME))
                .findFirst()
                .orElseThrow(() -> new TechnicalBearerTokenNotConfiguredException("A webhook action needed the webhook oidc client bean but it was not configured."));

        return new InputActionInvoker(endpoint, cloudEventType, client, abstractOidcClient);
    }

    private ActionProviderException buildNoEndpointException(ProcessorDTO processor) {
        String message = String.format("There is no endpoint specified in the parameters for Action on Processor '%s' on Bridge '%s'",
                processor.getId(), processor.getBridgeId());
        return new ActionProviderException(message);
    }
}
