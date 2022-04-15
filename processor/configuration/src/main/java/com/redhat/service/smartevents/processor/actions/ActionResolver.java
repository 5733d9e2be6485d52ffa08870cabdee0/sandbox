package com.redhat.service.smartevents.processor.actions;

import com.redhat.service.smartevents.infra.models.gateways.Action;

public interface ActionResolver extends ActionBean {

    Action resolve(Action action, String customerId, String bridgeId, String processorId);
}
