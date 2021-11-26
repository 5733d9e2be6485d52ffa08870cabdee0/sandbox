package com.redhat.service.bridge.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

/**
 * Invokable actions can be directly invoked by the executor via their specific {@link ActionInvoker}.
 */
public interface InvokableActionProvider extends ActionProvider {

    ActionInvoker getActionInvoker(ProcessorDTO processor, BaseAction baseAction);

    /**
     * Usually invokable actions don't need to be transformed by the transformer.
     * That's why the default implementation of this method returns the identity transformer.
     */
    @Override
    default ActionTransformer getTransformer() {
        return ActionTransformer.IDENTITY;
    }
}
