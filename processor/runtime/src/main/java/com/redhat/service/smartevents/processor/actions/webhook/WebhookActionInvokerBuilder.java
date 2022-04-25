package com.redhat.service.smartevents.processor.actions.webhook;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.AbstractWebClientInvokerBuilder;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

@ApplicationScoped
public class WebhookActionInvokerBuilder extends AbstractWebClientInvokerBuilder implements WebhookAction {

    @Override
    public ActionInvoker build(ProcessorDTO processor, Action action) {
        String endpoint = Optional.ofNullable(action.getParameters().get(ENDPOINT_PARAM))
                .orElseThrow(() -> buildNoEndpointException(processor));
        return requiresTechnicalBearerToken(action)
                ? new WebhookActionInvoker(endpoint, webClient, getOidcClient())
                : new WebhookActionInvoker(endpoint, webClient);
    }

    private static GatewayProviderException buildNoEndpointException(ProcessorDTO processor) {
        String message = String.format("There is no endpoint specified in the parameters for Action on Processor '%s' on Bridge '%s'",
                processor.getId(), processor.getBridgeId());
        return new GatewayProviderException(message);
    }

    private static boolean requiresTechnicalBearerToken(Action action) {
        return Boolean.parseBoolean(action.getParameters().get(USE_TECHNICAL_BEARER_TOKEN_PARAM));
    }
}
