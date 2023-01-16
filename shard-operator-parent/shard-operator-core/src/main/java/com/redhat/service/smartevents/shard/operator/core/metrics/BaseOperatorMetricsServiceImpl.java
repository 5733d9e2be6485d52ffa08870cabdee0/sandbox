package com.redhat.service.smartevents.shard.operator.core.metrics;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.metrics.SupportsMetrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

public abstract class BaseOperatorMetricsServiceImpl implements OperatorMetricsService {

    public static final String SHARD_ID = "shardId";
    public static final String REQUEST_STATUS = "status";
    public static final String HTTP_STATUS_CODE = "statusCode";

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

    @ConfigProperty(name = "event-bridge.shard-id")
    String shardId;

    @ConfigProperty(name = "rhose.metrics-name.manager-requests-total-count")
    String managerRequestsTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operator.operation-total-count")
    String operatorTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operator.operation-success-total-count")
    String operatorTotalSuccessCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operator.operation-failure-total-count")
    String operatorTotalFailureCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operator.operation-duration-seconds")
    String operationDurationMetricName;

    protected abstract Tag getVersionTag();

    @Override
    public Set<String> getMetricNames() {
        return Set.of(managerRequestsTotalCountMetricName, operatorTotalCountMetricName, operatorTotalSuccessCountMetricName, operatorTotalFailureCountMetricName);
    }

    @Override
    public void onOperationStart(SupportsMetrics resource, MetricsOperation operation) {
        List<Tag> tags = buildTags(resource, operation);
        meterRegistry.counter(operatorTotalCountMetricName, tags).increment();
    }

    @Override
    public void onOperationComplete(SupportsMetrics resource, MetricsOperation operation) {
        List<Tag> tags = buildTags(resource, operation);
        meterRegistry.counter(operatorTotalSuccessCountMetricName, tags).increment();
        recordOperationDuration(resource, operation);
    }

    @Override
    public void onOperationFailed(SupportsMetrics resource, MetricsOperation operation) {
        List<Tag> tags = buildTags(resource, operation);
        meterRegistry.counter(operatorTotalFailureCountMetricName, tags).increment();
        recordOperationDuration(resource, operation);
    }

    @Override
    public void updateManagerRequestMetrics(MetricsOperation operation, ManagerRequestStatus status, String statusCode) {
        List<Tag> tags = buildManagerRequestTags(operation, status, statusCode);
        meterRegistry.counter(managerRequestsTotalCountMetricName, tags).increment();
    }

    // Tags to record accumulative metrics
    // See https://github.com/prometheus/client_java/issues/696
    // A Metric needs the same set of Tag keys to be correctly recorded by the Prometheus client
    protected List<Tag> buildTags(SupportsMetrics resource, MetricsOperation operation) {
        Tag shardIdTag = Tag.of(SHARD_ID, shardId);
        Tag resourceTag = Tag.of(RESOURCE_TAG, resource.getClass().getSimpleName().toLowerCase());
        Tag resourceIdTag = Tag.of(RESOURCE_ID_TAG, WILDCARD);
        return List.of(shardIdTag, resourceTag, resourceIdTag, getVersionTag(), operation.getMetricTag());
    }

    // Tags to record instance metrics
    // See https://github.com/prometheus/client_java/issues/696
    // A Metric needs the same set of Tag keys to be correctly recorded by the Prometheus client
    protected List<Tag> buildInstanceTags(SupportsMetrics resource, MetricsOperation operation) {
        Tag shardIdTag = Tag.of(SHARD_ID, shardId);
        Tag resourceTag = Tag.of(RESOURCE_TAG, resource.getClass().getSimpleName().toLowerCase());
        Tag resourceIdTag = Tag.of(RESOURCE_ID_TAG, resource.getId());
        return List.of(shardIdTag, resourceTag, resourceIdTag, getVersionTag(), operation.getMetricTag());
    }

    protected List<Tag> buildManagerRequestTags(MetricsOperation operation, ManagerRequestStatus status, String statusCode) {
        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of(SHARD_ID, shardId));
        tags.add(operation.getMetricTag());
        tags.add(Tag.of(REQUEST_STATUS, status.name()));
        if (StringUtils.isNotEmpty(statusCode)) {
            tags.add(Tag.of(HTTP_STATUS_CODE, statusCode));
        }
        return tags;
    }

    private void recordOperationDuration(SupportsMetrics resource, MetricsOperation operation) {
        if (!(resource instanceof SupportsTimers)) {
            return;
        }
        SupportsTimers supportsTimers = (SupportsTimers) resource;
        Duration duration = Duration.between(supportsTimers.getCreationTimestamp(), ZonedDateTime.now(ZoneOffset.UTC));
        meterRegistry.timer(operationDurationMetricName, buildTags(resource, operation)).record(duration);
        meterRegistry.timer(operationDurationMetricName, buildInstanceTags(resource, operation)).record(duration);
    }

}
