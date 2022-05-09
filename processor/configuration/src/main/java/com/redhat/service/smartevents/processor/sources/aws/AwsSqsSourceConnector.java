package com.redhat.service.smartevents.processor.sources.aws;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.AbstractGatewayConnector;

@ApplicationScoped
public class AwsSqsSourceConnector extends AbstractGatewayConnector<Source> implements AwsSqsSource {

    public static final ConnectorType CONNECTOR_TYPE = ConnectorType.SOURCE;
    public static final String CONNECTOR_TYPE_ID = "aws_sqs_source_0.1";
    public static final String CONNECTOR_TOPIC_PARAMETER = "kafka_topic";
    public static final String CONNECTOR_AWS_REGION_PARAMETER = "aws_region";
    public static final String CONNECTOR_AWS_QUEUE_PARAMETER = "aws_queue_name_or_arn";
    public static final String CONNECTOR_AWS_QUEUE_URL_PARAMETER = "aws_queue_u_r_l";
    public static final String CONNECTOR_AWS_ACCESS_KEY_PARAMETER = "aws_access_key";
    public static final String CONNECTOR_AWS_SECRET_KEY_PARAMETER = "aws_secret_key";
    public static final String CONNECTOR_AWS_OVERRIDE_ENDPOINT_PARAMETER = "aws_override_endpoint";
    public static final String CONNECTOR_AWS_URI_ENDPOINT_OVERRIDE_PARAMETER = "aws_uri_endpoint_override";

    public AwsSqsSourceConnector() {
        super(CONNECTOR_TYPE, CONNECTOR_TYPE_ID);
    }

    @Override
    protected void addConnectorSpecificPayload(Source source, String topicName, ObjectNode definition) {
        Map<String, String> sourceParameters = source.getParameters();
        String[] queueChunks = sourceParameters.get(AWS_QUEUE_URL_PARAM).split("/");
        String queueName = queueChunks[queueChunks.length - 1];

        definition.set(CONNECTOR_TOPIC_PARAMETER, new TextNode(topicName));

        definition.set(CONNECTOR_AWS_QUEUE_PARAMETER, new TextNode(queueName));
        definition.set(CONNECTOR_AWS_QUEUE_URL_PARAMETER, new TextNode(sourceParameters.get(AWS_QUEUE_URL_PARAM)));

        definition.set(CONNECTOR_AWS_REGION_PARAMETER, new TextNode(sourceParameters.getOrDefault(AWS_REGION_PARAM, "us-east-1")));
        definition.set(CONNECTOR_AWS_ACCESS_KEY_PARAMETER, new TextNode(sourceParameters.getOrDefault(AWS_ACCESS_KEY_PARAM, "test")));
        definition.set(CONNECTOR_AWS_SECRET_KEY_PARAMETER, new TextNode(sourceParameters.getOrDefault(AWS_SECRET_KEY_PARAM, "test")));

        if (sourceParameters.containsKey(AWS_ENDPOINT_URI)) {
            definition.set(CONNECTOR_AWS_OVERRIDE_ENDPOINT_PARAMETER, BooleanNode.TRUE);
            definition.set(CONNECTOR_AWS_URI_ENDPOINT_OVERRIDE_PARAMETER, new TextNode(sourceParameters.get(AWS_ENDPOINT_URI)));
        }
    }
}
