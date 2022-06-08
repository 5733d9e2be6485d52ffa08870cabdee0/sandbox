package com.redhat.service.smartevents.processor.actions.slack;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

@ApplicationScoped
public class SlackActionValidator extends AbstractGatewayValidator<Action> implements SlackAction {

    @Inject
    public SlackActionValidator(ProcessorCatalogService processorCatalogService) {
        super(processorCatalogService);
    }
}
