package com.redhat.service.smartevents.shard.operator.metrics;

public interface MetricsService {
    void updateManagerRequestMetrics(ManagerRequestType requestType, ManagerRequestStatus status, String statusCode);
}
