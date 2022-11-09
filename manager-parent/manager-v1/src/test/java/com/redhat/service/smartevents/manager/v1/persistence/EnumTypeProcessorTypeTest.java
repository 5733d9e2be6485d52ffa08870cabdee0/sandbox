package com.redhat.service.smartevents.manager.v1.persistence;

import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.core.persistence.EnumTypeBaseTest;

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
