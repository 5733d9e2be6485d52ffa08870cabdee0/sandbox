package com.redhat.service.smartevents.processor.sources.aws;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface AwsS3Source extends GatewayBean {

    String TYPE = "aws_s3_source_0.1";

    String BUCKET_NAME_OR_ARN_PARAMETER = "aws_bucket_name_or_arn";
    String REGION_PARAMETER = "aws_region";
    String ACCESS_KEY_PARAMETER = "aws_access_key";
    String SECRET_KEY_PARAMETER = "aws_secret_key";

    @Override
    default String getType() {
        return TYPE;
    }
}
