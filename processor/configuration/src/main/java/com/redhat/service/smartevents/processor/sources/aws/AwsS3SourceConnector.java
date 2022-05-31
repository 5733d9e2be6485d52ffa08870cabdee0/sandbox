package com.redhat.service.smartevents.processor.sources.aws;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.AbstractGatewayConnector;

import static com.fasterxml.jackson.databind.node.BooleanNode.valueOf;
import static java.lang.Boolean.parseBoolean;

@ApplicationScoped
public class AwsS3SourceConnector extends AbstractGatewayConnector<Source> implements AwsS3Source {

    public static final ConnectorType CONNECTOR_TYPE = ConnectorType.SOURCE;
    public static final String CONNECTOR_TYPE_ID = "aws_s3_source_0.1";

    public static final String S3_AWS_BUCKET_NAME_OR_ARN_PARAMETER = "aws_bucket_name_or_arn";
    public static final String S3_AWS_REGION_PARAMETER = "aws_region";
    public static final String S3_AWS_PREFIX_PARAMETER = "aws_prefix";
    public static final String S3_AWS_ACCESS_KEY_PARAMETER = "aws_access_key";
    public static final String S3_AWS_SECRET_KEY_PARAMETER = "aws_secret_key";
    public static final String S3_AWS_IGNORE_BODY_PARAMETER = "aws_ignore_body";
    public static final String S3_AWS_DELETE_AFTER_READ_PARAMETER = "aws_delete_after_read";

    public AwsS3SourceConnector() {
        super(CONNECTOR_TYPE, CONNECTOR_TYPE_ID);
    }

    @Override
    protected void addConnectorSpecificPayload(Source source, String topicName, ObjectNode definition) {
        definition.set(CONNECTOR_TOPIC_PARAMETER, new TextNode(topicName));

        definition.set(S3_AWS_BUCKET_NAME_OR_ARN_PARAMETER, new TextNode(source.getParameter(BUCKET_NAME_OR_ARN_PARAMETER)));
        definition.set(S3_AWS_REGION_PARAMETER, new TextNode(source.getParameter(REGION_PARAMETER)));
        definition.set(S3_AWS_ACCESS_KEY_PARAMETER, new TextNode(source.getParameter(ACCESS_KEY_PARAMETER)));
        definition.set(S3_AWS_SECRET_KEY_PARAMETER, new TextNode(source.getParameter(SECRET_KEY_PARAMETER)));
        definition.set(S3_AWS_IGNORE_BODY_PARAMETER, valueOf(parseBoolean(source.getParameter(IGNORE_BODY_PARAMETER))));
        definition.set(S3_AWS_DELETE_AFTER_READ_PARAMETER, valueOf(parseBoolean(source.getParameter(DELETE_AFTER_READ_PARAMETER))));

        String prefix = source.getParameter(PREFIX);
        if (prefix != null && !prefix.isEmpty()) {
            definition.set(S3_AWS_PREFIX_PARAMETER, new TextNode(prefix));
        }
    }
}
