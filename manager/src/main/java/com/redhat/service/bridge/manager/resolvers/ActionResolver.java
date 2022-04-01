package com.redhat.service.bridge.manager.resolvers;

import com.redhat.service.bridge.actions.ActionAccepter;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

public interface ActionResolver extends ActionAccepter {

    BaseAction resolve(BaseAction action, String customerId, String bridgeId, String processorId);
}
