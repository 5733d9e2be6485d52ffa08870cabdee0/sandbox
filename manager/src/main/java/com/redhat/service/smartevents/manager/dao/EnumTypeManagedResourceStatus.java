package com.redhat.service.smartevents.manager.dao;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;

/**
 * Custom Type for {@link ManagedResourceStatus} enumeration.
 */
public class EnumTypeManagedResourceStatus extends EnumTypeBase<ManagedResourceStatus> {

    @Override
    protected Class<ManagedResourceStatus> getEnumClass() {
        return ManagedResourceStatus.class;
    }
}
