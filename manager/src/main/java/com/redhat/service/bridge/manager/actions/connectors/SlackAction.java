package com.redhat.service.bridge.manager.actions.connectors;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.actions.ActionTransformer;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

@ApplicationScoped
public class SlackAction implements ActionProvider,
        ConnectorAction {

    public static final String TYPE = "SlackAction";

    public static final String CHANNEL_PARAMETER = "channel";
    public static final String WEBHOOK_URL_PARAMETER = "webhookUrl";

    public static final String CONNECTOR_TYPE = "slack_sink_0.1";

    @Inject
    SlackActionValidator validator;

    @Inject
    SlackActionTransformer transformer;

    @Inject
    ObjectMapper mapper;

    @Override
    public String getConnectorType() {
        return CONNECTOR_TYPE;
    }

    @Override
    public JsonNode connectorPayload(BaseAction action) {
        Map<String, String> actionParameters = action.getParameters();

        String slackChannel = actionParameters.get(SlackAction.CHANNEL_PARAMETER);
        String webHookURL = actionParameters.get(SlackAction.WEBHOOK_URL_PARAMETER);
        String kafkaTopic = topicName(action);

        ObjectNode objectNode = mapper.createObjectNode();

        ObjectNode connectorMap = mapper.createObjectNode();
        connectorMap.set("channel", new TextNode(slackChannel));
        connectorMap.set("webhookUrl", new TextNode(webHookURL));

        objectNode.set("connector", connectorMap);

        ObjectNode kafkaMap = mapper.createObjectNode();
        kafkaMap.set("topic", new TextNode(kafkaTopic));

        objectNode.set("kafka", kafkaMap);

        return objectNode;
    }

    @Override
    public String topicName(BaseAction action) {
        return action.getParameters().get(KafkaTopicAction.TOPIC_PARAM);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public SlackActionValidator getParameterValidator() {
        return validator;
    }

    @Override
    public ActionTransformer getTransformer() {
        return transformer;
    }
}
