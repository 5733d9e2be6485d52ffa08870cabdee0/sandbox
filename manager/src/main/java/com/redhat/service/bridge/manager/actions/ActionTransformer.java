package com.redhat.service.bridge.manager.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;

public interface ActionTransformer {

    BaseAction transform(BaseAction action, String bridgeId, String customerId);
}
