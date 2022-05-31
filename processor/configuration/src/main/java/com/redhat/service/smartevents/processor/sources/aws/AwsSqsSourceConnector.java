package com.redhat.service.smartevents.processor.sources.aws;

import java.util.regex.Matcher;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.AbstractGatewayConnector;

import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidator.AWS_QUEUE_URL_PATTERN;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidator.GENERIC_QUEUE_URL_PATTERN;

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

    public static final String DEFAULT_AWS_REGION = "us-east-1";

    public AwsSqsSourceConnector() {
        super(CONNECTOR_TYPE, CONNECTOR_TYPE_ID);
    }

    @Override
    protected void addConnectorSpecificPayload(Source source, String topicName, ObjectNode definition) {
        String queueUrl = source.getParameter(AWS_QUEUE_URL_PARAM);

        definition.set(CONNECTOR_TOPIC_PARAMETER, new TextNode(topicName));

        definition.set(CONNECTOR_AWS_QUEUE_URL_PARAMETER, new TextNode(queueUrl));
        definition.set(CONNECTOR_AWS_ACCESS_KEY_PARAMETER, new TextNode(source.getParameter(AWS_ACCESS_KEY_ID_PARAM)));
        definition.set(CONNECTOR_AWS_SECRET_KEY_PARAMETER, new TextNode(source.getParameter(AWS_SECRET_ACCESS_KEY_PARAM)));

        Matcher awsQueueUrlMatcher = AWS_QUEUE_URL_PATTERN.matcher(queueUrl);
        if (awsQueueUrlMatcher.find()) {
            definition.set(CONNECTOR_AWS_REGION_PARAMETER, new TextNode(awsQueueUrlMatcher.group(1)));
            definition.set(CONNECTOR_AWS_QUEUE_PARAMETER, new TextNode(awsQueueUrlMatcher.group(2)));
        } else {
            Matcher genericQueueUrlMatcher = GENERIC_QUEUE_URL_PATTERN.matcher(queueUrl);
            if (genericQueueUrlMatcher.find()) {
                definition.set(CONNECTOR_AWS_REGION_PARAMETER, new TextNode(source.getParameterOrDefault(AWS_REGION_PARAM, DEFAULT_AWS_REGION)));
                definition.set(CONNECTOR_AWS_QUEUE_PARAMETER, new TextNode(genericQueueUrlMatcher.group(3)));
                definition.set(CONNECTOR_AWS_OVERRIDE_ENDPOINT_PARAMETER, BooleanNode.TRUE);
                definition.set(CONNECTOR_AWS_URI_ENDPOINT_OVERRIDE_PARAMETER, new TextNode(genericQueueUrlMatcher.group(1)));
            }
        }
    }
}
