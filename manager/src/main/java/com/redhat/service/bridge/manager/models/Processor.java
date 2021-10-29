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
import javax.persistence.Version;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.processors.BaseProcessor;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;

import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;

// The join fetch on the filters will produce duplicates that must be removed from the results (see https://developer.jboss.org/docs/DOC-15782#jive_content_id_Hibernate_does_not_return_distinct_results_for_a_query_with_outer_join_fetching_enabled_for_a_collection_even_if_I_use_the_distinct_keyword)
@NamedQueries({
        @NamedQuery(name = "PROCESSOR.findByBridgeIdAndName",
                query = "from Processor p where p.name=:name and p.bridge.id=:bridgeId"),
        @NamedQuery(name = "PROCESSOR.findByStatus",
                query = "from Processor p join fetch p.bridge where p.status in (:statuses) and p.bridge.status='AVAILABLE'"),
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
public class Processor {

    public static final String ID_PARAM = "id";

    public static final String NAME_PARAM = "name";

    public static final String BRIDGE_ID_PARAM = "bridgeId";

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, name = "name")
    private String name;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "definition", columnDefinition = JsonTypes.JSON_BIN)
    private BaseProcessor definition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bridge_id")
    private Bridge bridge;

    @Version
    private long version;

    @Column(name = "submitted_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    private ZonedDateTime submittedAt;

    @Column(name = "published_at", columnDefinition = "TIMESTAMP")
    private ZonedDateTime publishedAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private BridgeStatus status;

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

    public BaseProcessor getDefinition() {
        return definition;
    }

    public void setDefinition(BaseProcessor definition) {
        this.definition = definition;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Bridge getBridge() {
        return bridge;
    }

    public void setBridge(Bridge bridge) {
        this.bridge = bridge;
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

    public BridgeStatus getStatus() {
        return status;
    }

    public void setStatus(BridgeStatus status) {
        this.status = status;
    }

    public ProcessorResponse toResponse() {

        ProcessorResponse processorResponse = new ProcessorResponse();
        processorResponse.setId(id);
        processorResponse.setName(name);
        processorResponse.setStatus(status);
        processorResponse.setPublishedAt(publishedAt);
        processorResponse.setSubmittedAt(submittedAt);
        processorResponse.setFilters(definition.getFilters());
        processorResponse.setTransformationTemplate(definition.getTransformationTemplate());
        processorResponse.setAction(definition.getAction());

        if (this.bridge != null) {
            processorResponse.setHref(APIConstants.USER_API_BASE_PATH + bridge.getId() + "/processors/" + id);
            processorResponse.setBridge(this.bridge.toResponse());
        }

        return processorResponse;
    }

    public ProcessorDTO toDTO() {

        ProcessorDTO dto = new ProcessorDTO();
        dto.setId(this.id);
        dto.setName(this.name);
        dto.setStatus(this.status);
        dto.setFilters(definition.getFilters());
        dto.setTransformationTemplate(definition.getTransformationTemplate());
        dto.setAction(definition.getAction());

        if (this.bridge != null) {
            dto.setBridge(this.bridge.toDTO());
        }

        return dto;
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
}
