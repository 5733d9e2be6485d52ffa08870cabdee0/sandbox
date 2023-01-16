package com.redhat.service.smartevents.shard.operator.core.metrics;

import java.time.ZonedDateTime;

import com.redhat.service.smartevents.infra.core.metrics.SupportsMetrics;

public interface SupportsTimers extends SupportsMetrics {

    ZonedDateTime getCreationTimestamp();

}
