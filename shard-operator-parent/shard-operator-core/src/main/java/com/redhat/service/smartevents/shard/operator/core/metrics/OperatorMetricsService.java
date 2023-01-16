package com.redhat.service.smartevents.shard.operator.core.metrics;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.metrics.MetricsService;
import com.redhat.service.smartevents.infra.core.metrics.SupportsMetrics;

public interface OperatorMetricsService extends MetricsService<SupportsMetrics> {

    void updateManagerRequestMetrics(MetricsOperation operation, ManagerRequestStatus status, String statusCode);

}
