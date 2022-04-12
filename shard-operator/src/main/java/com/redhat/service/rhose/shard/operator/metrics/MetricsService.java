package com.redhat.service.rhose.shard.operator.metrics;

public interface MetricsService {
    void updateManagerRequestMetrics(ManagerRequestType requestType, ManagerRequestStatus status, String statusCode);
}
