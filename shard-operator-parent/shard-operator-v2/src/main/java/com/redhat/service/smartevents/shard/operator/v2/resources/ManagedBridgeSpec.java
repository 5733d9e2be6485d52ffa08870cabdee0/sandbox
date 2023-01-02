package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.Objects;

public class ManagedBridgeSpec {

    private String id;

    private String name;

    private String customerId;

    private KNativeBrokerConfigurationSpec kNativeBrokerConfiguration = new KNativeBrokerConfigurationSpec();

    private DNSConfigurationSpec dnsConfiguration = new DNSConfigurationSpec();

    private String owner;

    private long generation;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long generation) {
        this.generation = generation;
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
                && Objects.equals(dnsConfiguration, that.dnsConfiguration) && Objects.equals(owner, that.owner) && Objects.equals(generation, that.generation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId, kNativeBrokerConfiguration, dnsConfiguration, owner, generation);
    }
}
