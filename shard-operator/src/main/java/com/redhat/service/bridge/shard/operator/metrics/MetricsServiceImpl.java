package com.redhat.service.bridge.shard.operator.metrics;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class MetricsServiceImpl implements MetricsService {

    private static final String MANAGER_REQUEST_METRICS = "http.manager.request";
    private static final String SHARD_ID = "shardId";
    private static final String REQUEST_TYPE = "type";
    private static final String REQUEST_STATUS = "status";
    private static final String HTTP_STATUS_CODE = "statusCode";

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "event-bridge.shard-id")
    String shardId;

    @Override
    public void updateManagerRequestMetrics(ManagerRequestType requestType, ManagerRequestStatus status, String statusCode) {
        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of(SHARD_ID, shardId));
        tags.add(Tag.of(REQUEST_TYPE, requestType.name()));
        tags.add(Tag.of(REQUEST_STATUS, status.name()));
        tags.add(Tag.of(HTTP_STATUS_CODE, statusCode));
        meterRegistry.counter(MANAGER_REQUEST_METRICS, tags).increment();
    }
}
