package com.redhat.service.bridge.actions;

import com.redhat.service.bridge.infra.api.models.actions.BaseAction;

public interface ActionTransformer {

    /**
     * This is a helper transformer to be used whenever the action doesn't need
     * to be modified by the transformer (e.g. for invokable actions).
     */
    ActionTransformer IDENTITY = (action, bridgeId, customerId) -> action;

    BaseAction transform(BaseAction action, String bridgeId, String customerId);
}
