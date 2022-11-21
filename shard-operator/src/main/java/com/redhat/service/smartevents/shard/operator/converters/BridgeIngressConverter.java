package com.redhat.service.smartevents.shard.operator.converters;

import com.google.common.base.Strings;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.DnsConfigurationDTO;
import com.redhat.service.smartevents.infra.models.dto.KafkaConfigurationDTO;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngressSpec;
import com.redhat.service.smartevents.shard.operator.resources.DnsConfiguration;
import com.redhat.service.smartevents.shard.operator.resources.KafkaConfiguration;
import com.redhat.service.smartevents.shard.operator.utils.NamespaceUtil;

import javax.enterprise.context.ApplicationScoped;
import java.net.MalformedURLException;
import java.net.URL;

import static java.util.Objects.requireNonNull;

@ApplicationScoped
public class BridgeIngressConverter {

    public BridgeIngress fromBridgeDTOToBridgeIngress(BridgeDTO bridgeDTO) {
        validate(bridgeDTO);
        BridgeIngress bridgeIngress = new BridgeIngress();
        String namespace = NamespaceUtil.resolveCustomerNamespace(bridgeDTO.getCustomerId());
        bridgeIngress.getMetadata().setNamespace(namespace);
        bridgeIngress.getMetadata().setName(bridgeDTO.getName());

        BridgeIngressSpec bridgeIngressSpec = bridgeIngress.getSpec();
        bridgeIngressSpec.setId(bridgeDTO.getId());
        bridgeIngressSpec.setBridgeName(bridgeDTO.getName());
        bridgeIngressSpec.setCustomerId(bridgeDTO.getCustomerId());
        bridgeIngressSpec.setHost(getHost(bridgeDTO.getEndpoint()));
        bridgeIngressSpec.setOwner(bridgeDTO.getOwner());
        bridgeIngressSpec.setKafkaConfiguration(fromKafkaConfigurationDTOtoKafkaConfiguration(bridgeDTO.getKafkaConfiguration()));
        bridgeIngressSpec.setDnsConfiguration(fromDnsConfigurationDTOToDnsConfiguration(bridgeDTO.getDnsConfiguration()));
        return bridgeIngress;
    }

    private KafkaConfiguration fromKafkaConfigurationDTOtoKafkaConfiguration(KafkaConfigurationDTO kafkaConfigurationDTO) {
        KafkaConfiguration kafkaConfiguration = new KafkaConfiguration();
        kafkaConfiguration.setBootstrapServers(kafkaConfigurationDTO.getBootstrapServers());
        kafkaConfiguration.setClientId(kafkaConfigurationDTO.getClientId());
        kafkaConfiguration.setClientSecret(kafkaConfigurationDTO.getClientSecret());
        kafkaConfiguration.setSaslMechanism(kafkaConfigurationDTO.getSaslMechanism());
        kafkaConfiguration.setSecurityProtocol(kafkaConfigurationDTO.getSecurityProtocol());
        kafkaConfiguration.setTopic(kafkaConfigurationDTO.getTopic());
        return kafkaConfiguration;
    }

    private DnsConfiguration fromDnsConfigurationDTOToDnsConfiguration(DnsConfigurationDTO dnsConfigurationDTO) {
        DnsConfiguration dnsConfiguration = new DnsConfiguration();
        dnsConfiguration.setTlsCertificate(dnsConfigurationDTO.getTlsCertificate());
        dnsConfiguration.setTlsKey(dnsConfigurationDTO.getTlsKey());
        return dnsConfiguration;
    }

    private void validate(BridgeDTO bridgeDTO) {
        requireNonNull(Strings.emptyToNull(bridgeDTO.getCustomerId()), "[BridgeIngress] CustomerId can't be null");
        requireNonNull(Strings.emptyToNull(bridgeDTO.getId()), "[BridgeIngress] Id can't be null");
        requireNonNull(Strings.emptyToNull(bridgeDTO.getName()), "[BridgeIngress] Name can't be null");
        requireNonNull(Strings.emptyToNull(getHost(bridgeDTO.getEndpoint())), "[BridgeIngress] Host can't be null");
    }

    private String getHost(String endpoint) {
        try {
            return new URL(endpoint).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
