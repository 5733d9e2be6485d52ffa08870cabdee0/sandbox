package com.redhat.service.smartevents.manager.v1.persistence;

import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;
import com.redhat.service.smartevents.manager.core.persistence.EnumTypeBase;

/**
 * Custom Type for {@link ManagedResourceStatusV1} enumeration.
 */
public class EnumTypeManagedResourceStatus extends EnumTypeBase<ManagedResourceStatusV1> {

    @Override
    protected Class<ManagedResourceStatusV1> getEnumClass() {
        return ManagedResourceStatusV1.class;
    }
}
