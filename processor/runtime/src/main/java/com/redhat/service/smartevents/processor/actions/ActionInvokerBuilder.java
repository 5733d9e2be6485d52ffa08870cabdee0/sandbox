package com.redhat.service.smartevents.processor.actions;

import com.redhat.service.smartevents.infra.models.actions.BaseAction;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;

public interface ActionInvokerBuilder extends ActionBean {

    ActionInvoker build(ProcessorDTO processor, BaseAction baseAction);
}
