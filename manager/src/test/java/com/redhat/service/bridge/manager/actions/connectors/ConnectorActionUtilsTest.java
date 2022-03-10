package com.redhat.service.bridge.manager.actions.connectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.redhat.service.bridge.manager.actions.connectors.ConnectorAction.PROCESSORS_PARAMETER;
import static com.redhat.service.bridge.manager.actions.connectors.ConnectorActionUtils.LOG_PROCESSOR_MULTILINE_PARAMETER;
import static com.redhat.service.bridge.manager.actions.connectors.ConnectorActionUtils.LOG_PROCESSOR_PARENT_PARAMETER;
import static com.redhat.service.bridge.manager.actions.connectors.ConnectorActionUtils.LOG_PROCESSOR_SHOWHEADERS_PARAMETER;
import static com.redhat.service.bridge.manager.actions.connectors.ConnectorActionUtils.addLogProcessorToDefinition;
import static org.assertj.core.api.Assertions.assertThat;

class ConnectorActionUtilsTest {

    public static String EXPECTED_PROCESSORS_JSON = "\"" + PROCESSORS_PARAMETER + "\":[" +
            "  {" +
            "    \"" + LOG_PROCESSOR_PARENT_PARAMETER + "\": {" +
            "        \"" + LOG_PROCESSOR_MULTILINE_PARAMETER + "\":true," +
            "        \"" + LOG_PROCESSOR_SHOWHEADERS_PARAMETER + "\":true" +
            "    }" +
            "  }" +
            "]";

    @Test
    void testAddLogProcessorToDefinition() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode initialDefinition = mapper.createObjectNode();
        ObjectNode definition = addLogProcessorToDefinition(mapper, initialDefinition);
        JsonNode expected = mapper.readTree("{" + EXPECTED_PROCESSORS_JSON + "}");
        assertThat(definition).isEqualTo(expected);
    }

    @Test
    void testAddLogProcessorToDefinitionWithExistingProcessorsParameter() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode initialDefinition = (ObjectNode) mapper.readTree("{\"" + PROCESSORS_PARAMETER + "\":[]}");
        ObjectNode definition = addLogProcessorToDefinition(mapper, initialDefinition);
        JsonNode expected = mapper.readTree("{" + EXPECTED_PROCESSORS_JSON + "}");
        assertThat(definition).isEqualTo(expected);
    }
}
