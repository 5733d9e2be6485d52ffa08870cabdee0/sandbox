package com.redhat.service.smartevents.processor.sources.slack;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.actions.Source;
import com.redhat.service.smartevents.processor.sources.AbstractSourceConnector;

@ApplicationScoped
public class SlackSourceConnector extends AbstractSourceConnector implements SlackSource {

    public static final String CONNECTOR_TYPE = "slack_source_0.1";
    public static final String CONNECTOR_CHANNEL_PARAMETER = "slack_channel";
    public static final String CONNECTOR_TOKEN_PARAMETER = "slack_token";
    public static final String CONNECTOR_TOPIC_PARAMETER = "kafka_topic";

    @Override
    public String getConnectorType() {
        return CONNECTOR_TYPE;
    }

    @Override
    protected void addConnectorSpecificPayload(Source source, String topicName, ObjectNode definition) {
        Map<String, String> sourceParameters = source.getParameters();

        String slackChannel = sourceParameters.get(CHANNEL_PARAM);
        String slackToken = sourceParameters.get(TOKEN_PARAM);

        definition.set(CONNECTOR_CHANNEL_PARAMETER, new TextNode(slackChannel));
        definition.set(CONNECTOR_TOKEN_PARAMETER, new TextNode(slackToken));
        definition.set(CONNECTOR_TOPIC_PARAMETER, new TextNode(topicName));
    }
}
