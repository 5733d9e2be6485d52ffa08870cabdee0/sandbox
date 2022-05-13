package com.redhat.service.smartevents.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.VaultSecret;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;

public abstract class AbstractGatewayConnector<T extends Gateway> implements GatewayConnector<T> {

    public static final String PROCESSORS_PARAMETER = "processors";
    public static final String LOG_PROCESSOR_PARENT_PARAMETER = "log";
    public static final String LOG_PROCESSOR_MULTILINE_PARAMETER = "multiLine";
    public static final String LOG_PROCESSOR_SHOWHEADERS_PARAMETER = "showHeaders";

    @ConfigProperty(name = "managed-connectors.log-enabled")
    boolean logEnabled;

    @Inject
    ObjectMapper mapper;

    private final ConnectorType connectorType;
    private final String connectorTypeId;

    protected AbstractGatewayConnector(ConnectorType connectorType, String connectorTypeId) {
        this.connectorType = connectorType;
        this.connectorTypeId = connectorTypeId;
    }

    protected abstract void addConnectorSpecificPayload(T gateway, String topicName, Map<String, String> sensitiveParameters, ObjectNode definition);

    protected abstract boolean expectsSensitiveParameters();

    @Override
    public ConnectorType getConnectorType() {
        return connectorType;
    }

    @Override
    public String getConnectorTypeId() {
        return connectorTypeId;
    }

    @Override
    public JsonNode connectorPayload(T gateway, String topicName, Optional<VaultSecret> vaultSecret) {
        ObjectNode definition = mapper.createObjectNode();

        if (logEnabled) {
            ArrayNode processors = mapper.createArrayNode();
            processors.add(getLogProcessor());
            definition.set(PROCESSORS_PARAMETER, processors);
        }

        Map<String, String> sensitiveParameters;
        if (expectsSensitiveParameters()) {
            // TODO - tidy up exception message
            VaultSecret secret = vaultSecret.orElseThrow(() -> new IllegalStateException("Expected Vault parameters are not present"));
            sensitiveParameters = secret.getValues();
        } else {
            sensitiveParameters = new HashMap<>();
        }

        addConnectorSpecificPayload(gateway, topicName, sensitiveParameters, definition);
        return definition;
    }

    private ObjectNode getLogProcessor() {
        ObjectNode logProcessorParams = mapper.createObjectNode();
        logProcessorParams.set(LOG_PROCESSOR_MULTILINE_PARAMETER, BooleanNode.TRUE);
        logProcessorParams.set(LOG_PROCESSOR_SHOWHEADERS_PARAMETER, BooleanNode.TRUE);

        ObjectNode logProcessor = mapper.createObjectNode();
        logProcessor.set(LOG_PROCESSOR_PARENT_PARAMETER, logProcessorParams);

        return logProcessor;
    }
}
