package com.redhat.service.smartevents.processor.actions.aws;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface AwsLambdaAction extends GatewayBean {

    String TYPE = "aws_lambda_sink_0.1";

    @Override
    default String getType() {
        return TYPE;
    }
}
