package com.redhat.service.bridge.processor.actions;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

public interface ActionInvokerBuilder extends ActionBean {

    ActionInvoker build(ProcessorDTO processor, BaseAction baseAction);
}
