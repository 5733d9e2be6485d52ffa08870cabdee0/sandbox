package com.redhat.service.smartevents.processor.sources.slack;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.AbstractGatewayConnector;

@ApplicationScoped
public class SlackSourceConnector extends AbstractGatewayConnector<Source> implements SlackSource {

    public static final ConnectorType CONNECTOR_TYPE = ConnectorType.SOURCE;
    public static final String CONNECTOR_TYPE_ID = "slack_source_0.1";
    public static final String CONNECTOR_CHANNEL_PARAMETER = "slack_channel";
    public static final String CONNECTOR_TOKEN_PARAMETER = "slack_token";

    public SlackSourceConnector() {
        super(CONNECTOR_TYPE, CONNECTOR_TYPE_ID);
    }

    @Override
    protected void addConnectorSpecificPayload(Source source, String topicName, ObjectNode definition) {
        Map<String, String> sourceParameters = source.getParameters();

        definition.set(CONNECTOR_CHANNEL_PARAMETER, new TextNode(sourceParameters.get(CHANNEL_PARAM)));
        definition.set(CONNECTOR_TOKEN_PARAMETER, new TextNode(sourceParameters.get(TOKEN_PARAM)));
        definition.set(CONNECTOR_TOPIC_PARAMETER, new TextNode(topicName));
    }
}
