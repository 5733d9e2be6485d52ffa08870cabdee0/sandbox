package com.redhat.service.bridge.manager.resolvers;

import com.redhat.service.bridge.processor.actions.common.ActionAccepter;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

public interface ActionResolver extends ActionAccepter {

    BaseAction resolve(BaseAction action, String customerId, String bridgeId, String processorId);
}
