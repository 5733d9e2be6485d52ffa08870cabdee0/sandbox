package com.redhat.service.bridge.manager.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.models.Bridge;

public interface ActionTransformer {

    default boolean accept(String actionType) {
        return getType().equals(actionType);
    }

    String getType();

    BaseAction transform(Bridge bridge, ProcessorRequest processorRequest);
}
