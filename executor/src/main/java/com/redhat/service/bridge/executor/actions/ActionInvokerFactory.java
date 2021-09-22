package com.redhat.service.bridge.executor.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

public interface ActionInvokerFactory {

    boolean accepts(BaseAction baseAction);

    ActionInvoker build(ProcessorDTO processor, BaseAction baseAction);
}
