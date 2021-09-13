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
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.dto.BridgeStatus;
import com.redhat.service.bridge.infra.dto.ProcessorDTO;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;

@NamedQueries({
        @NamedQuery(name = "PROCESSOR.findByBridgeIdAndName",
                query = "from Processor p where p.name=:name and p.bridge.id=:bridgeId"),
        @NamedQuery(name = "PROCESSOR.findByStatus",
                query = "from Processor p join fetch p.bridge where p.status in (:statuses) and p.bridge.status='AVAILABLE'")
})
@Entity
public class Processor {

    public static final String NAME_PARAM = "name";

    public static final String BRIDGE_ID_PARAM = "bridgeId";

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
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

        if (this.bridge != null) {
            processorResponse.setHref(APIConstants.USER_API_BASE_PATH + bridge.getId() + "/processors/" + id);
            processorResponse.setBridge(this.bridge.toResponse());
        }

        return processorResponse;
    }

    public ProcessorDTO toDTO() {

        ProcessorDTO dto = new ProcessorDTO();
        dto.setStatus(this.status);
        dto.setName(this.name);
        dto.setId(this.id);

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
