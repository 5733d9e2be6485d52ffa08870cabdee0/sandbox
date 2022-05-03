package com.redhat.service.smartevents.processor.actions.slack;

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
class SlackActionConnectorTest {

    private static final String CHANNEL = "channel";
    private static final String WEBHOOK_URL = "https://www.example.com/webhook";
    private static final String TOPIC_NAME = "topic";

    private static final String EXPECTED_PAYLOAD_JSON = "{" +
            "   \"slack_channel\":\"" + CHANNEL + "\"," +
            "   \"slack_webhook_url\":\"" + WEBHOOK_URL + "\"," +
            "   \"kafka_topic\":\"" + TOPIC_NAME + "\"," +
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
    SlackActionConnector connector;

    @Inject
    ObjectMapper mapper;

    @Test
    void testConnectorType() {
        assertThat(connector.getConnectorTypeId()).isEqualTo(SlackActionConnector.CONNECTOR_TYPE_ID);
    }

    @Test
    void testConnectorPayload() throws JsonProcessingException {
        JsonNode expectedPayload = mapper.readTree(EXPECTED_PAYLOAD_JSON);

        Action action = new Action();
        action.setType(SlackAction.TYPE);
        action.setParameters(Map.of(SlackAction.CHANNEL_PARAM, CHANNEL, SlackAction.WEBHOOK_URL_PARAM, WEBHOOK_URL));

        JsonNode payload = connector.connectorPayload(action, TOPIC_NAME);

        assertThat(payload).isEqualTo(expectedPayload);
    }
}
