package com.redhat.service.smartevents.processor.sources.aws;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.JsonSchemaService;

@ApplicationScoped
public class AwsS3SourceValidator extends AbstractGatewayValidator<Source> implements AwsS3Source {

    @Inject
    public AwsS3SourceValidator(JsonSchemaService jsonSchemaService) {
        super(jsonSchemaService);
    }
}
