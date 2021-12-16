package com.redhat.service.bridge.manager.actions.connectors;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SlackActionTransformerTest {
    @Test
    public void transformToNeededParameters() {

        SlackActionTransformer slackActionTransformer = new SlackActionTransformer();

        BaseAction baseAction = new BaseAction();

        Map<String, String> parameters = new HashMap<>();

        parameters.put(SlackAction.CHANNEL_PARAMETER, "myChannel");
        parameters.put(SlackAction.WEBHOOK_URL_PARAMETER, "myWebhook");

        slackActionTransformer.topicName = "topicName";

        baseAction.setParameters(parameters);
        BaseAction transformedAction = slackActionTransformer.transform(baseAction, "", "", "");

        assertThat(transformedAction.getType()).isEqualTo(KafkaTopicAction.TYPE);

        Map<String, String> trasnformedActionParameter = transformedAction.getParameters();
        assertThat(trasnformedActionParameter.get(SlackAction.CHANNEL_PARAMETER)).isEqualTo("myChannel");
        assertThat(trasnformedActionParameter.get(SlackAction.WEBHOOK_URL_PARAMETER)).isEqualTo("myWebhook");
        assertThat(trasnformedActionParameter.get(KafkaTopicAction.TOPIC_PARAM)).isEqualTo("topicName");

    }
}
