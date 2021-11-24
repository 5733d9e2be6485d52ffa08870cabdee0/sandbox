package com.redhat.service.bridge.manager.actions;

import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

public interface VirtualActionProvider extends ActionProvider {

    @Override
    default ActionInvoker getActionInvoker(ProcessorDTO processor, BaseAction baseAction) {
        throw new UnsupportedOperationException("Can't invoke virtual action");
    }

    ActionTransformer getTransformer();
}
