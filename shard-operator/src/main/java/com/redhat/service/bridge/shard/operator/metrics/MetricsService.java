package com.redhat.service.bridge.shard.operator.metrics;

public interface MetricsService {
    void UpdateManagerRequestMetrics(ManagerRequestType requestType, ManagerRequestStatus status);
}
