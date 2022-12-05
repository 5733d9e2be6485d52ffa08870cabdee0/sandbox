package com.redhat.service.smartevents.shard.operator.v2.converters;

import java.net.MalformedURLException;
import java.net.URL;

import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InvalidURLException;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.DNSConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.KNativeBrokerConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.KafkaConfigurationSpec;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.TLSSpec;

public class ManagedBridgeConverter {

    public static ManagedBridge fromBridgeDTOToManageBridge(BridgeDTO bridgeDTO, String namespace) {
        try {

            DNSConfigurationSpec dns = DNSConfigurationSpec.Builder.builder()
                    .host(new URL(bridgeDTO.getEndpoint()).getHost())
                    .tls(new TLSSpec(bridgeDTO.getTlsKey(), bridgeDTO.getTlsCertificate()))
                    .build();

            KafkaConnectionDTO kafkaConnectionDTO = bridgeDTO.getKafkaConnection();
            KafkaConfigurationSpec kafkaConfigurationSpec = KafkaConfigurationSpec.Builder.builder()
                    .bootstrapServers(kafkaConnectionDTO.getBootstrapServers())
                    .password(kafkaConnectionDTO.getClientSecret())
                    .user(kafkaConnectionDTO.getClientId())
                    .saslMechanism(kafkaConnectionDTO.getSaslMechanism())
                    .securityProtocol(kafkaConnectionDTO.getSecurityProtocol())
                    .topic(kafkaConnectionDTO.getTopic())
                    .build();

            return new ManagedBridge.Builder()
                    .withNamespace(namespace)
                    .withBridgeName(bridgeDTO.getName())
                    .withCustomerId(bridgeDTO.getCustomerId())
                    .withOwner(bridgeDTO.getOwner())
                    .withBridgeId(bridgeDTO.getId())
                    .withDnsConfigurationSpec(dns)
                    .withKnativeBrokerConfigurationSpec(new KNativeBrokerConfigurationSpec(kafkaConfigurationSpec))
                    .build();
        } catch (MalformedURLException e) {
            throw new InvalidURLException("Could not extract host from " + bridgeDTO.getEndpoint());
        }
    }
}
