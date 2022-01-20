package com.redhat.service.bridge.actions.webhook;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.actions.InvokableActionProvider;
import com.redhat.service.bridge.infra.exceptions.definitions.platform.TechnicalBearerTokenNotConfiguredException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@ApplicationScoped
public class WebhookAction implements InvokableActionProvider {

    public static final String TYPE = "Webhook";
    public static final String ENDPOINT_PARAM = "endpoint";
    public static final String USE_TECHNICAL_BEARER_TOKEN = "useTechincalBearerToken";

    private WebClient client;

    @ConfigProperty(name = "event-bridge.webhook.technical-bearer-token")
    Optional<String> webhookTechnicalBearerToken;

    @Inject
    WebhookActionValidator validator;

    @Inject
    Vertx vertx;

    @PostConstruct
    private void onPostConstruct() {
        client = WebClient.create(vertx);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public WebhookActionValidator getParameterValidator() {
        return validator;
    }

    @Override
    public ActionInvoker getActionInvoker(ProcessorDTO processor, BaseAction baseAction) {
        String endpoint = Optional.ofNullable(baseAction.getParameters().get(ENDPOINT_PARAM))
                .orElseThrow(() -> buildNoEndpointException(processor));
        if (baseAction.getParameters().containsKey(USE_TECHNICAL_BEARER_TOKEN) && baseAction.getParameters().get(USE_TECHNICAL_BEARER_TOKEN).equals("true")) {
            if (!webhookTechnicalBearerToken.isPresent()) {
                throw new TechnicalBearerTokenNotConfiguredException("A webhook action needed the technical bearer token but it was not configured.");
            }
            return new WebhookInvoker(endpoint, client, webhookTechnicalBearerToken.get());
        }
        return new WebhookInvoker(endpoint, client);
    }

    private ActionProviderException buildNoEndpointException(ProcessorDTO processor) {
        String message = String.format("There is no endpoint specified in the parameters for Action on Processor '%s' on Bridge '%s'",
                processor.getId(), processor.getBridgeId());
        return new ActionProviderException(message);
    }
}
