package com.redhat.service.smartevents.shard.operator.v2.converters;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.platform.InvalidURLException;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.DNSConfigurationDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.KnativeBrokerConfigurationDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.SourceConfigurationDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.DNSConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.KafkaConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.KnativeBrokerConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.SourceConfigurationSpec;

public class ManagedBridgeConverter {

    public static ManagedBridge fromBridgeDTOToManageBridge(BridgeDTO bridgeDTO, String namespace) {
        DNSConfigurationSpec dns = fromDNSConfigurationDTOToDNSConfigurationSpec(bridgeDTO.getDnsConfiguration());
        KnativeBrokerConfigurationSpec kNativeBrokerConfigurationSpec = fromKnativeBrokerConfigurationToKnativeBrokerConfigurationSpec(bridgeDTO.getKnativeBrokerConfiguration());
        SourceConfigurationSpec sourceConfigurationSpec = fromSourceConfigurationToSourceConfigurationSpec(bridgeDTO.getSourceConfiguration());

        return new ManagedBridge.Builder()
                .withNamespace(namespace)
                .withBridgeName(bridgeDTO.getName())
                .withCustomerId(bridgeDTO.getCustomerId())
                .withOwner(bridgeDTO.getOwner())
                .withBridgeId(bridgeDTO.getId())
                .withDnsConfigurationSpec(dns)
                .withKnativeBrokerConfigurationSpec(kNativeBrokerConfigurationSpec)
                .withGeneration(bridgeDTO.getGeneration())
                .withSourceConfiguration(sourceConfigurationSpec)
                .build();
    }

    private static DNSConfigurationSpec fromDNSConfigurationDTOToDNSConfigurationSpec(DNSConfigurationDTO dnsConfigurationDTO) throws InvalidURLException {
        try {
            return DNSConfigurationSpec.Builder.builder()
                    .host(new URL(dnsConfigurationDTO.getEndpoint()).getHost())
                    .build();
        } catch (MalformedURLException e) {
            throw new InvalidURLException("Could not extract host from " + dnsConfigurationDTO.getEndpoint());
        }
    }

    private static SourceConfigurationSpec fromSourceConfigurationToSourceConfigurationSpec(SourceConfigurationDTO sourceConfiguration) {
        if (Objects.isNull(sourceConfiguration)) {
            return null;
        }
        KafkaConnectionDTO sourceKafkaConnectionDTO = sourceConfiguration.getKafkaConnection();
        KafkaConfigurationSpec sourceKafkaConfigurationSpec = fromKafkaConnectionDTOToKafkaConfigurationSpec(sourceKafkaConnectionDTO);
        return new SourceConfigurationSpec(sourceKafkaConfigurationSpec);
    }

    private static KnativeBrokerConfigurationSpec fromKnativeBrokerConfigurationToKnativeBrokerConfigurationSpec(KnativeBrokerConfigurationDTO knativeBrokerConfiguration) {
        KafkaConnectionDTO knativeBrokerKafkaConnectionDTO = knativeBrokerConfiguration.getKafkaConnection();
        KafkaConfigurationSpec knativeBrokerKafkaConfigurationSpec = fromKafkaConnectionDTOToKafkaConfigurationSpec(knativeBrokerKafkaConnectionDTO);
        return new KnativeBrokerConfigurationSpec(knativeBrokerKafkaConfigurationSpec);
    }

    private static KafkaConfigurationSpec fromKafkaConnectionDTOToKafkaConfigurationSpec(KafkaConnectionDTO kafkaConnectionDTO) {
        return KafkaConfigurationSpec.Builder.builder()
                .bootstrapServers(kafkaConnectionDTO.getBootstrapServers())
                .topic(kafkaConnectionDTO.getTopic())
                .build();
    }
}
