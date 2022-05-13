package com.redhat.service.smartevents.processor.sources.aws;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface AwsSqsSource extends GatewayBean {

    String TYPE = "AwsSqs";
    String AWS_QUEUE_URL_PARAM = "aws_queue_url";
    String AWS_REGION_PARAM = "aws_region";
    String AWS_ACCESS_KEY_ID_PARAM = "aws_access_key_id";
    String AWS_SECRET_ACCESS_KEY_PARAM = "aws_secret_access_key";

    @Override
    default String getType() {
        return TYPE;
    }
}
