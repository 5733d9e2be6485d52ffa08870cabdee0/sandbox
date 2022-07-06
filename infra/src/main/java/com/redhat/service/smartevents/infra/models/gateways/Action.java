package com.redhat.service.smartevents.infra.models.gateways;

import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

public class Action extends Gateway {

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.SINK;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
