package com.redhat.service.smartevents.shard.operator.metrics;

import com.redhat.service.smartevents.infra.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.metrics.MetricsService;

public interface OperatorMetricsService extends MetricsService<Object> {

    void updateManagerRequestMetrics(MetricsOperation operation, ManagerRequestStatus status, String statusCode);

}
