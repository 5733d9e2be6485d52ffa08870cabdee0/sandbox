package com.redhat.service.smartevents.shard.operator.v2.converters;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.KafkaConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.utils.Fixtures;

import static org.assertj.core.api.Assertions.assertThat;

public class ManagedBridgeConverterTest {

    @Test
    public void fromBridgeDTO() {

        BridgeDTO bridgeDTO = Fixtures.createBridge(OperationType.CREATE);
        String namespace = UUID.randomUUID().toString();

        ManagedBridge managedBridge = ManagedBridgeConverter.fromBridgeDTOToManageBridge(bridgeDTO, namespace);

        assertThat(managedBridge.getMetadata().getNamespace()).isEqualTo(namespace);
        assertThat(managedBridge.getSpec().getId()).isEqualTo(bridgeDTO.getId());
        assertThat(managedBridge.getSpec().getCustomerId()).isEqualTo(bridgeDTO.getCustomerId());
        assertThat(managedBridge.getSpec().getName()).isEqualTo(bridgeDTO.getName());
        assertThat(managedBridge.getSpec().getGeneration()).isEqualTo(bridgeDTO.getGeneration());
        assertThat(managedBridge.getSpec().getOwner()).isEqualTo(bridgeDTO.getOwner());
        assertThat(managedBridge.getSpec().getDnsConfiguration().getHost()).isEqualTo(Fixtures.BRIDGE_HOST);

        KafkaConfigurationSpec kafkaConfiguration = managedBridge.getSpec().getkNativeBrokerConfiguration().getKafkaConfiguration();
        assertThat(kafkaConfiguration.getBootstrapServers()).isEqualTo(bridgeDTO.getKnativeBrokerConfiguration().getKafkaConnection().getBootstrapServers());
        assertThat(kafkaConfiguration.getTopic()).isEqualTo(bridgeDTO.getKnativeBrokerConfiguration().getKafkaConnection().getTopic());
    }
}
