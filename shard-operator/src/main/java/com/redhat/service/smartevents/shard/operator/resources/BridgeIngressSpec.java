package com.redhat.service.smartevents.shard.operator.resources;

import java.util.Objects;

/**
 * Since this will be a representation of a Deployment resource, ideally we should implement the Podspecable interface.
 * Supposed to be a Duck Type of Pod. SREs would need all the fine-tuning attributes possible in the target pod.
 * The Controller then can reconcile only the main fields that the core engine would care.
 */
public class BridgeIngressSpec {

    private String image;

    private String customerId;

    private String owner;

    private String bridgeName;

    private String id;

    private String host;

    private KafkaConfiguration kafkaConfiguration;

    private DnsConfiguration dnsConfiguration;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOwner() {
        return owner;
    }

    public String getHost() {
        return host;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getBridgeName() {
        return bridgeName;
    }

    public void setBridgeName(String bridgeName) {
        this.bridgeName = bridgeName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public KafkaConfiguration getKafkaConfiguration() {
        return kafkaConfiguration;
    }

    public void setKafkaConfiguration(KafkaConfiguration kafkaConfiguration) {
        this.kafkaConfiguration = kafkaConfiguration;
    }

    public DnsConfiguration getDnsConfiguration() {
        return dnsConfiguration;
    }

    public void setDnsConfiguration(DnsConfiguration dnsConfiguration) {
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
        BridgeIngressSpec that = (BridgeIngressSpec) o;
        return Objects.equals(image, that.image)
                && Objects.equals(customerId, that.customerId)
                && Objects.equals(owner, that.owner)
                && Objects.equals(bridgeName, that.bridgeName)
                && Objects.equals(id, that.id)
                && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(image, customerId, bridgeName, id, host);
    }
}
