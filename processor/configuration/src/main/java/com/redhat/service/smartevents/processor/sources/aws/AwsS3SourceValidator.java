package com.redhat.service.smartevents.processor.sources.aws;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.JsonSchemaService;

import static com.redhat.service.smartevents.processor.ValidatorUtils.notValidKey;

@ApplicationScoped
public class AwsS3SourceValidator extends AbstractGatewayValidator<Source> implements AwsS3Source {

    public static final String INVALID_BUCKET_NAME_OR_ARN_PARAMETER_MESSAGE = notValidKey(BUCKET_NAME_OR_ARN_PARAMETER);
    public static final String INVALID_REGION_PARAMETER_MESSAGE = notValidKey(REGION_PARAMETER);
    public static final String INVALID_ACCESS_KEY_PARAMETER_MESSAGE = notValidKey(ACCESS_KEY_PARAMETER);
    public static final String INVALID_SECRET_KEY_PARAMETER_MESSAGE = notValidKey(SECRET_KEY_PARAMETER);
    public static final String INVALID_IGNORE_BODY_PARAMETER_MESSAGE = notValidKey(IGNORE_BODY_PARAMETER);
    public static final String INVALID_DELETE_AFTER_READ_PARAMETER_MESSAGE = notValidKey(DELETE_AFTER_READ_PARAMETER);

    @Inject
    public AwsS3SourceValidator(JsonSchemaService jsonSchemaService) {
        super(jsonSchemaService);
    }
}
