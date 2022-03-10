package com.redhat.service.bridge.manager.actions.connectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.redhat.service.bridge.manager.actions.connectors.ConnectorAction.PROCESSORS_PARAMETER;

public class ConnectorActionUtils {

    public static final String LOG_PROCESSOR_PARENT_PARAMETER = "log";
    public static final String LOG_PROCESSOR_MULTILINE_PARAMETER = "multiLine";
    public static final String LOG_PROCESSOR_SHOWHEADERS_PARAMETER = "showHeaders";

    public static ObjectNode addLogProcessorToDefinition(ObjectMapper mapper, ObjectNode definition) {
        ArrayNode processors = definition.has(PROCESSORS_PARAMETER) && definition.get(PROCESSORS_PARAMETER).isArray()
                ? (ArrayNode) definition.get(PROCESSORS_PARAMETER)
                : mapper.createArrayNode();
        processors.add(getLogProcessor(mapper));
        definition.set(PROCESSORS_PARAMETER, processors);
        return definition;
    }

    public static ObjectNode getLogProcessor(ObjectMapper mapper) {
        ObjectNode logProcessorParams = mapper.createObjectNode();
        logProcessorParams.set(LOG_PROCESSOR_MULTILINE_PARAMETER, BooleanNode.TRUE);
        logProcessorParams.set(LOG_PROCESSOR_SHOWHEADERS_PARAMETER, BooleanNode.TRUE);

        ObjectNode logProcessor = mapper.createObjectNode();
        logProcessor.set(LOG_PROCESSOR_PARENT_PARAMETER, logProcessorParams);

        return logProcessor;
    }

    private ConnectorActionUtils() {
    }
}
