package com.redhat.service.smartevents.processor.sources.aws;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface AwsSqsSource extends GatewayBean {

    String TYPE = "AwsSqs";
    String AWS_REGION_PARAM = "aws_region";
    String AWS_QUEUE_URL_PARAM = "aws_queue_url";
    String AWS_ACCESS_KEY_PARAM = "aws_access_key";
    String AWS_SECRET_KEY_PARAM = "aws_secret_key";
    String AWS_ENDPOINT_URI = "aws_endpoint_uri";

    @Override
    default String getType() {
        return TYPE;
    }
}
