package com.redhat.service.smartevents.manager.core.metrics;

import com.redhat.service.smartevents.infra.core.metrics.MetricsService;
import com.redhat.service.smartevents.manager.core.models.ManagedResource;

public interface ManagerMetricsService<M extends ManagedResource> extends MetricsService<M> {
}
