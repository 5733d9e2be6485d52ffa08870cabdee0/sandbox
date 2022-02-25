package com.redhat.service.bridge.manager.models;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

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
public class ConnectorEntity extends ManagedEntity { // called -Entity to avoid clash with Connector REST API

    public static final String ID_PARAM = "id";

    public static final String NAME_PARAM = "name";

    public static final String PROCESSOR_ID_PARAM = "processorId";

    // ID returned by MC service
    @Column(name = "connector_external_id")
    private String connectorExternalId;

    @Column(nullable = false, name = "name")
    private String name;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "definition", columnDefinition = JsonTypes.JSON_BIN)
    private JsonNode definition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private Processor processor;

    @Column(name = "connector_type")
    private String connectorType;

    @Column(name = "topic_name")
    private String topicName;

    @Column(name = "error")
    private String error;

    public String getConnectorExternalId() {
        return connectorExternalId;
    }

    public void setConnectorExternalId(String connectorExternalId) {
        this.connectorExternalId = connectorExternalId;
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

    public Processor getProcessor() {
        return processor;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
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
}
