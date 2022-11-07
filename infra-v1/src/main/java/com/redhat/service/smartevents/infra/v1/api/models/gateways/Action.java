package com.redhat.service.smartevents.infra.v1.api.models.gateways;

import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;

public class Action extends Gateway {

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.SINK;
    }
}
