package com.redhat.service.smartevents.processor.validators;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.processor.ProcessorCatalogService;

@ApplicationScoped
public class DefaultGatewayValidator extends AbstractGatewayValidator {
    @Inject
    public DefaultGatewayValidator(ProcessorCatalogService processorCatalogService) {
        super(processorCatalogService);
    }
}
