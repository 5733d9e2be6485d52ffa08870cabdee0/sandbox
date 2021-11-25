package com.redhat.service.bridge.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;

public interface ActionTransformer {

    ActionTransformer IDENTITY = (action, bridgeId, customerId) -> action;

    BaseAction transform(BaseAction action, String bridgeId, String customerId);
}
