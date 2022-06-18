package com.redhat.service.smartevents.processor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.JsonSchema;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;

@ApplicationScoped
public class GatewaySecretsHandler {

    public static final String MASK_PATTERN = "*****";
    public static final Map<String, List<String>> PASSWORD_FIELDS_MAP = new HashMap<>();

    @Inject
    ProcessorCatalogService processorCatalogService;

    public Action mask(Action action) {
        JsonSchema schema = processorCatalogService.getActionJsonSchema(action.getType());
        return mask(action, schema);
    }

    public Source mask(Source source) {
        JsonSchema schema = processorCatalogService.getSourceJsonSchema(source.getType());
        return mask(source, schema);
    }

    private <T extends Gateway> T mask(T gateway, JsonSchema schema) {
        List<String> passwordFields = getPasswordFields(gateway.getType(), schema);
        ObjectNode parameters = gateway.getParameters();
        for (String passwordField : passwordFields) {
            if (parameters.has(passwordField)) {
                parameters.set(passwordField, new TextNode(MASK_PATTERN));
            }
        }
        gateway.setParameters(parameters);
        return gateway;
    }

    private List<String> getPasswordFields(String gatewayType, JsonSchema schema) {
        if (PASSWORD_FIELDS_MAP.containsKey(gatewayType)) {
            return PASSWORD_FIELDS_MAP.get(gatewayType);
        }

        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = schema.getSchemaNode().get("properties").fields();
        Stream<Map.Entry<String, JsonNode>> fieldsStream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(fieldsIterator, 0), false);

        List<String> passwordFields = fieldsStream
                .filter(GatewaySecretsHandler::isPasswordField)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableList());

        PASSWORD_FIELDS_MAP.put(gatewayType, passwordFields);

        return passwordFields;
    }

    private static boolean isPasswordField(Map.Entry<String, JsonNode> fieldEntry) {
        JsonNode fieldNode = fieldEntry.getValue();
        return fieldNode.has("format") && fieldNode.get("format").asText().equalsIgnoreCase("password");
    }

}
