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

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SlackActionTest {

    @Inject
    SlackAction slackAction;

    @Test
    public void createSlackPayload() throws JsonProcessingException {
        BaseAction baseAction = new BaseAction();

        Map<String, String> parameters = baseAction.getParameters();
        parameters.put(SlackAction.CHANNEL_PARAMETER, "channel");
        parameters.put(SlackAction.WEBHOOK_URL_PARAMETER, "webhook_url");
        parameters.put(KafkaTopicAction.TOPIC_PARAM, "topic");
        JsonNode slackConnectorPayload = slackAction.connectorPayload(baseAction);

        JsonNode expected = new ObjectMapper().readTree("    {  \"connector\": {\n" +
                "         \"channel\": \"channel\",\n" +
                "         \"webhookUrl\": \"webhook_url\"\n" +
                "      },\n" +
                "      \"kafka\": {\n" +
                "         \"topic\": \"topic\"\n" +
                "      } }");

        assertThat(slackConnectorPayload).isEqualTo(expected);
    }
}
