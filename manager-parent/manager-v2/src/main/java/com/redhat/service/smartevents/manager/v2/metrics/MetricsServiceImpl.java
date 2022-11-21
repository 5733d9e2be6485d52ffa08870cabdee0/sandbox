package com.redhat.service.smartevents.manager.v2.metrics;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.manager.v2.persistence.models.ManagedResourceV2;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getManagedResourceStatus;

@ApplicationScoped
public class MetricsServiceImpl implements ManagerMetricsServiceV2 {

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
        ManagedResourceStatus managedResourceStatus = getManagedResourceStatus(managedResource);
        if (MetricsOperation.MANAGER_RESOURCE_PROVISION == operation || MetricsOperation.MANAGER_RESOURCE_MODIFY == operation) {
            return ManagedResourceStatus.READY == managedResourceStatus;
        }

        return ManagedResourceStatus.DELETED == managedResourceStatus;
    }

    @Override
    public void onOperationFailed(ManagedResourceV2 managedResource, MetricsOperation operation) {
        if (isOperationFailed(managedResource)) {
            incrementCounter(operationTotalFailureCountMetricName, buildTags(managedResource, operation));
            recordOperationDuration(managedResource, operation);
        }
    }

    private boolean isOperationFailed(ManagedResourceV2 managedResource) {
        ManagedResourceStatus managedResourceStatus = getManagedResourceStatus(managedResource);
        return ManagedResourceStatus.FAILED == managedResourceStatus;
    }

    private List<Tag> buildTags(ManagedResourceV2 managedResource, MetricsOperation operation) {
        Tag instanceTag = Tag.of(RESOURCE_TAG, managedResource.getClass().getSimpleName().toLowerCase());
        return List.of(instanceTag, operation.getMetricTag());
    }

    private Duration calculateOperationDuration(ManagedResourceV2 managedResource, MetricsOperation operation) {
        switch (operation) {
            case MANAGER_RESOURCE_PROVISION:
                return Duration.between(managedResource.getSubmittedAt(), ZonedDateTime.now(ZoneOffset.UTC));
            case MANAGER_RESOURCE_MODIFY:
                return Duration.between(assertUpdateOperation(managedResource, operation), ZonedDateTime.now(ZoneOffset.UTC));
            case MANAGER_RESOURCE_DELETE:
                return Duration.between(assertDeleteOperation(managedResource, operation), ZonedDateTime.now(ZoneOffset.UTC));
            default:
                throw new IllegalStateException(String.format("Unable to calculate operation duration for MetricsOperation '%s'", operation));
        }
    }

    private ZonedDateTime assertUpdateOperation(ManagedResourceV2 managedResource, MetricsOperation metricsOperation) {
        return assertOperation(managedResource, OperationType.UPDATE, metricsOperation);
    }

    private ZonedDateTime assertDeleteOperation(ManagedResourceV2 managedResource, MetricsOperation metricsOperation) {
        return assertOperation(managedResource, OperationType.DELETE, metricsOperation);
    }

    private ZonedDateTime assertOperation(ManagedResourceV2 managedResource, OperationType managedResourceOperationType, MetricsOperation metricsOperation) {
        Operation operation = managedResource.getOperation();
        if (Objects.isNull(operation)) {
            throw new IllegalStateException(String.format("Unable to record metrics for [%s]. Operation is null.", managedResource.getName()));
        } else if (operation.getType() != managedResourceOperationType) {
            throw new IllegalStateException(String.format("Unable to record metrics for [%s]. Operation [%s] is inconsistent with MetricOperation [%s].",
                    managedResource.getName(),
                    operation.getType().name(),
                    metricsOperation.getMetricTag().getValue()));
        }
        return operation.getRequestedAt();
    }

    private void recordOperationDuration(ManagedResourceV2 managedResource, MetricsOperation operation) {
        Duration operationDuration = calculateOperationDuration(managedResource, operation);
        meterRegistry.timer(operationDurationMetricName, buildTags(managedResource, operation)).record(operationDuration);
    }
}
