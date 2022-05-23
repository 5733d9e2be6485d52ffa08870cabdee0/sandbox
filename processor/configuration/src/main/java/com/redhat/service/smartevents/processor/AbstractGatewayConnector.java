package com.redhat.service.smartevents.processor;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;

public abstract class AbstractGatewayConnector<T extends Gateway> implements GatewayConnector<T> {

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

    private final ConnectorType connectorType;
    private final String connectorTypeId;

    protected AbstractGatewayConnector(ConnectorType connectorType, String connectorTypeId) {
        this.connectorType = connectorType;
        this.connectorTypeId = connectorTypeId;
    }

    protected abstract void addConnectorSpecificPayload(T gateway, String topicName, ObjectNode definition);

    @Override
    public ConnectorType getConnectorType() {
        return connectorType;
    }

    @Override
    public String getConnectorTypeId() {
        return connectorTypeId;
    }

    @Override
    public JsonNode connectorPayload(T gateway, String topicName, String errorHandlerTopicName) {
        ObjectNode definition = mapper.createObjectNode();

        if (logEnabled) {
            ArrayNode processors = mapper.createArrayNode();
            processors.add(getLogProcessor());
            definition.set(PROCESSORS_PARAMETER, processors);
        }

        addConnectorSpecificPayload(gateway, topicName, definition);

        addErrorHandlerPayload(errorHandlerTopicName, definition);

        return definition;
    }

    protected void addErrorHandlerPayload(String errorHandlerTopicName, ObjectNode definition) {
        ObjectNode errorHandler = mapper.createObjectNode();
        ObjectNode deadLetterQueue = mapper.createObjectNode();
        errorHandler.set(CONNECTOR_ERROR_HANDLER_DLQ_PARAMETER, deadLetterQueue);
        deadLetterQueue.set(CONNECTOR_ERROR_HANDLER_DLQ_TOPIC_NAME_PARAMETER, new TextNode(errorHandlerTopicName));
        definition.set(CONNECTOR_ERROR_HANDLER_PARAMETER, errorHandler);
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
