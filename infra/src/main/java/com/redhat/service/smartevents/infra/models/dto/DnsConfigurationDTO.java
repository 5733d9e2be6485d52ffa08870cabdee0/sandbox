package com.redhat.service.smartevents.infra.models.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DnsConfigurationDTO {

    @JsonProperty("tlsCertificate")
    private String tlsCertificate;

    @JsonProperty("tlsKey")
    private String tlsKey;

    public DnsConfigurationDTO(String tlsCertificate, String tlsKey) {
        this.tlsCertificate = tlsCertificate;
        this.tlsKey = tlsKey;
    }

    public String getTlsCertificate() {
        return tlsCertificate;
    }

    public void setTlsCertificate(String tlsCertificate) {
        this.tlsCertificate = tlsCertificate;
    }

    public String getTlsKey() {
        return tlsKey;
    }

    public void setTlsKey(String tlsKey) {
        this.tlsKey = tlsKey;
    }
}
