package com.redhat.service.smartevents.manager.v2.persistence;

import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.manager.core.persistence.EnumTypeBase;

/**
 * Custom Type for {@link ManagedResourceStatusV2} enumeration.
 */
public class EnumTypeManagedResourceStatus extends EnumTypeBase<ManagedResourceStatusV2> {

    @Override
    protected Class<ManagedResourceStatusV2> getEnumClass() {
        return ManagedResourceStatusV2.class;
    }
}
