package com.redhat.service.smartevents.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;

@ApplicationScoped
public class GatewaySecretsHandler {

    @Inject
    ProcessorCatalogService processorCatalogService;

    @Inject
    ObjectMapper mapper;

    public static JsonNode emptyObjectNode() {
        return new ObjectNode(JsonNodeFactory.instance);
    }

    public Pair<Gateway, Map<String, String>> mask(Gateway gateway) throws JsonProcessingException {
        List<String> passwordProps = gateway instanceof Action
                ? processorCatalogService.getActionPasswordProperties(gateway.getType())
                : processorCatalogService.getSourcePasswordProperties(gateway.getType());

        ObjectNode parameters = gateway.getParameters();
        Map<String, String> secrets = new HashMap<>();
        for (String passwordProperty : passwordProps) {
            if (parameters.has(passwordProperty)) {
                parameters.set(passwordProperty, emptyObjectNode());
            }
        }
        gateway.setParameters(parameters);

        return Pair.of(gateway, secrets);
    }

    public Gateway unmask(Gateway gateway, Map<String, String> secrets) throws JsonProcessingException {
        for (Map.Entry<String, String> secret : secrets.entrySet()) {
            gateway.getParameters().set(secret.getKey(), mapper.readTree(secret.getValue()));
        }
        return gateway;
    }

}
