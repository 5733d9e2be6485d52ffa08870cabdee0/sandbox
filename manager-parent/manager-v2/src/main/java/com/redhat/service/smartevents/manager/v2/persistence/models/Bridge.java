package com.redhat.service.smartevents.manager.v2.persistence.models;

import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;

@NamedQueries({
        @NamedQuery(name = "BRIDGE_V2.findByNameAndCustomerId",
                query = "from Bridge_V2 where name=:name and customer_id=:customerId"),
        @NamedQuery(name = "BRIDGE_V2.findByIdWithConditions",
                query = "from Bridge_V2 b left join fetch b.conditions where b.id=:id"),
        @NamedQuery(name = "BRIDGE.findByIdAndCustomerIdWithConditions",
                query = "from Bridge_V2 b left join fetch b.conditions where b.id=:id and customer_id=:customerId"),
        @NamedQuery(name = "BRIDGE_V2.countByOrganisationId",
                query = "select count(*) from Bridge_V2 where organisation_id=:organisationId"),
        @NamedQuery(name = "BRIDGE_V2.findByCustomerId",
                query = "select distinct (b) from Bridge_V2 b left join fetch b.conditions where customer_id=:customerId order by submitted_at desc")
})
@NamedNativeQueries({
        @NamedNativeQuery(name = "BRIDGE_V2.findByShardIdToDeployOrDelete",
                resultClass = Bridge.class,
                query = "select b.* " +
                        "from Bridge_V2 b " +
                        "left join (" +
                        "  select bridge_id, count(*) as conditions_count from Condition c where component='MANAGER' group by c.bridge_id" +
                        ") cp_counts on cp_counts.bridge_id=b.id " +
                        "left join (" +
                        "  select bridge_id, count(*)  as conditions_count from Condition c where component='MANAGER' and status='TRUE' group by c.bridge_id" +
                        "    ) cp_ready_counts on cp_ready_counts.bridge_id=b.id " +
                        "where " +
                        "b.shard_id = :shardId and " +
                        "cp_counts.conditions_count = cp_ready_counts.conditions_count")
})
@FilterDefs({
        @FilterDef(name = "byName", parameters = { @ParamDef(name = "name", type = "string") }),
})
@Filters({
        @Filter(name = "byName", condition = "name like :name"),
})
@Entity(name = "Bridge_V2")
@Table(name = "BRIDGE_V2", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "customer_id" }) })
public class Bridge extends ManagedResourceV2 {

    public static final String CUSTOMER_ID_PARAM = "customerId";

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

    @Override
    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        if (Objects.isNull(this.conditions)) {
            this.conditions = conditions;
        } else {
            // Hibernate manages the underlying collection to handle one-to-many orphan removal.
            // If we replace the underlying collection Hibernate complains that its managed collection
            // becomes disconnected. Therefore, clear it and add all.
            this.conditions.clear();
            this.conditions.addAll(conditions);
        }
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
