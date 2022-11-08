package com.redhat.service.smartevents.processor.actions;

import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayBean;

public interface ActionInvokerBuilder extends GatewayBean {

    ActionInvoker build(ProcessorDTO processor, Action action);
}
