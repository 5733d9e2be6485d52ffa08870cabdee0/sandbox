package com.redhat.service.smartevents.processor.actions;

import com.redhat.service.smartevents.infra.api.v1.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayBean;

public interface ActionInvokerBuilder extends GatewayBean {

    ActionInvoker build(ProcessorDTO processor, Action action);
}
