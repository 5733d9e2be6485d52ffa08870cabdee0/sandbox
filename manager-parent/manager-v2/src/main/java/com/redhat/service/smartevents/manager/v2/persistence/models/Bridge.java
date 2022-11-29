package com.redhat.service.smartevents.manager.v2.persistence.models;

import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@NamedQueries({
        @NamedQuery(name = "BRIDGE_V2.countByOrganisationId",
                query = "select count(*) from Bridge_V2 where organisation_id=:organisationId")
})
@Entity(name = "Bridge_V2")
@Table(name = "BRIDGE_V2", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "customer_id" }) })
public class Bridge extends ManagedResourceV2 {

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private String customerId;

    @Column(name = "shard_id", nullable = false)
    private String shardId;

    @Column(name = "organisation_id", nullable = false, updatable = false)
    private String organisationId;

    @Column(name = "cloud_provider", nullable = false, updatable = false)
    private String cloudProvider;

    @Column(name = "region", nullable = false, updatable = false)
    private String region;

    @Column(name = "subscription_id", nullable = false, updatable = false)
    private String subscriptionId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bridge_id")
    private List<Condition> conditions;

    public Bridge() {
    }

    public Bridge(String name) {
        this.name = name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getShardId() {
        return shardId;
    }

    public String getOrganisationId() {
        return organisationId;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setShardId(String shardId) {
        this.shardId = shardId;
    }

    public void setOrganisationId(String organisationId) {
        this.organisationId = organisationId;
    }

    public String getCloudProvider() {
        return cloudProvider;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    /*
     * See: https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
     * In the context of JPA equality, our id is our unique business key as we generate it via UUID.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Bridge bridge = (Bridge) o;
        return id.equals(bridge.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
