package com.redhat.service.rhose.processor.actions;

import com.redhat.service.rhose.infra.models.actions.BaseAction;
import com.redhat.service.rhose.infra.models.dto.ProcessorDTO;

public interface ActionInvokerBuilder extends ActionBean {

    ActionInvoker build(ProcessorDTO processor, BaseAction baseAction);
}
