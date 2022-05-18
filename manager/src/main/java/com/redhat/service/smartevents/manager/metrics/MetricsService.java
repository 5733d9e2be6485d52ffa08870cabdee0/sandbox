package com.redhat.service.smartevents.manager.metrics;

import java.util.Set;

import com.redhat.service.smartevents.manager.models.ManagedResource;

public interface MetricsService {

    Set<String> getMetricNames();

    <T extends ManagedResource> void onOperationStart(T managedResource, MetricsOperation operation);

    <T extends ManagedResource> void onOperationComplete(T managedResource, MetricsOperation operation);
}
