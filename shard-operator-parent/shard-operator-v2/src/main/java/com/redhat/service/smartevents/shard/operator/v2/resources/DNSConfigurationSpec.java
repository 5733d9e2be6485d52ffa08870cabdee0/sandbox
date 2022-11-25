package com.redhat.service.smartevents.shard.operator.v2.resources;

public class DNSConfigurationSpec {

    String host;

    TLSSpec tls = new TLSSpec();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public TLSSpec getTls() {
        return tls;
    }

    public void setTls(TLSSpec tls) {
        this.tls = tls;
    }
}
