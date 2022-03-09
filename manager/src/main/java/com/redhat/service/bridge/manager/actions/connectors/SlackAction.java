package com.redhat.service.bridge.manager.actions.connectors;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.bridge.actions.ActionTransformer;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

@ApplicationScoped
public class SlackAction implements ConnectorAction {

    public static final String TYPE = "Slack";

    public static final String CHANNEL_PARAMETER = "channel";
    public static final String WEBHOOK_URL_PARAMETER = "webhookUrl";

    public static final String CONNECTOR_TYPE = "slack_sink_0.1";
    public static final String CONNECTOR_CHANNEL_PARAMETER = "slack_channel";
    public static final String CONNECTOR_WEBHOOK_URL_PARAMETER = "slack_webhook_url";
    public static final String CONNECTOR_TOPIC_PARAMETER = "kafka_topic";

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

        objectNode.set(CONNECTOR_CHANNEL_PARAMETER, new TextNode(slackChannel));
        objectNode.set(CONNECTOR_WEBHOOK_URL_PARAMETER, new TextNode(webHookURL));
        objectNode.set(CONNECTOR_TOPIC_PARAMETER, new TextNode(kafkaTopic));


        ObjectNode logProcessorParams = mapper.createObjectNode();
        logProcessorParams.set("multiLine", BooleanNode.TRUE);
        logProcessorParams.set("showHeaders", BooleanNode.TRUE);

        ObjectNode logProcessor = mapper.createObjectNode();
        logProcessor.set("log", logProcessorParams);

        ArrayNode processors = mapper.createArrayNode();
        processors.add(logProcessor);

        objectNode.set("processors", processors);


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
