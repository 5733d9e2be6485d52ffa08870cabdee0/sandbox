package com.redhat.service.smartevents.shard.operator.v1.metrics;

import java.time.ZonedDateTime;
import java.util.Objects;

import com.redhat.service.smartevents.shard.operator.core.metrics.SupportsTimers;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;

public class MetricsUtilities {

    public static SupportsTimers from(BridgeIngress resource) {
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

    public static SupportsTimers from(BridgeExecutor resource) {
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
