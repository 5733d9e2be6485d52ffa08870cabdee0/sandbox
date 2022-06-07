package com.redhat.service.smartevents.processor.actions.eventhubs;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

@ApplicationScoped
public class AzureEventHubsActionValidator extends AbstractGatewayValidator<Action> implements AzureEventHubsAction {

    @Inject
    public AzureEventHubsActionValidator(ProcessorCatalogService processorCatalogService) {
        super(processorCatalogService);
    }
}
