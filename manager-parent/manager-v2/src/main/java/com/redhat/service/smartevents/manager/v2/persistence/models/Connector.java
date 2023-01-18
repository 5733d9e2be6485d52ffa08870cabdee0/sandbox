package com.redhat.service.smartevents.manager.v2.persistence.models;

import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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

import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v2.api.models.connectors.ConnectorDefinition;

@NamedQueries({
        @NamedQuery(name = "CONNECTOR_V2.findByIdWithConditions",
                query = "from Connector_V2 conn left join fetch conn.conditions where conn.id=:id and conn.type=:type"),
        @NamedQuery(name = "CONNECTOR_V2.findByBridgeIdAndName",
                query = "from Connector_V2 conn where conn.name=:name and conn.bridge.id=:bridgeId and conn.type=:type"),
        @NamedQuery(name = "CONNECTOR_V2.findByBridgeIdAndCustomerId",
                query = "select distinct (conn) from Connector_V2 conn left join fetch conn.bridge left join fetch conn.conditions where conn.bridge.id=:bridgeId and conn.bridge.customerId=:customerId order by conn.submittedAt desc"),
        @NamedQuery(name = "CONNECTOR_V2.findByIdsWithBridgeAndConditions",
                query = "select distinct (conn) from Connector_V2 conn left join fetch conn.bridge left join fetch conn.conditions where conn.id in (:ids)"),
        @NamedQuery(name = "CONNECTOR_V2.countByBridgeIdAndCustomerId",
                query = "select count(conn.id) from Connector_V2 conn where conn.bridge.id=:bridgeId and conn.bridge.customerId=:customerId and conn.type=:type")
})
// Hibernate does not support sub-queries in Named Queries. This is therefore written as Native Query.
// Hibernate however does not support eager fetches with Native Queries without composing a _View_ class model,
// retrieving a flat ResultSet and then building the object model hierarchy in Java. We therefore split
// retrieval into two database calls: (1) Get the Connector IDs, (2) Fetch the Connector objects.
@NamedNativeQueries({
        @NamedNativeQuery(name = "CONNECTOR_V2.findConnectorIdByShardIdToDeployOrDelete",
                query = "select conn.id " +
                        "from Connector_V2 conn " +
                        "left join Bridge_V2 b on conn.bridge_id = b.id " +
                        "left join (" +
                        "  select connector_id, count(*) as incomplete_count from Condition c where component='MANAGER' and status != 'TRUE' group by c.connector_id " +
                        "    ) cp on cp.connector_id = conn.id " +
                        "where " +
                        // The LEFT JOIN on the sub-query can return a null if there are no MANAGER records that are NOT complete.
                        "(cp.incomplete_count = 0 or cp.incomplete_count is null) and " +
                        "b.shard_id = :shardId and " +
                        "conn.type = :type")
})
@FilterDefs({
        @FilterDef(name = "byName", parameters = { @ParamDef(name = "name", type = "string") })
})
@Filters({
        @Filter(name = "byName", condition = "name like :name")
})
@Entity(name = "Connector_V2")
@Table(name = "CONNECTOR_V2", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "bridge_id", "type" }) })
public class Connector extends ManagedDefinedResourceV2<ConnectorDefinition> {

    public static final String BRIDGE_ID_PARAM = "bridgeId";

    public static final String TYPE_PARAM = "type";

    // ID returned by MC service
    @Column(name = "connector_external_id")
    protected String connectorExternalId;

    @Column(name = "connector_type_id", nullable = false)
    protected String connectorTypeId;

    @Column(name = "topic_name")
    protected String topicName;

    @Column(name = "error")
    protected String error;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bridge_id")
    protected Bridge bridge;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "connector_id")
    protected List<Condition> conditions;

    // The discriminator to retrieve sources/sinks
    @Column(name = "type", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    protected ConnectorType type;

    public Connector() {
    }

    public Connector(String name) {
        this.name = name;
    }

    public String getConnectorExternalId() {
        return connectorExternalId;
    }

    public void setConnectorExternalId(String connectorExternalId) {
        this.connectorExternalId = connectorExternalId;
    }

    public String getConnectorTypeId() {
        return connectorTypeId;
    }

    public void setConnectorTypeId(String connectorTypeId) {
        this.connectorTypeId = connectorTypeId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Bridge getBridge() {
        return bridge;
    }

    public void setBridge(Bridge bridge) {
        this.bridge = bridge;
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

    public ConnectorType getType() {
        return type;
    }

    public void setType(ConnectorType type) {
        this.type = type;
    }
}
