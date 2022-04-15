package com.redhat.service.smartevents.processor.actions;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;

public interface ActionInvokerBuilder extends ActionBean {

    ActionInvoker build(ProcessorDTO processor, Action action);
}
