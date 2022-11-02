package com.redhat.service.smartevents.infra.core.models.gateways;

import com.redhat.service.smartevents.infra.core.models.processors.ProcessorType;

public class Source extends Gateway {
    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.SOURCE;
    }
}
