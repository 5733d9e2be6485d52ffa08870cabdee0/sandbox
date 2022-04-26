package com.redhat.service.smartevents.processor.actions.source;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.auth.AbstractOidcClient;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.AbstractWebClientInvokerBuilder;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

@ApplicationScoped
public class SourceActionInvokerBuilder extends AbstractWebClientInvokerBuilder implements SourceAction {

    @Override
    public ActionInvoker build(ProcessorDTO processor, Action action) {
        String endpoint = getOrThrowException(action, ENDPOINT_PARAM);
        String cloudEventType = getOrThrowException(action, CLOUD_EVENT_TYPE_PARAM);
        AbstractOidcClient abstractOidcClient = getOidcClient();
        return new SourceActionInvoker(endpoint, cloudEventType, webClient, abstractOidcClient);
    }

    private static String getOrThrowException(Action action, String parameterName) {
        if (action.getParameters() == null || !action.getParameters().containsKey(parameterName)) {
            throw new GatewayProviderException(String.format("Missing %s parameter from action", parameterName));
        }
        return action.getParameters().get(parameterName);
    }
}
