package com.redhat.service.rhose.processor.actions;

import com.redhat.service.rhose.infra.models.actions.BaseAction;

public interface ActionResolver extends ActionBean {

    BaseAction resolve(BaseAction action, String customerId, String bridgeId, String processorId);
}
