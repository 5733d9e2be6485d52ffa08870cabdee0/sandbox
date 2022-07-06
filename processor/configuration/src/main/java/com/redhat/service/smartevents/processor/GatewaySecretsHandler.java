package com.redhat.service.smartevents.processor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;

@ApplicationScoped
public class GatewaySecretsHandler {

    @Inject
    ProcessorCatalogService processorCatalogService;

    public static ObjectNode emptyObjectNode() {
        return new ObjectNode(JsonNodeFactory.instance);
    }

    @SuppressWarnings("unchecked")
    public <T extends Gateway> Pair<T, ObjectNode> mask(T gateway) {
        List<String> passwordProps = gateway instanceof Action
                ? processorCatalogService.getActionPasswordProperties(gateway.getType())
                : processorCatalogService.getSourcePasswordProperties(gateway.getType());

        T gatewayCopy = (T) gateway.deepCopy();
        ObjectNode parameters = gatewayCopy.getParameters();
        ObjectNode secrets = emptyObjectNode();
        for (String passwordProperty : passwordProps) {
            if (parameters.has(passwordProperty)) {
                secrets.set(passwordProperty, parameters.get(passwordProperty));
                parameters.set(passwordProperty, emptyObjectNode());
            }
        }
        gatewayCopy.setParameters(parameters);

        return Pair.of(gatewayCopy, secrets);
    }

    @SuppressWarnings("unchecked")
    public <T extends Gateway> T unmask(T gateway, ObjectNode secrets) {
        Iterator<Map.Entry<String, JsonNode>> it = secrets.fields();
        T gatewayCopy = (T) gateway.deepCopy();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> secretEntry = it.next();
            if (!gatewayCopy.getParameters().has(secretEntry.getKey()) || emptyObjectNode().equals(gatewayCopy.getParameters().get(secretEntry.getKey()))) {
                gatewayCopy.setParameter(secretEntry.getKey(), secretEntry.getValue());
            }
        }
        return gatewayCopy;
    }

}
