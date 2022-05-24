package com.redhat.service.smartevents.processor.actions.generic;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.models.gateways.Action;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class GenericConnectorActionTest {

    private static final String TEST_CHANNEL = "test-channel";
    private static final String TEST_WEBHOOK_URL = "https://www.example.com/webhook";
    private static final String TEST_TOPIC_NAME = "test-topic";

    private static final String EXPECTED_PAYLOAD_JSON = "{" +
            "   \"slack_channel\":\"" + TEST_CHANNEL + "\"," +
            "   \"slack_webhook_url\":\"" + TEST_WEBHOOK_URL + "\"," +
            "   \"kafka_topic\":\"" + TEST_TOPIC_NAME + "\"," +
            "   \"processors\": [" +
            "       {" +
            "           \"log\": {" +
            "               \"multiLine\":true," +
            "               \"showHeaders\":true" +
            "        }" +
            "     }" +
            "   ]" +
            "}";

    @Inject
    GenericConnectorAction connector;

    @Inject
    ObjectMapper mapper;

    @Test
    void testConnectorPayload() throws JsonProcessingException {
        JsonNode expectedPayload = mapper.readTree(EXPECTED_PAYLOAD_JSON);

        Action action = new Action();
        action.setType("Generic");
        Map<String, String> parametersMap = Map.of(
                "slack_channel", TEST_CHANNEL,
                "slack_webhook_url", TEST_WEBHOOK_URL);

        action.setMapParameters(parametersMap);

        JsonNode payload = connector.connectorPayload(action, TEST_TOPIC_NAME);

        assertThat(payload).isEqualTo(expectedPayload);
    }

}
