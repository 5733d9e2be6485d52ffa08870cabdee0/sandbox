package com.redhat.service.smartevents.processor.actions;

import com.redhat.service.smartevents.infra.models.actions.BaseAction;

public interface ActionResolver extends ActionBean {

    BaseAction resolve(BaseAction action, String customerId, String bridgeId, String processorId);
}
