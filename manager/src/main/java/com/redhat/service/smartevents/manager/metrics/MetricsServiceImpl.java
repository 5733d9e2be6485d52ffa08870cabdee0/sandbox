package com.redhat.service.smartevents.manager.metrics;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.models.ManagedResource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MetricsServiceImpl implements MetricsService {

    /*
     * Constant for the metric tag for the resource instance we're operating on e.g. Processor/Bridge.
     */
    static final String INSTANCE_TAG = "instance";

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "rhose.metrics-name.operation-total-count")
    String operationTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-success-total-count")
    String operationTotalSuccessCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-duration-seconds")
    String operatonDurationMetricName;

    @Override
    public Set<String> getMetricNames() {
        return Set.of(operatonDurationMetricName, operationTotalCountMetricName, operationTotalSuccessCountMetricName);
    }

    @Override
    public <T extends ManagedResource> void onOperationStart(T managedResource, MetricsOperation operation) {
        incrementCounter(operationTotalCountMetricName, buildTags(managedResource, operation));
    }

    private void incrementCounter(String counterName, List<Tag> tags) {
        meterRegistry.counter(counterName, tags).increment();
    }

    private boolean wasOperationSuccessful(ManagedResource managedResource, MetricsOperation operation) {
        if (MetricsOperation.PROVISION == operation || MetricsOperation.MODIFY == operation) {
            return ManagedResourceStatus.READY == managedResource.getStatus();
        }

        return ManagedResourceStatus.DELETED == managedResource.getStatus();
    }

    @Override
    public <T extends ManagedResource> void onOperationComplete(T managedResource, MetricsOperation operation) {
        if (wasOperationSuccessful(managedResource, operation)) {
            incrementCounter(operationTotalSuccessCountMetricName, buildTags(managedResource, operation));
            recordOperationDuration(managedResource, operation);
        }
    }

    private List<Tag> buildTags(ManagedResource managedResource, MetricsOperation operation) {
        Tag instanceTag = Tag.of(INSTANCE_TAG, managedResource.getClass().getSimpleName().toLowerCase());
        return List.of(instanceTag, operation.getMetricTag());
    }

    private Duration calculateOperationDuration(ManagedResource managedResource, MetricsOperation operation) {
        switch (operation) {
            case PROVISION:
                return Duration.between(managedResource.getSubmittedAt(), ZonedDateTime.now());
            case MODIFY:
                return Duration.between(managedResource.getModifiedAt(), ZonedDateTime.now());
            case DELETE:
                return Duration.between(managedResource.getDeletedAt(), ZonedDateTime.now());
            default:
                throw new IllegalStateException(String.format("Unable to calculate operation duration for MetricsOperation '%s'", operation));
        }
    }

    private void recordOperationDuration(ManagedResource managedResource, MetricsOperation operation) {
        Duration operationDuration = calculateOperationDuration(managedResource, operation);
        meterRegistry.timer(operatonDurationMetricName, buildTags(managedResource, operation)).record(operationDuration);
    }
}
