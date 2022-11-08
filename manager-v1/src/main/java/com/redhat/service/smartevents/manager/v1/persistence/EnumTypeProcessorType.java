package com.redhat.service.smartevents.manager.v1.persistence;

import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.core.persistence.EnumTypeBase;

/**
 * Custom Type for {@link ProcessorType} enumeration.
 */
public class EnumTypeProcessorType extends EnumTypeBase<ProcessorType> {

    @Override
    protected Class<ProcessorType> getEnumClass() {
        return ProcessorType.class;
    }
}
