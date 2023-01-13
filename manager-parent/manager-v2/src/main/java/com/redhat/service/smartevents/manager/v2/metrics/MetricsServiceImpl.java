package com.redhat.service.smartevents.manager.v2.metrics;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.manager.v2.persistence.models.ManagedResourceV2;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class MetricsServiceImpl implements ManagerMetricsServiceV2 {

    // Constant for the metric tag for the resource instance we're operating on e.g. Processor/Bridge.
    static final String RESOURCE_TAG = "resource";
    // Constant for the version tag of the resource instance we're operating on e.g. v1/v2.
    static final String VERSION_TAG = "version";

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "rhose.metrics-name.operation-total-count")
    String operationTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-success-total-count")
    String operationTotalSuccessCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-failure-total-count")
    String operationTotalFailureCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-duration-seconds")
    String operationDurationMetricName;

    @Override
    public Set<String> getMetricNames() {
        return Set.of(operationDurationMetricName, operationTotalCountMetricName, operationTotalSuccessCountMetricName, operationTotalFailureCountMetricName);
    }

    @Override
    public void onOperationStart(ManagedResourceV2 managedResource, MetricsOperation operation) {
        incrementCounter(operationTotalCountMetricName, buildTags(managedResource, operation));
    }

    private void incrementCounter(String counterName, List<Tag> tags) {
        meterRegistry.counter(counterName, tags).increment();
    }

    @Override
    public void onOperationComplete(ManagedResourceV2 managedResource, MetricsOperation operation) {
        if (isOperationSuccessful(managedResource, operation)) {
            incrementCounter(operationTotalSuccessCountMetricName, buildTags(managedResource, operation));
            recordOperationDuration(managedResource, operation);
        }
    }

    private boolean isOperationSuccessful(ManagedResourceV2 managedResource, MetricsOperation operation) {
        ManagedResourceStatus status = StatusUtilities.getManagedResourceStatus(managedResource);
        if (MetricsOperation.MANAGER_RESOURCE_PROVISION == operation || MetricsOperation.MANAGER_RESOURCE_UPDATE == operation) {
            return ManagedResourceStatusV2.READY == status;
        }

        return ManagedResourceStatusV2.DELETED == status;
    }

    @Override
    public void onOperationFailed(ManagedResourceV2 managedResource, MetricsOperation operation) {
        if (isOperationFailed(managedResource)) {
            incrementCounter(operationTotalFailureCountMetricName, buildTags(managedResource, operation));
            recordOperationDuration(managedResource, operation);
        }
    }

    private boolean isOperationFailed(ManagedResourceV2 managedResource) {
        ManagedResourceStatus status = StatusUtilities.getManagedResourceStatus(managedResource);
        return ManagedResourceStatusV2.FAILED == status;
    }

    private List<Tag> buildTags(ManagedResourceV2 managedResource, MetricsOperation operation) {
        Tag instanceTag = Tag.of(RESOURCE_TAG, managedResource.getClass().getSimpleName().toLowerCase());
        Tag versionTag = Tag.of(VERSION_TAG, V2.class.getSimpleName());
        return List.of(instanceTag, versionTag, operation.getMetricTag());
    }

    private void recordOperationDuration(ManagedResourceV2 managedResource, MetricsOperation operation) {
        Duration operationDuration = calculateOperationDuration(managedResource.getOperation());
        meterRegistry.timer(operationDurationMetricName, buildTags(managedResource, operation)).record(operationDuration);
    }

    private Duration calculateOperationDuration(Operation operation) {
        return Duration.between(operation.getRequestedAt(), ZonedDateTime.now(ZoneOffset.UTC));
    }

}
