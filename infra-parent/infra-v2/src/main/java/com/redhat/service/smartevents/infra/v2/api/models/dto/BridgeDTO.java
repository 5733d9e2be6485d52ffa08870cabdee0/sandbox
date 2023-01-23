package com.redhat.service.smartevents.infra.v2.api.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeDTO extends BaseResourceDTO {

    @JsonProperty("dnsConfiguration")
    private DNSConfigurationDTO dnsConfiguration;
    @JsonProperty("knativeBrokerConfiguration")
    private KnativeBrokerConfigurationDTO knativeBrokerConfiguration;

    @JsonProperty("sourceConfiguration")
    private SourceConfigurationDTO sourceConfiguration;

    public BridgeDTO() {
    }

    public BridgeDTO(String id,
            String name,
            String customerId,
            String owner,
            DNSConfigurationDTO dnsConfiguration,
            KnativeBrokerConfigurationDTO knativeBrokerConfiguration,
            SourceConfigurationDTO sourceConfiguration,
            OperationType operationType,
            int timeoutSeconds) {
        super(id, name, customerId, owner, operationType, timeoutSeconds);
        this.dnsConfiguration = dnsConfiguration;
        this.knativeBrokerConfiguration = knativeBrokerConfiguration;
        this.sourceConfiguration = sourceConfiguration;
    }

    public DNSConfigurationDTO getDnsConfiguration() {
        return dnsConfiguration;
    }

    public void setDnsConfiguration(DNSConfigurationDTO dnsConfiguration) {
        this.dnsConfiguration = dnsConfiguration;
    }

    public KnativeBrokerConfigurationDTO getKnativeBrokerConfiguration() {
        return knativeBrokerConfiguration;
    }

    public void setKnativeBrokerConfiguration(KnativeBrokerConfigurationDTO knativeBrokerConfiguration) {
        this.knativeBrokerConfiguration = knativeBrokerConfiguration;
    }

    public SourceConfigurationDTO getSourceConfiguration() {
        return sourceConfiguration;
    }

    public void setSourceConfiguration(SourceConfigurationDTO sourceConfiguration) {
        this.sourceConfiguration = sourceConfiguration;
    }
}
