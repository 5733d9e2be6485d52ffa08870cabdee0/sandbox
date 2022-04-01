package com.redhat.service.bridge.manager.resolvers;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.processor.actions.common.ActionAccepter;

public interface ActionResolver extends ActionAccepter {

    BaseAction resolve(BaseAction action, String customerId, String bridgeId, String processorId);
}
