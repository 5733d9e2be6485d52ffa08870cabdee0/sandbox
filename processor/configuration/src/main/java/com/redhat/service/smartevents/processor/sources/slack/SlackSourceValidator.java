package com.redhat.service.smartevents.processor.sources.slack;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

@ApplicationScoped
public class SlackSourceValidator extends AbstractGatewayValidator<Source> implements SlackSource {

    @Inject
    public SlackSourceValidator(ProcessorCatalogService processorCatalogService) {
        super(processorCatalogService);
    }
}
