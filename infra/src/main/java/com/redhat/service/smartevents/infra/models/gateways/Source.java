package com.redhat.service.smartevents.infra.models.gateways;

import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

public class Source extends Gateway {
    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.SOURCE;
    }

    @Override
    public Source deepCopy() {
        return super.deepCopy(new Source());
    }
}
