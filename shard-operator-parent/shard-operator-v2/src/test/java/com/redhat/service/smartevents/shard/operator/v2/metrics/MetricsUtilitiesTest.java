package com.redhat.service.smartevents.shard.operator.v2.metrics;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.service.smartevents.shard.operator.core.metrics.SupportsTimers;
import com.redhat.service.smartevents.shard.operator.v2.resources.DNSConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.KnativeBrokerConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsUtilitiesTest {

    @Test
    void testFromManagedBridge() {
        ManagedBridge bridge = ManagedBridge.fromBuilder()
                .withBridgeId("bridgeId")
                .withBridgeName("name")
                .withNamespace("namespace")
                .withCustomerId("customerId")
                .withOwner("owner")
                .withDnsConfigurationSpec(new DNSConfigurationSpec())
                .withKnativeBrokerConfigurationSpec(new KnativeBrokerConfigurationSpec())
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
        ManagedProcessor processor = ManagedProcessor.fromBuilder()
                .withBridgeId("bridgeId")
                .withProcessorId("processorId")
                .withProcessorName("name")
                .withNamespace("namespace")
                .withCustomerId("customerId")
                .withDefinition(JsonNodeFactory.instance.objectNode())
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
