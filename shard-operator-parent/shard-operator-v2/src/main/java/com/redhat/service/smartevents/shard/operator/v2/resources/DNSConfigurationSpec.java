package com.redhat.service.smartevents.shard.operator.v2.resources;

public class DNSConfigurationSpec {

    String host;

    TLSSpec tls = new TLSSpec();

    public DNSConfigurationSpec() {

    }

    private DNSConfigurationSpec(Builder builder) {
        setHost(builder.host);
        setTls(builder.tls);
    }

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

    public static final class Builder {

        private String host;
        private TLSSpec tls;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder host(String val) {
            host = val;
            return this;
        }

        public Builder tls(TLSSpec val) {
            tls = val;
            return this;
        }

        public DNSConfigurationSpec build() {
            return new DNSConfigurationSpec(this);
        }
    }
}
