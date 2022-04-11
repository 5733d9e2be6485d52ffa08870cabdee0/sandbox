package com.redhat.service.bridge.processor.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;

public interface ActionResolver extends ActionBean {

    BaseAction resolve(BaseAction action, String customerId, String bridgeId, String processorId);
}
