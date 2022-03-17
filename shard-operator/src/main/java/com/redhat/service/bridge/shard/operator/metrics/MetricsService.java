package com.redhat.service.bridge.shard.operator.metrics;

public interface MetricsService {
    void updateManagerRequestMetrics(ManagerRequestType requestType, ManagerRequestStatus status);
}
