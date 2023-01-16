package com.redhat.service.smartevents.shard.operator.v2.metrics;

import java.time.ZonedDateTime;
import java.util.Objects;

import com.redhat.service.smartevents.shard.operator.core.metrics.SupportsTimers;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

public class MetricsUtilities {

    public static SupportsTimers from(ManagedBridge resource) {
        return new SupportsTimers() {
            @Override
            public String getId() {
                return resource.getSpec().getId();
            }

            @Override
            public ZonedDateTime getCreationTimestamp() {
                String k8sCreationTimestamp = resource.getMetadata().getCreationTimestamp();
                return Objects.nonNull(k8sCreationTimestamp) ? ZonedDateTime.parse(k8sCreationTimestamp) : null;
            }

        };
    }

    public static SupportsTimers from(ManagedProcessor resource) {
        return new SupportsTimers() {
            @Override
            public String getId() {
                return resource.getSpec().getId();
            }

            @Override
            public ZonedDateTime getCreationTimestamp() {
                String k8sCreationTimestamp = resource.getMetadata().getCreationTimestamp();
                return Objects.nonNull(k8sCreationTimestamp) ? ZonedDateTime.parse(k8sCreationTimestamp) : null;
            }

        };
    }

}
