package com.redhat.service.smartevents.manager.dao;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;

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
