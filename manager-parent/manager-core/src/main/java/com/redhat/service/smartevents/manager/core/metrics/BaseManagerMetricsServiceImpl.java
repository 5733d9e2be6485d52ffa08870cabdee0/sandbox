package com.redhat.service.smartevents.manager.core.metrics;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.manager.core.models.ManagedResource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

public abstract class BaseManagerMetricsServiceImpl<M extends ManagedResource> implements ManagerMetricsService<M> {

    // Constant for the metric tag for the resource instance we're operating on e.g. Processor/Bridge.
    public static final String RESOURCE_TAG = "resource";
    // Constant for the id of the resource instance we're operating on.
    public static final String RESOURCE_ID_TAG = "id";
    // Constant for the version tag of the resource instance we're operating on e.g. v1/v2.
    public static final String VERSION_TAG = "version";
    // Metrics must have the same set of Tag keys. Therefore, when recording accumulative metrics use a WILDCARD as the Resource Id
    public static final String WILDCARD = "*";

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

    protected abstract Tag getVersionTag();

    protected abstract boolean isOperationSuccessful(M managedResource, MetricsOperation operation);

    protected abstract boolean isOperationFailed(M managedResource);

    protected abstract Duration calculateOperationDuration(M resource, MetricsOperation operation);

    @Override
    public Set<String> getMetricNames() {
        return Set.of(operationDurationMetricName, operationTotalCountMetricName, operationTotalSuccessCountMetricName, operationTotalFailureCountMetricName);
    }

    @Override
    public void onOperationStart(M resource, MetricsOperation operation) {
        incrementCounter(operationTotalCountMetricName, buildTags(resource, operation));
    }

    @Override
    public void onOperationComplete(M resource, MetricsOperation operation) {
        if (isOperationSuccessful(resource, operation)) {
            incrementCounter(operationTotalSuccessCountMetricName, buildTags(resource, operation));
            recordOperationDuration(resource, operation);
        }
    }

    @Override
    public void onOperationFailed(M resource, MetricsOperation operation) {
        if (isOperationFailed(resource)) {
            incrementCounter(operationTotalFailureCountMetricName, buildTags(resource, operation));
            recordOperationDuration(resource, operation);
        }
    }

    private void incrementCounter(String counterName, List<Tag> tags) {
        meterRegistry.counter(counterName, tags).increment();
    }

    // Tags to record accumulative metrics
    // See https://github.com/prometheus/client_java/issues/696
    // A Metric needs the same set of Tag keys to be correctly recorded by the Prometheus client
    private List<Tag> buildTags(M resource, MetricsOperation operation) {
        Tag resourceTag = Tag.of(RESOURCE_TAG, resource.getClass().getSimpleName().toLowerCase());
        Tag resourceIdTag = Tag.of(RESOURCE_ID_TAG, WILDCARD);
        return List.of(resourceTag, resourceIdTag, getVersionTag(), operation.getMetricTag());
    }

    // Tags to record instance metrics
    // See https://github.com/prometheus/client_java/issues/696
    // A Metric needs the same set of Tag keys to be correctly recorded by the Prometheus client
    private List<Tag> buildInstanceTags(M resource, MetricsOperation operation) {
        Tag resourceTag = Tag.of(RESOURCE_TAG, resource.getClass().getSimpleName().toLowerCase());
        Tag resourceIdTag = Tag.of(RESOURCE_ID_TAG, resource.getId());
        return List.of(resourceTag, resourceIdTag, getVersionTag(), operation.getMetricTag());
    }

    private void recordOperationDuration(M resource, MetricsOperation operation) {
        Duration operationDuration = calculateOperationDuration(resource, operation);
        meterRegistry.timer(operationDurationMetricName, buildTags(resource, operation)).record(operationDuration);
        meterRegistry.timer(operationDurationMetricName, buildInstanceTags(resource, operation)).record(operationDuration);
    }
}
