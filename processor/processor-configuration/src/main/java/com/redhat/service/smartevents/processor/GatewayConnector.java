package com.redhat.service.smartevents.processor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Gateway;

@ApplicationScoped
public class GatewayConnector {

    public static final String PROCESSORS_PARAMETER = "processors";
    public static final String LOG_PROCESSOR_PARENT_PARAMETER = "log";
    public static final String LOG_PROCESSOR_MULTILINE_PARAMETER = "multiLine";
    public static final String LOG_PROCESSOR_SHOWHEADERS_PARAMETER = "showHeaders";

    public static final String CONNECTOR_TOPIC_PARAMETER = "kafka_topic";
    public static final String CONNECTOR_ERROR_HANDLER_PARAMETER = "error_handler";
    public static final String CONNECTOR_ERROR_HANDLER_DLQ_PARAMETER = "dead_letter_queue";
    public static final String CONNECTOR_ERROR_HANDLER_DLQ_TOPIC_NAME_PARAMETER = "topic";

    @ConfigProperty(name = "managed-connectors.log-enabled")
    boolean logEnabled;

    @Inject
    ObjectMapper mapper;

    protected GatewayConnector() {
    }

    public JsonNode connectorPayload(Gateway gateway, String topicName) {
        ObjectNode definition = mapper.createObjectNode();

        if (logEnabled) {
            ArrayNode processors = mapper.createArrayNode();
            processors.add(getLogProcessor());
            definition.set(PROCESSORS_PARAMETER, processors);
        }

        definition.set(CONNECTOR_TOPIC_PARAMETER, new TextNode(topicName));
        definition.setAll(gateway.getParameters());

        return definition;
    }

    public JsonNode connectorPayload(Gateway gateway, String topicName, String errorHandlerTopicName) {
        ObjectNode definition = (ObjectNode) connectorPayload(gateway, topicName);

        addErrorHandlerPayload(errorHandlerTopicName, definition);

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

    private void addErrorHandlerPayload(String errorHandlerTopicName, ObjectNode definition) {
        ObjectNode errorHandler = mapper.createObjectNode();
        ObjectNode deadLetterQueue = mapper.createObjectNode();
        errorHandler.set(CONNECTOR_ERROR_HANDLER_DLQ_PARAMETER, deadLetterQueue);
        deadLetterQueue.set(CONNECTOR_ERROR_HANDLER_DLQ_TOPIC_NAME_PARAMETER, new TextNode(errorHandlerTopicName));
        definition.set(CONNECTOR_ERROR_HANDLER_PARAMETER, errorHandler);
    }
}
