package com.redhat.service.bridge.processor.actions.slack;

import java.util.Map;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicAction;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.bridge.processor.actions.AbstractActionConnector.LOG_PROCESSOR_MULTILINE_PARAMETER;
import static com.redhat.service.bridge.processor.actions.AbstractActionConnector.LOG_PROCESSOR_PARENT_PARAMETER;
import static com.redhat.service.bridge.processor.actions.AbstractActionConnector.LOG_PROCESSOR_SHOWHEADERS_PARAMETER;
import static com.redhat.service.bridge.processor.actions.AbstractActionConnector.PROCESSORS_PARAMETER;
import static com.redhat.service.bridge.processor.actions.slack.SlackActionConnector.CONNECTOR_CHANNEL_PARAMETER;
import static com.redhat.service.bridge.processor.actions.slack.SlackActionConnector.CONNECTOR_TOPIC_PARAMETER;
import static com.redhat.service.bridge.processor.actions.slack.SlackActionConnector.CONNECTOR_WEBHOOK_URL_PARAMETER;

@QuarkusTest
class SlackActionTest {

    static final String EXPECTED_PROCESSORS_JSON = "\"" + PROCESSORS_PARAMETER + "\":[" +
            "  {" +
            "    \"" + LOG_PROCESSOR_PARENT_PARAMETER + "\": {" +
            "        \"" + LOG_PROCESSOR_MULTILINE_PARAMETER + "\":true," +
            "        \"" + LOG_PROCESSOR_SHOWHEADERS_PARAMETER + "\":true" +
            "    }" +
            "  }" +
            "]";

    @Inject
    SlackActionConnector slackActionConnector;

    @Test
    public void createSlackPayload() throws JsonProcessingException {
        final String channelValue = "channel";
        final String webhookUrlValue = "webhook_url";
        final String topicValue = "topic";

        BaseAction baseAction = new BaseAction();

        Map<String, String> parameters = baseAction.getParameters();
        parameters.put(SlackAction.CHANNEL_PARAM, channelValue);
        parameters.put(SlackAction.WEBHOOK_URL_PARAM, webhookUrlValue);
        parameters.put(KafkaTopicAction.TOPIC_PARAM, topicValue);
        JsonNode slackConnectorPayload = slackActionConnector.connectorPayload(baseAction, topicValue);

        JsonNode expected = new ObjectMapper().readTree("{" +
                "    \"" + CONNECTOR_CHANNEL_PARAMETER + "\":\"" + channelValue + "\"," +
                "    \"" + CONNECTOR_WEBHOOK_URL_PARAMETER + "\":\"" + webhookUrlValue + "\"," +
                "    \"" + CONNECTOR_TOPIC_PARAMETER + "\":\"" + topicValue + "\"," +
                "    " + EXPECTED_PROCESSORS_JSON +
                "}");

        Assertions.assertThat(slackConnectorPayload).isEqualTo(expected);
    }
}
