package com.redhat.service.bridge.manager.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.hibernate.annotations.TypeDef;

import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;

@NamedQueries({
        @NamedQuery(name = "PROCESSOR.findByBridgeIdAndName",
                query = "from Processor p where p.name=:name and p.bridge.id=:bridgeId"),
        @NamedQuery(name = "PROCESSOR.findByShardIdWithReadyDependencies",
                query = "select p " +
                        "from Processor p " +
                        "join fetch p.bridge " +
                        "left join p.connectorEntities as c " +
                        "where p.status='ACCEPTED' " +
                        "and p.bridge.status='READY' " +
                        "and p.shardId=:shardId " +
                        "and p.dependencyStatus='READY'"),
        @NamedQuery(name = "PROCESSOR.findByShardIdWithDeletedDependencies",
                query = "select p " +
                        "from Processor p " +
                        "join fetch p.bridge " +
                        "left join p.connectorEntities as c " +
                        "where p.status='DEPROVISION' " +
                        "and p.bridge.status='READY' " +
                        "and p.shardId=:shardId " +
                        "and p.dependencyStatus='DELETED'"),
        @NamedQuery(name = "PROCESSOR.findByIdBridgeIdAndCustomerId",
                query = "from Processor p join fetch p.bridge where p.id=:id and (p.bridge.id=:bridgeId and p.bridge.customerId=:customerId)"),
        @NamedQuery(name = "PROCESSOR.findByBridgeIdAndCustomerId",
                query = "from Processor p join fetch p.bridge where p.bridge.id=:bridgeId and p.bridge.customerId=:customerId"),
        @NamedQuery(name = "PROCESSOR.countByBridgeIdAndCustomerId",
                query = "select count(p.id) from Processor p where p.bridge.id=:bridgeId and p.bridge.customerId=:customerId"),
        @NamedQuery(name = "PROCESSOR.idsByBridgeIdAndCustomerId",
                query = "select p.id from Processor p where p.bridge.id=:bridgeId and p.bridge.customerId=:customerId order by p.submittedAt asc"),
        @NamedQuery(name = "PROCESSOR.findByIds",
                query = "select p from Processor p join fetch p.bridge where p.id in (:ids)")
})
@Entity
@TypeDef(name = JsonTypes.JSON_BIN, typeClass = JsonBinaryType.class)
public class Processor extends ManagedDefinedResource {

    public static final String BRIDGE_ID_PARAM = "bridgeId";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bridge_id")
    private Bridge bridge;

    @OneToMany(mappedBy = "processor")
    private List<ConnectorEntity> connectorEntities = new ArrayList<>();

    @Column(name = "shard_id")
    private String shardId;

    public Bridge getBridge() {
        return bridge;
    }

    public void setBridge(Bridge bridge) {
        this.bridge = bridge;
    }

    public List<ConnectorEntity> getConnectorEntities() {
        return connectorEntities;
    }

    public void setConnectorEntities(List<ConnectorEntity> connectorEntities) {
        this.connectorEntities = connectorEntities;
    }

    public String getShardId() {
        return shardId;
    }

    public void setShardId(String shardId) {
        this.shardId = shardId;
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
        Processor processor = (Processor) o;
        return id.equals(processor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Processor{" +
                "definition=" + definition +
                ", id='" + id + '\'' +
                ", status=" + status +
                ", dependencyStatus=" + dependencyStatus +
                ", name='" + name + '\'' +
                ", submittedAt=" + submittedAt +
                ", publishedAt=" + publishedAt +
                ", bridge=" + bridge +
                ", connectorEntities=" + connectorEntities +
                ", shardId='" + shardId + '\'' +
                '}';
    }
}
