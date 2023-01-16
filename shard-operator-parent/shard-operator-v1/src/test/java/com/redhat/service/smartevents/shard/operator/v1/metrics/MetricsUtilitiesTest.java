package com.redhat.service.smartevents.shard.operator.v1.metrics;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;
import com.redhat.service.smartevents.shard.operator.core.metrics.SupportsTimers;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsUtilitiesTest {

    @Test
    void testFromManagedBridge() {
        BridgeIngress bridge = BridgeIngress.fromBuilder()
                .withBridgeId("bridgeId")
                .withBridgeName("name")
                .withNamespace("namespace")
                .withCustomerId("customerId")
                .withOwner("owner")
                .withHost("host")
                .build();

        SupportsTimers supportsTimers = MetricsUtilities.from(bridge);
        assertThat(supportsTimers.getId()).isEqualTo("bridgeId");
        assertThat(supportsTimers.getCreationTimestamp()).isNull();

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        bridge.getMetadata().setCreationTimestamp(now.toString());

        SupportsTimers supportsTimers2 = MetricsUtilities.from(bridge);
        assertThat(supportsTimers2.getId()).isEqualTo("bridgeId");
        assertThat(supportsTimers2.getCreationTimestamp()).isEqualTo(now);
    }

    @Test
    void testFromManagedProcessor() {
        BridgeExecutor processor = BridgeExecutor.fromBuilder()
                .withBridgeId("bridgeId")
                .withProcessorId("processorId")
                .withProcessorName("name")
                .withNamespace("namespace")
                .withCustomerId("customerId")
                .withDefinition(new ProcessorDefinition())
                .withProcessorType(ProcessorType.SOURCE)
                .withImageName("image")
                .build();

        SupportsTimers supportsTimers = MetricsUtilities.from(processor);
        assertThat(supportsTimers.getId()).isEqualTo("processorId");
        assertThat(supportsTimers.getCreationTimestamp()).isNull();

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        processor.getMetadata().setCreationTimestamp(now.toString());

        SupportsTimers supportsTimers2 = MetricsUtilities.from(processor);
        assertThat(supportsTimers2.getId()).isEqualTo("processorId");
        assertThat(supportsTimers2.getCreationTimestamp()).isEqualTo(now);
    }

}
