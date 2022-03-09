package com.redhat.service.bridge.manager.actions.connectors;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.bridge.manager.actions.connectors.SlackAction.CONNECTOR_CHANNEL_PARAMETER;
import static com.redhat.service.bridge.manager.actions.connectors.SlackAction.CONNECTOR_TOPIC_PARAMETER;
import static com.redhat.service.bridge.manager.actions.connectors.SlackAction.CONNECTOR_WEBHOOK_URL_PARAMETER;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SlackActionTest {

    @Inject
    SlackAction slackAction;

    @Test
    public void createSlackPayload() throws JsonProcessingException {
        final String channelValue = "channel";
        final String webhookUrlValue = "webhook_url";
        final String topicValue = "topic";

        BaseAction baseAction = new BaseAction();

        Map<String, String> parameters = baseAction.getParameters();
        parameters.put(SlackAction.CHANNEL_PARAMETER, channelValue);
        parameters.put(SlackAction.WEBHOOK_URL_PARAMETER, webhookUrlValue);
        parameters.put(KafkaTopicAction.TOPIC_PARAM, topicValue);
        JsonNode slackConnectorPayload = slackAction.connectorPayload(baseAction);

        JsonNode expected = new ObjectMapper().readTree("{" +
                "    \"" + CONNECTOR_CHANNEL_PARAMETER + "\":\"" + channelValue + "\"," +
                "    \"" + CONNECTOR_WEBHOOK_URL_PARAMETER + "\":\"" + webhookUrlValue + "\"," +
                "    \"" + CONNECTOR_TOPIC_PARAMETER + "\":\"" + topicValue + "\"," +
                "    \"processors\":[{\"log\":{\"multiLine\":true,\"showHeaders\":true}}]}" +
                "}");

        assertThat(slackConnectorPayload).isEqualTo(expected);
    }
}
