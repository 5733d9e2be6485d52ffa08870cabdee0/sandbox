package com.redhat.service.bridge.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

public interface ActionProvider extends ActionAccepter {

    default ActionInvoker getActionInvoker(ProcessorDTO processor, BaseAction baseAction) {
        return null;
    }

    default boolean isConnectorAction() {
        return false;
    }
}
