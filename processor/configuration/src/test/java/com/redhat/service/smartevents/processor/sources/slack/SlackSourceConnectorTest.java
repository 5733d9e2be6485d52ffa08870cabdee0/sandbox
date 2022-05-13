package com.redhat.service.smartevents.processor.sources.slack;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.models.gateways.Source;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SlackSourceConnectorTest {

    private static final String CHANNEL = "channel";
    private static final String TOKEN = "token";
    private static final String TOPIC_NAME = "topic";

    private static final String EXPECTED_PAYLOAD_JSON = "{" +
            "   \"slack_channel\":\"" + CHANNEL + "\"," +
            "   \"slack_token\":\"" + TOKEN + "\"," +
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
    SlackSourceConnector connector;

    @Inject
    ObjectMapper mapper;

    @Test
    void testConnectorType() {
        assertThat(connector.getConnectorTypeId()).isEqualTo(SlackSourceConnector.CONNECTOR_TYPE_ID);
    }

    @Test
    void testConnectorPayload() throws JsonProcessingException {
        JsonNode expectedPayload = mapper.readTree(EXPECTED_PAYLOAD_JSON);

        Source source = new Source();
        source.setType(SlackSource.TYPE);
        source.setParameters(Map.of(SlackSource.CHANNEL_PARAM, CHANNEL, SlackSource.TOKEN_PARAM, TOKEN));

        JsonNode payload = connector.connectorPayload(source, TOPIC_NAME);

        assertThat(payload).isEqualTo(expectedPayload);
    }
}
