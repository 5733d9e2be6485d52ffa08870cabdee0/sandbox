package com.redhat.service.bridge.processor.actions.common;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

public abstract class AbstractActionConnector implements ActionConnector {

    public static final String PROCESSORS_PARAMETER = "processors";
    public static final String LOG_PROCESSOR_PARENT_PARAMETER = "log";
    public static final String LOG_PROCESSOR_MULTILINE_PARAMETER = "multiLine";
    public static final String LOG_PROCESSOR_SHOWHEADERS_PARAMETER = "showHeaders";

    @ConfigProperty(name = "managed-connectors.log-enabled")
    boolean logEnabled;

    @Inject
    ObjectMapper mapper;

    protected abstract void addConnectorSpecificPayload(BaseAction action, ObjectNode definition);

    @Override
    public JsonNode connectorPayload(BaseAction action) {
        ObjectNode definition = mapper.createObjectNode();

        if (logEnabled) {
            ArrayNode processors = mapper.createArrayNode();
            processors.add(getLogProcessor());
            definition.set(PROCESSORS_PARAMETER, processors);
        }

        addConnectorSpecificPayload(action, definition);

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
