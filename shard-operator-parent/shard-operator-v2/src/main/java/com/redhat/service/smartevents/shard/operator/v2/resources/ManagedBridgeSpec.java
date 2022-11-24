package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.Objects;

public class ManagedBridgeSpec {

    private String id;

    private String customerId;

    private KNativeBrokerConfigurationSpec kNativeBrokerConfiguration;

    private DNSConfigurationSpec dnsConfiguration;

    private String owner;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public KNativeBrokerConfigurationSpec getkNativeBrokerConfiguration() {
        return kNativeBrokerConfiguration;
    }

    public void setkNativeBrokerConfiguration(KNativeBrokerConfigurationSpec kNativeBrokerConfiguration) {
        this.kNativeBrokerConfiguration = kNativeBrokerConfiguration;
    }

    public DNSConfigurationSpec getDnsConfiguration() {
        return dnsConfiguration;
    }

    public void setDnsConfiguration(DNSConfigurationSpec dnsConfiguration) {
        this.dnsConfiguration = dnsConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ManagedBridgeSpec that = (ManagedBridgeSpec) o;
        return Objects.equals(id, that.id) && Objects.equals(customerId, that.customerId) && Objects.equals(kNativeBrokerConfiguration, that.kNativeBrokerConfiguration)
                && Objects.equals(dnsConfiguration, that.dnsConfiguration) && Objects.equals(owner, that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId, kNativeBrokerConfiguration, dnsConfiguration, owner);
    }
}
