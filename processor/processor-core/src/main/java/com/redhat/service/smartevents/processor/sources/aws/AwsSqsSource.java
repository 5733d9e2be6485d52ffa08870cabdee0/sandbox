package com.redhat.service.smartevents.processor.sources.aws;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface AwsSqsSource extends GatewayBean {

    String TYPE = "aws_sqs_source_0.1";
    String AWS_QUEUE_URL_PARAM = "aws_queue_name_or_arn";
    String AWS_REGION_PARAM = "aws_region";
    String AWS_ACCESS_KEY_ID_PARAM = "aws_access_key";
    String AWS_SECRET_ACCESS_KEY_PARAM = "aws_secret_key";

    @Override
    default String getType() {
        return TYPE;
    }
}
