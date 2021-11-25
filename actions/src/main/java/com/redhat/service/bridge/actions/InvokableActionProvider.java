package com.redhat.service.bridge.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

public interface InvokableActionProvider extends ActionProvider {

    ActionInvoker getActionInvoker(ProcessorDTO processor, BaseAction baseAction);

    @Override
    default ActionTransformer getTransformer() {
        return ActionTransformer.IDENTITY;
    }
}
