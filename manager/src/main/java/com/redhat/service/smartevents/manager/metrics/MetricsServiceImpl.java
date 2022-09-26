package com.redhat.service.smartevents.manager.metrics;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.infra.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.models.ManagedResource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class MetricsServiceImpl implements ManagerMetricsService {

    /*
     * Constant for the metric tag for the resource instance we're operating on e.g. Processor/Bridge.
     */
    static final String RESOURCE_TAG = "resource";

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
    public void onOperationStart(ManagedResource managedResource, MetricsOperation operation) {
        incrementCounter(operationTotalCountMetricName, buildTags(managedResource, operation));
    }

    private void incrementCounter(String counterName, List<Tag> tags) {
        meterRegistry.counter(counterName, tags).increment();
    }

    @Override
    public void onOperationComplete(ManagedResource managedResource, MetricsOperation operation) {
        if (isOperationSuccessful(managedResource, operation)) {
            incrementCounter(operationTotalSuccessCountMetricName, buildTags(managedResource, operation));
            recordOperationDuration(managedResource, operation);
        }
    }

    private boolean isOperationSuccessful(ManagedResource managedResource, MetricsOperation operation) {
        if (MetricsOperation.MANAGER_RESOURCE_PROVISION == operation || MetricsOperation.MANAGER_RESOURCE_MODIFY == operation) {
            return ManagedResourceStatus.READY == managedResource.getStatus();
        }

        return ManagedResourceStatus.DELETED == managedResource.getStatus();
    }

    @Override
    public void onOperationFailed(ManagedResource managedResource, MetricsOperation operation) {
        if (isOperationFailed(managedResource)) {
            incrementCounter(operationTotalFailureCountMetricName, buildTags(managedResource, operation));
            recordOperationDuration(managedResource, operation);
        }
    }

    private boolean isOperationFailed(ManagedResource managedResource) {
        return ManagedResourceStatus.FAILED == managedResource.getStatus();
    }

    private List<Tag> buildTags(ManagedResource managedResource, MetricsOperation operation) {
        Tag instanceTag = Tag.of(RESOURCE_TAG, managedResource.getClass().getSimpleName().toLowerCase());
        return List.of(instanceTag, operation.getMetricTag());
    }

    private Duration calculateOperationDuration(ManagedResource managedResource, MetricsOperation operation) {
        switch (operation) {
            case MANAGER_RESOURCE_PROVISION:
                return Duration.between(managedResource.getSubmittedAt(), ZonedDateTime.now(ZoneOffset.UTC));
            case MANAGER_RESOURCE_MODIFY:
                return Duration.between(managedResource.getModifiedAt(), ZonedDateTime.now(ZoneOffset.UTC));
            case MANAGER_RESOURCE_DELETE:
                return Duration.between(managedResource.getDeletionRequestedAt(), ZonedDateTime.now(ZoneOffset.UTC));
            default:
                throw new IllegalStateException(String.format("Unable to calculate operation duration for MetricsOperation '%s'", operation));
        }
    }

    private void recordOperationDuration(ManagedResource managedResource, MetricsOperation operation) {
        Duration operationDuration = calculateOperationDuration(managedResource, operation);
        meterRegistry.timer(operationDurationMetricName, buildTags(managedResource, operation)).record(operationDuration);
    }
}
