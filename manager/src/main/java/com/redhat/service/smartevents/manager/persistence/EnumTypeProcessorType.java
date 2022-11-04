package com.redhat.service.smartevents.manager.persistence;

import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

/**
 * Custom Type for {@link ProcessorType} enumeration.
 */
public class EnumTypeProcessorType extends EnumTypeBase<ProcessorType> {

    @Override
    protected Class<ProcessorType> getEnumClass() {
        return ProcessorType.class;
    }
}
