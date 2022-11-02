package com.redhat.service.smartevents.manager.core.persistence;

import com.redhat.service.smartevents.infra.core.models.processors.ProcessorType;

public class EnumTypeProcessorTypeTest extends EnumTypeBaseTest<ProcessorType, EnumTypeProcessorType> {

    @Override
    protected EnumTypeProcessorType getType() {
        return new EnumTypeProcessorType();
    }

    @Override
    protected Class<ProcessorType> getEnumType() {
        return ProcessorType.class;
    }
}
