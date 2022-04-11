package com.redhat.service.bridge.processor.actions.slack;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.processor.actions.AbstractActionConnector;

@ApplicationScoped
public class SlackActionConnector extends AbstractActionConnector implements SlackActionBean {

    public static final String CONNECTOR_TYPE = "slack_sink_0.1";
    public static final String CONNECTOR_CHANNEL_PARAMETER = "slack_channel";
    public static final String CONNECTOR_WEBHOOK_URL_PARAMETER = "slack_webhook_url";
    public static final String CONNECTOR_TOPIC_PARAMETER = "kafka_topic";

    @Override
    public String getConnectorType() {
        return CONNECTOR_TYPE;
    }

    @Override
    protected void addConnectorSpecificPayload(BaseAction action, String topicName, ObjectNode definition) {
        Map<String, String> actionParameters = action.getParameters();

        String slackChannel = actionParameters.get(SlackActionBean.CHANNEL_PARAM);
        String webHookURL = actionParameters.get(SlackActionBean.WEBHOOK_URL_PARAM);

        definition.set(CONNECTOR_CHANNEL_PARAMETER, new TextNode(slackChannel));
        definition.set(CONNECTOR_WEBHOOK_URL_PARAMETER, new TextNode(webHookURL));
        definition.set(CONNECTOR_TOPIC_PARAMETER, new TextNode(topicName));
    }
}
