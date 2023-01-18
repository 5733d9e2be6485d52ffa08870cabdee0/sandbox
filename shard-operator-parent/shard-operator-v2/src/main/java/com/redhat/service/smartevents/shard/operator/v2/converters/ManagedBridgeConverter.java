package com.redhat.service.smartevents.shard.operator.v2.converters;

import java.net.MalformedURLException;
import java.net.URL;

import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.platform.InvalidURLException;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.DNSConfigurationDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.DNSConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.KNativeBrokerConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.KafkaConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.SourceConfigurationSpec;

public class ManagedBridgeConverter {

    public static ManagedBridge fromBridgeDTOToManageBridge(BridgeDTO bridgeDTO, String namespace) {
        DNSConfigurationSpec dns = fromDNSConfigurationDTOToDNSConfigurationSpec(bridgeDTO.getDnsConfiguration());

        KafkaConnectionDTO knativeBrokerKafkaConnectionDTO = bridgeDTO.getKnativeBrokerConfiguration().getKafkaConnection();
        KafkaConfigurationSpec knativeBrokerKafkaConfigurationSpec = fromKafkaConnectionDTOToKafkaConfigurationSpec(knativeBrokerKafkaConnectionDTO);
        KNativeBrokerConfigurationSpec kNativeBrokerConfigurationSpec = new KNativeBrokerConfigurationSpec(knativeBrokerKafkaConfigurationSpec);

        KafkaConnectionDTO sourceKafkaConnectionDTO = bridgeDTO.getSourceConfiguration().getKafkaConnection();
        KafkaConfigurationSpec sourceKafkaConfigurationSpec = fromKafkaConnectionDTOToKafkaConfigurationSpec(sourceKafkaConnectionDTO);
        SourceConfigurationSpec sourceConfigurationSpec = new SourceConfigurationSpec(sourceKafkaConfigurationSpec);

        return new ManagedBridge.Builder()
                .withNamespace(namespace)
                .withBridgeName(bridgeDTO.getName())
                .withCustomerId(bridgeDTO.getCustomerId())
                .withOwner(bridgeDTO.getOwner())
                .withBridgeId(bridgeDTO.getId())
                .withDnsConfigurationSpec(dns)
                .withKnativeBrokerConfigurationSpec(kNativeBrokerConfigurationSpec)
                .withGeneration(bridgeDTO.getGeneration())
                .withManagedSource(sourceConfigurationSpec)
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

    private static KafkaConfigurationSpec fromKafkaConnectionDTOToKafkaConfigurationSpec(KafkaConnectionDTO kafkaConnectionDTO) {
        return KafkaConfigurationSpec.Builder.builder()
                .bootstrapServers(kafkaConnectionDTO.getBootstrapServers())
                .topic(kafkaConnectionDTO.getTopic())
                .build();
    }
}
