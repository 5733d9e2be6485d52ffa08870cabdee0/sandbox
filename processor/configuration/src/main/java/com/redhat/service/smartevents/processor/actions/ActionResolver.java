package com.redhat.service.smartevents.processor.actions;

import com.redhat.service.smartevents.infra.models.actions.Action;

public interface ActionResolver extends ActionBean {

    Action resolve(Action action, String customerId, String bridgeId, String processorId);
}
