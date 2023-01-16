package com.redhat.service.smartevents.manager.v2.metrics;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.manager.core.metrics.BaseManagerMetricsServiceImpl;
import com.redhat.service.smartevents.manager.v2.persistence.models.ManagedResourceV2;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;

import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class MetricsServiceImpl extends BaseManagerMetricsServiceImpl<ManagedResourceV2> implements ManagerMetricsServiceV2 {

    @Override
    protected Tag getVersionTag() {
        return Tag.of(VERSION_TAG, V2.class.getSimpleName());
    }

    @Override
    protected boolean isOperationSuccessful(ManagedResourceV2 managedResource, MetricsOperation operation) {
        ManagedResourceStatus status = StatusUtilities.getManagedResourceStatus(managedResource);
        if (MetricsOperation.MANAGER_RESOURCE_PROVISION == operation || MetricsOperation.MANAGER_RESOURCE_UPDATE == operation) {
            return ManagedResourceStatusV2.READY == status;
        }

        return ManagedResourceStatusV2.DELETED == status;
    }

    @Override
    protected boolean isOperationFailed(ManagedResourceV2 managedResource) {
        ManagedResourceStatus status = StatusUtilities.getManagedResourceStatus(managedResource);
        return ManagedResourceStatusV2.FAILED == status;
    }

    @Override
    protected Duration calculateOperationDuration(ManagedResourceV2 resource, MetricsOperation operation) {
        return Duration.between(resource.getOperation().getRequestedAt(), ZonedDateTime.now(ZoneOffset.UTC));
    }

}
