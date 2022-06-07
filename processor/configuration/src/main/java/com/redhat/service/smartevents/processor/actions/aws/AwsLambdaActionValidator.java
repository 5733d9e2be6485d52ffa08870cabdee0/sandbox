package com.redhat.service.smartevents.processor.actions.aws;

import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

public class AwsLambdaActionValidator extends AbstractGatewayValidator<Action> implements AwsLambdaAction {

    @Inject
    public AwsLambdaActionValidator(ProcessorCatalogService processorCatalogService) {
        super(processorCatalogService);
    }
}
