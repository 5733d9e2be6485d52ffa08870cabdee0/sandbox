package com.redhat.service.smartevents.manager.persistence.v1.models;

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
import javax.persistence.Table;

import org.hibernate.annotations.TypeDef;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.manager.models.ManagedDefinedResource;

import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;

@NamedQueries({
        @NamedQuery(name = "CONNECTORENTITY.findByProcessorIdAndName",
                query = "from ConnectorEntity c where c.name=:name and c.processor.id=:processorId"),
        @NamedQuery(name = "CONNECTORENTITY.findByProcessorId",
                query = "from ConnectorEntity c where c.processor.id=:processorId")
})
@Entity
@Table(name = "CONNECTOR")
@TypeDef(name = JsonTypes.JSON_BIN, typeClass = JsonBinaryType.class)
public class ConnectorEntity extends ManagedDefinedResource<JsonNode> { // called -Entity to avoid clash with Connector REST API

    public static final String PROCESSOR_ID_PARAM = "processorId";

    @Column(name = "type", updatable = false, nullable = false)
    @Enumerated(EnumType.STRING)
    private ConnectorType type;

    // ID returned by MC service
    @Column(name = "connector_external_id")
    private String connectorExternalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private Processor processor;

    @Column(name = "connector_type_id")
    private String connectorTypeId;

    @Column(name = "topic_name")
    private String topicName;

    @Column(name = "error")
    private String error;

    public ConnectorType getType() {
        return type;
    }

    public void setType(ConnectorType type) {
        this.type = type;
    }

    public String getConnectorExternalId() {
        return connectorExternalId;
    }

    public void setConnectorExternalId(String connectorExternalId) {
        this.connectorExternalId = connectorExternalId;
    }

    public Processor getProcessor() {
        return processor;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    public String getConnectorTypeId() {
        return connectorTypeId;
    }

    public void setConnectorTypeId(String connectorType) {
        this.connectorTypeId = connectorType;
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
        ConnectorEntity processor = (ConnectorEntity) o;
        return id.equals(processor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ConnectorEntity{" +
                "connectorExternalId='" + connectorExternalId + '\'' +
                ", type='" + type + '\'' +
                ", connectorTypeId='" + connectorTypeId + '\'' +
                ", topicName='" + topicName + '\'' +
                ", error='" + error + '\'' +
                ", definition=" + definition +
                ", id='" + id + '\'' +
                ", status=" + status +
                ", dependencyStatus=" + dependencyStatus +
                ", name='" + name + '\'' +
                ", submittedAt=" + submittedAt +
                ", publishedAt=" + publishedAt +
                '}';
    }
}
