package com.redhat.service.smartevents.processor.sources.errorhandler;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.GatewayResolver;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

@ApplicationScoped
public class ErrorHandlerSourceResolver implements ErrorHandlerSource, GatewayResolver<Source> {

    @Override
    public Action resolve(Source source, String customerId, String bridgeId, String processorId) {
        Action resolvedAction = new Action();
        resolvedAction.setType(WebhookAction.TYPE);
        resolvedAction.setParameters(Map.of(WebhookAction.ENDPOINT_PARAM, source.getParameters().get(WebhookAction.ENDPOINT_PARAM)));

        return resolvedAction;
    }

}
