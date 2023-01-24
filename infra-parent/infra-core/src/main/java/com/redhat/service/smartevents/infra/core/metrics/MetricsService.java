package com.redhat.service.smartevents.infra.core.metrics;

import java.util.Set;

public interface MetricsService<T> {

    Set<String> getMetricNames();

    void onOperationStart(T resource, MetricsOperation operation);

    void onOperationComplete(T resource, MetricsOperation operation);

    void onOperationFailed(T resource, MetricsOperation operation);

}
