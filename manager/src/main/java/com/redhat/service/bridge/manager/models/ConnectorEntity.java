package com.redhat.service.bridge.manager.models;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.bridge.infra.models.dto.ConnectorStatus;

import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;

@NamedQueries({
        @NamedQuery(name = "CONNECTORENTITY.findByProcessorIdAndName",
                query = "from ConnectorEntity c where c.name=:name and c.processor.id=:processorId"),
        @NamedQuery(name = "CONNECTORENTITY.findByProcessorId",
                query = "from ConnectorEntity c where c.processor.id=:processorId"),
        @NamedQuery(name = "CONNECTORENTITY.findUnprocessed",
                query = "from ConnectorEntity c where c.status != c.desiredStatus and c.workerId is null"),
})
@Entity
@Table(name = "CONNECTOR")
@TypeDef(name = JsonTypes.JSON_BIN, typeClass = JsonBinaryType.class)
public class ConnectorEntity { // called -Entity to avoid clash with Connector REST API

    public static final String ID_PARAM = "id";

    public static final String NAME_PARAM = "name";

    public static final String PROCESSOR_ID_PARAM = "processorId";

    @Id
    private String id = UUID.randomUUID().toString();

    // ID returned by MC service
    @Column(name = "connector_external_id")
    private String connectorExternalId;

    @Column(name = "worker_id")
    private String workerId;

    @Column(nullable = false, name = "name")
    private String name;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "definition", columnDefinition = JsonTypes.JSON_BIN)
    private JsonNode definition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private Processor processor;

    @Version
    private long version;

    @Column(name = "submitted_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    private ZonedDateTime submittedAt;

    @Column(name = "published_at", columnDefinition = "TIMESTAMP")
    private ZonedDateTime publishedAt;

    @Column(name = "modified_at", columnDefinition = "TIMESTAMP")
    private ZonedDateTime modifiedAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ConnectorStatus status;

    @Column(name = "desired_state")
    @Enumerated(EnumType.STRING)
    private ConnectorStatus desiredStatus;

    @Column(name = "connector_type")
    private String connectorType;

    @Column(name = "topic_name")
    private String topicName;

    @Column(name = "error")
    private String error;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConnectorExternalId() {
        return connectorExternalId;
    }

    public void setConnectorExternalId(String connectorExternalId) {
        this.connectorExternalId = connectorExternalId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }

    public JsonNode getDefinition() {
        return definition;
    }

    public void setDefinition(JsonNode definition) {
        this.definition = definition;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Processor getProcessor() {
        return processor;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    public long getVersion() {
        return version;
    }

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public ZonedDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(ZonedDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public ZonedDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(ZonedDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public ConnectorStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectorStatus status) {
        this.status = status;
    }

    public ConnectorStatus getDesiredStatus() {
        return desiredStatus;
    }

    public void setDesiredStatus(ConnectorStatus desiredStatus) {
        this.desiredStatus = desiredStatus;
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
                "id='" + id + '\'' +
                ", status=" + status +
                ", desiredStatus=" + desiredStatus +
                ", connectorExternalId='" + connectorExternalId + '\'' +
                ", workerId='" + workerId + '\'' +
                ", name='" + name + '\'' +
                ", definition=" + definition +
                ", processor=" + processor +
                ", version=" + version +
                ", submittedAt=" + submittedAt +
                ", publishedAt=" + publishedAt +
                ", connectorType='" + connectorType + '\'' +
                ", topicName='" + topicName + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
