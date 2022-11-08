package com.redhat.service.smartevents.shard.operator.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class MetricsServiceImpl implements OperatorMetricsService {

    private static final String SHARD_ID = "shardId";
    private static final String REQUEST_STATUS = "status";
    private static final String HTTP_STATUS_CODE = "statusCode";

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

    @Override
    public Set<String> getMetricNames() {
        return Set.of(managerRequestsTotalCountMetricName, operatorTotalCountMetricName, operatorTotalSuccessCountMetricName, operatorTotalFailureCountMetricName);
    }

    @Override
    public void updateManagerRequestMetrics(MetricsOperation operation, ManagerRequestStatus status, String statusCode) {
        List<Tag> tags = buildCoreTags(operation, status, statusCode);
        meterRegistry.counter(managerRequestsTotalCountMetricName, tags).increment();
    }

    private List<Tag> buildCoreTags(MetricsOperation operation, ManagerRequestStatus status, String statusCode) {
        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of(SHARD_ID, shardId));
        tags.add(operation.getMetricTag());
        tags.add(Tag.of(REQUEST_STATUS, status.name()));
        if (StringUtils.isNotEmpty(statusCode)) {
            tags.add(Tag.of(HTTP_STATUS_CODE, statusCode));
        }
        return tags;
    }

    @Override
    public void onOperationStart(Object resource, MetricsOperation operation) {
        List<Tag> tags = buildCoreTags(operation, ManagerRequestStatus.SUCCESS, "");
        meterRegistry.counter(operatorTotalCountMetricName, tags).increment();
    }

    @Override
    public void onOperationComplete(Object resource, MetricsOperation operation) {
        List<Tag> tags = buildCoreTags(operation, ManagerRequestStatus.SUCCESS, "");
        meterRegistry.counter(operatorTotalSuccessCountMetricName, tags).increment();
    }

    @Override
    public void onOperationFailed(Object resource, MetricsOperation operation) {
        List<Tag> tags = buildCoreTags(operation, ManagerRequestStatus.FAILURE, "");
        meterRegistry.counter(operatorTotalFailureCountMetricName, tags).increment();
    }

}
