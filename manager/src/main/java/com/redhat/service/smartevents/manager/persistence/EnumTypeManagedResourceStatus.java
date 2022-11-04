package com.redhat.service.smartevents.manager.persistence;

import com.redhat.service.smartevents.infra.models.ManagedResourceStatus;

/**
 * Custom Type for {@link ManagedResourceStatus} enumeration.
 */
public class EnumTypeManagedResourceStatus extends EnumTypeBase<ManagedResourceStatus> {

    @Override
    protected Class<ManagedResourceStatus> getEnumClass() {
        return ManagedResourceStatus.class;
    }
}
