package com.redhat.service.smartevents.manager.core.persistence;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;

public class EnumTypeManagedResourceStatusTest extends EnumTypeBaseTest<ManagedResourceStatus, EnumTypeManagedResourceStatus> {

    @Override
    protected EnumTypeManagedResourceStatus getType() {
        return new EnumTypeManagedResourceStatus();
    }

    @Override
    protected Class<ManagedResourceStatus> getEnumType() {
        return ManagedResourceStatus.class;
    }
}
