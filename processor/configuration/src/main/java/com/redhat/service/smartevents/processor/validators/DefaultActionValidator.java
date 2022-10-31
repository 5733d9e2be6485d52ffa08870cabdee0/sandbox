package com.redhat.service.smartevents.processor.validators;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.processor.ProcessorCatalogService;

@ApplicationScoped
public class DefaultActionValidator extends AbstractActionValidator {
    @Inject
    public DefaultActionValidator(ProcessorCatalogService processorCatalogService) {
        super(processorCatalogService);
    }
}
