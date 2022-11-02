package com.redhat.service.smartevents.shard.operator.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DnsConfiguration {

    private String tlsCertificate;

    private String tlsKey;

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
