package com.redhat.service.smartevents.processor;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;

@ApplicationScoped
public class GatewaySecretsHandler {

    @Inject
    ProcessorCatalogService processorCatalogService;

    public static JsonNode emptyObjectNode() {
        return new ObjectNode(JsonNodeFactory.instance);
    }

    public Action mask(Action action) {
        List<String> passwordProps = processorCatalogService.getActionPasswordProperties(action.getType());
        return mask(action, passwordProps);
    }

    private Action mask(Action action, List<String> passwordProps) {
        ObjectNode parameters = action.getParameters();
        for (String passwordProperty : passwordProps) {
            if (parameters.has(passwordProperty)) {
                parameters.set(passwordProperty, emptyObjectNode());
            }
        }
        action.setParameters(parameters);
        return action;
    }

}
