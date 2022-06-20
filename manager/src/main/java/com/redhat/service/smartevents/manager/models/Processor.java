package com.redhat.service.smartevents.manager.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.TypeDef;

import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;

@NamedQueries({
        @NamedQuery(name = "PROCESSOR.findByBridgeIdAndName",
                query = "from Processor p where p.name=:name and p.bridge.id=:bridgeId"),
        @NamedQuery(name = "PROCESSOR.findByShardIdToDeployOrDelete",
                query = "select p " +
                        "from Processor p " +
                        "join fetch p.bridge " +
                        "left join p.connectorEntities as c " +
                        "where " +
                        "p.shardId=:shardId and " +
                        "(" +
                        "  (" +
                        // Status combinations to support SINK/SOURCE Processors
                        "    p.bridge.status='READY' and " +
                        "    (" +
                        "      (p.status='PREPARING' and p.dependencyStatus='READY') " +
                        "      or " +
                        "      (p.status='PROVISIONING' and p.dependencyStatus='READY') " +
                        "      or " +
                        "      (p.status='DEPROVISION' and p.dependencyStatus='DELETED') " +
                        "      or " +
                        "      (p.status='DELETING' and p.dependencyStatus='DELETED') " +
                        "    )" +
                        "  )" +
                        // Status combinations to support updating a Bridge's ERROR_HANDLER Processor
                        // In these scenarios the Bridge will not be READY as its life-cycle is dependent
                        // on that of the ERROR_HANDLER Processor.
                        ") or (" +
                        "  p.bridge.status='PREPARING' and p.type='ERROR_HANDLER' and p.status='PREPARING' and p.dependencyStatus='READY'" +
                        ") or (" +
                        "  p.bridge.status='PREPARING' and p.type='ERROR_HANDLER' and p.status='DEPROVISION' and p.dependencyStatus='DELETED'" +
                        ") or (" +
                        "  p.bridge.status='DEPROVISION' and p.type='ERROR_HANDLER' and p.status='DEPROVISION' and p.dependencyStatus='DELETED'" +
                        ")"),
        @NamedQuery(name = "PROCESSOR.findByIdBridgeIdAndCustomerId",
                query = "from Processor p join fetch p.bridge where p.id=:id and (p.bridge.id=:bridgeId and p.bridge.customerId=:customerId)"),
        @NamedQuery(name = "PROCESSOR.findByBridgeIdAndCustomerId",
                query = "from Processor p join fetch p.bridge where p.bridge.id=:bridgeId and p.bridge.customerId=:customerId"),
        @NamedQuery(name = "PROCESSOR.countByBridgeIdAndCustomerId",
                query = "select count(p.id) from Processor p where p.bridge.id=:bridgeId and p.bridge.customerId=:customerId"),
        @NamedQuery(name = "PROCESSOR.findByBridgeIdAndCustomerIdNoFilter",
                query = "from Processor p where p.bridge.id=:bridgeId and p.bridge.customerId=:customerId order by p.submittedAt desc"),
        @NamedQuery(name = "PROCESSOR.findByIds",
                query = "select p from Processor p join fetch p.bridge where p.id in (:ids) order by p.submittedAt desc")
})
@Entity
@FilterDefs({
        @FilterDef(name = "byName", parameters = { @ParamDef(name = "name", type = "string") }),
        @FilterDef(name = "byStatus", parameters = { @ParamDef(name = "status", type = "com.redhat.service.smartevents.manager.dao.EnumTypeManagedResourceStatus") }),
        @FilterDef(name = "byType", parameters = { @ParamDef(name = "ptype", type = "com.redhat.service.smartevents.manager.dao.EnumTypeProcessorType") })
})
@Filters({
        @Filter(name = "byName", condition = "name like :name"),
        @Filter(name = "byStatus", condition = "status in (:status)"),
        @Filter(name = "byType", condition = "type in (:ptype)")
})
@TypeDef(name = JsonTypes.JSON_BIN, typeClass = JsonBinaryType.class)
public class Processor extends ManagedDefinedResource<ProcessorDefinition> {

    public static final String BRIDGE_ID_PARAM = "bridgeId";

    @Column(name = "type", updatable = false, nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessorType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bridge_id")
    private Bridge bridge;

    @OneToMany(mappedBy = "processor")
    private List<ConnectorEntity> connectorEntities = new ArrayList<>();

    @Column(name = "shard_id")
    private String shardId;

    @Column(name = "owner")
    private String owner;

    @Column(name = "has_secret")
    private boolean hasSecret;

    public ProcessorType getType() {
        return type;
    }

    public void setType(ProcessorType type) {
        this.type = type;
    }

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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean hasSecret() {
        return hasSecret;
    }

    public void setHasSecret(boolean hasSecret) {
        this.hasSecret = hasSecret;
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
                ", owner='" + owner + '\'' +
                '}';
    }
}
