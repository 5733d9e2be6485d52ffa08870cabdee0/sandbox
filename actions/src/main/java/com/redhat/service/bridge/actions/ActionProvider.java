package com.redhat.service.bridge.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

public interface ActionProvider {

    default boolean accept(String actionType) {
        return getType().equals(actionType);
    }

    String getType();

    ActionParameterValidator getParameterValidator();

    ActionInvoker getActionInvoker(ProcessorDTO processor, BaseAction baseAction);

}
