package com.redhat.service.smartevents.processor;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Source;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class GatewayConnectorTest {

    @Inject
    GatewayConnector gatewayConnector;

    @Inject
    ObjectMapper mapper;

    @Test
    public void testSlackConnectorPayload() throws JsonProcessingException {
        String channel = "channel";
        String webhookURL = "https://www.example.com/webhook";
        String topicName = "topic";
        String payload = "{" +
                "   \"slack_channel\":\"" + channel + "\"," +
                "   \"slack_webhook_url\":\"" + webhookURL + "\"," +
                "   \"kafka_topic\":\"" + topicName + "\"," +
                "   \"processors\": [" +
                "       {" +
                "           \"log\": {" +
                "               \"multiLine\":true," +
                "               \"showHeaders\":true" +
                "        }" +
                "     }" +
                "   ]" +
                "}";

        JsonNode expectedPayload = mapper.readTree(payload);

        Action action = new Action();
        action.setType(SlackAction.TYPE);
        action.setMapParameters(Map.of(SlackAction.CHANNEL_PARAM, channel, SlackAction.WEBHOOK_URL_PARAM, webhookURL));

        JsonNode actualPayload = gatewayConnector.connectorPayload(action, topicName);

        assertThat(actualPayload).isEqualTo(expectedPayload);
    }

    @Test
    public void testSlackConnectorPayloadWithErrorHandler() throws JsonProcessingException {
        String channel = "channel";
        String token = "token";
        String topicName = "topic";
        String errorTopicName = "errorHandlerTopic";

        String payload = "{" +
                "  \"slack_channel\":\"" + channel + "\"," +
                "  \"slack_token\":\"" + token + "\"," +
                "  \"kafka_topic\":\"" + topicName + "\"," +
                "  \"processors\": [" +
                "    {" +
                "      \"log\": {" +
                "        \"multiLine\":true," +
                "        \"showHeaders\":true" +
                "      }" +
                "    }" +
                "  ], " +
                "  \"error_handler\": {" +
                "    \"dead_letter_queue\": {" +
                "      \"topic\": \"errorHandlerTopic\"" +
                "    }" +
                "  }" +
                "}";

        JsonNode expectedPayload = mapper.readTree(payload);

        Source source = new Source();
        source.setType(SlackSource.TYPE);
        source.setMapParameters(Map.of(SlackSource.CHANNEL_PARAM, channel, SlackSource.TOKEN_PARAM, token));

        JsonNode actualPayload = gatewayConnector.connectorPayload(source, topicName, errorTopicName);

        assertThat(actualPayload).isEqualTo(expectedPayload);
    }
}
