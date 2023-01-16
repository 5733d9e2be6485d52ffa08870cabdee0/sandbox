package com.redhat.service.smartevents.manager.v1.metrics;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.v1.api.V1;
import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;
import com.redhat.service.smartevents.manager.core.metrics.BaseManagerMetricsServiceImpl;
import com.redhat.service.smartevents.manager.v1.models.ManagedResourceV1;

import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class MetricsServiceImpl extends BaseManagerMetricsServiceImpl<ManagedResourceV1> implements ManagerMetricsServiceV1 {

    @Override
    protected Tag getVersionTag() {
        return Tag.of(VERSION_TAG, V1.class.getSimpleName());
    }

    @Override
    protected boolean isOperationSuccessful(ManagedResourceV1 managedResource, MetricsOperation operation) {
        if (MetricsOperation.MANAGER_RESOURCE_PROVISION == operation || MetricsOperation.MANAGER_RESOURCE_UPDATE == operation) {
            return ManagedResourceStatusV1.READY == managedResource.getStatus();
        }

        return ManagedResourceStatusV1.DELETED == managedResource.getStatus();
    }

    @Override
    protected boolean isOperationFailed(ManagedResourceV1 managedResource) {
        return ManagedResourceStatusV1.FAILED == managedResource.getStatus();
    }

    @Override
    protected Duration calculateOperationDuration(ManagedResourceV1 managedResource, MetricsOperation operation) {
        switch (operation) {
            case MANAGER_RESOURCE_PROVISION:
                return Duration.between(managedResource.getSubmittedAt(), ZonedDateTime.now(ZoneOffset.UTC));
            case MANAGER_RESOURCE_UPDATE:
                return Duration.between(managedResource.getModifiedAt(), ZonedDateTime.now(ZoneOffset.UTC));
            case MANAGER_RESOURCE_DELETE:
                return Duration.between(managedResource.getDeletionRequestedAt(), ZonedDateTime.now(ZoneOffset.UTC));
            default:
                throw new IllegalStateException(String.format("Unable to calculate operation duration for MetricsOperation '%s'", operation));
        }
    }

}
