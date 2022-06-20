package com.redhat.service.smartevents.processor;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;

@ApplicationScoped
public class GatewaySecretsHandler {

    public static final String MASK_PATTERN = "***";

    @Inject
    ProcessorCatalogService processorCatalogService;

    public Action mask(Action action) {
        List<String> passwordProps = processorCatalogService.getActionPasswordProperties(action.getType());
        return mask(action, passwordProps);
    }

    public Source mask(Source source) {
        List<String> passwordProps = processorCatalogService.getSourcePasswordProperties(source.getType());
        return mask(source, passwordProps);
    }

    private <T extends Gateway> T mask(T gateway, List<String> passwordProps) {
        ObjectNode parameters = gateway.getParameters();
        for (String passwordProperty : passwordProps) {
            if (parameters.has(passwordProperty)) {
                parameters.set(passwordProperty, new TextNode(MASK_PATTERN));
            }
        }
        gateway.setParameters(parameters);
        return gateway;
    }

}
