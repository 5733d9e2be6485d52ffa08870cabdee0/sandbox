package com.redhat.service.bridge.manager.resolvers;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.processor.actions.ActionBean;

public interface ActionResolver extends ActionBean {

    BaseAction resolve(BaseAction action, String customerId, String bridgeId, String processorId);
}
