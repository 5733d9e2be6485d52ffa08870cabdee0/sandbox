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

            TLSSpec tlsSpec = new TLSSpec();
            tlsSpec.setCertificate(bridgeDTO.getTlsCertificate());
            tlsSpec.setKey(bridgeDTO.getTlsKey());

            DNSConfigurationSpec dns = new DNSConfigurationSpec();
            dns.setHost(new URL(bridgeDTO.getEndpoint()).getHost());
            dns.setTls(tlsSpec);

            KafkaConnectionDTO kafkaConnectionDTO = bridgeDTO.getKafkaConnection();
            KafkaConfigurationSpec kafkaConfigurationSpec = new KafkaConfigurationSpec();
            kafkaConfigurationSpec.setBootstrapServers(kafkaConnectionDTO.getBootstrapServers());
            kafkaConfigurationSpec.setPassword(kafkaConnectionDTO.getClientSecret());
            kafkaConfigurationSpec.setUserId(kafkaConnectionDTO.getClientId());
            kafkaConfigurationSpec.setSaslMechanism(kafkaConnectionDTO.getSaslMechanism());
            kafkaConfigurationSpec.setSecurityProtocol(kafkaConnectionDTO.getSecurityProtocol());
            kafkaConfigurationSpec.setTopicName(kafkaConnectionDTO.getTopic());

            KNativeBrokerConfigurationSpec kNativeBrokerConfigurationSpec = new KNativeBrokerConfigurationSpec();
            kNativeBrokerConfigurationSpec.setKafkaConfiguration(kafkaConfigurationSpec);

            return new ManagedBridge.Builder()
                    .withNamespace(namespace)
                    .withBridgeName(bridgeDTO.getName())
                    .withCustomerId(bridgeDTO.getCustomerId())
                    .withOwner(bridgeDTO.getOwner())
                    .withBridgeId(bridgeDTO.getId())
                    .withDnsConfigurationSpec(dns)
                    .withKnativeBrokerConfigurationSpec(kNativeBrokerConfigurationSpec)
                    .build();
        } catch (MalformedURLException e) {
            throw new InvalidURLException("Could not extract host from " + bridgeDTO.getEndpoint());
        }
    }
}
