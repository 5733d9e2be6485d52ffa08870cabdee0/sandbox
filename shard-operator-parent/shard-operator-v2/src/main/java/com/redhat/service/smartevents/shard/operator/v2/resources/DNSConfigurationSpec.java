package com.redhat.service.smartevents.shard.operator.v2.resources;

public class DNSConfigurationSpec {

    String host;

    public DNSConfigurationSpec() {

    }

    private DNSConfigurationSpec(Builder builder) {
        setHost(builder.host);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public static final class Builder {

        private String host;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder host(String val) {
            host = val;
            return this;
        }

        public DNSConfigurationSpec build() {
            return new DNSConfigurationSpec(this);
        }
    }
}
