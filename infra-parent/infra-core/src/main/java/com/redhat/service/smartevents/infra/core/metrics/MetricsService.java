package com.redhat.service.smartevents.infra.core.metrics;

import java.util.Set;

public interface MetricsService<T> {

    Set<String> getMetricNames();

    void onOperationStart(T managedResource, MetricsOperation operation);

    void onOperationComplete(T managedResource, MetricsOperation operation);

    void onOperationFailed(T managedResource, MetricsOperation operation);

}
