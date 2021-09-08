package com.redhat.developer.manager.models;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.api.models.responses.BridgeResponse;

@NamedQueries({
        @NamedQuery(name = "BRIDGE.findByStatus",
                query = "from Bridge where status=:status"),
        @NamedQuery(name = "BRIDGE.findByNameAndCustomerId",
                query = "from Bridge where name=:name and customer_id=:customerId"),
        @NamedQuery(name = "BRIDGE.findByIdAndCustomerId",
                query = "from Bridge where id=:id and customer_id=:customerId"),
        @NamedQuery(name = "BRIDGE.findByCustomerId",
                query = "from Bridge where customer_id=:customerId order by submitted_at desc"),
})
@Entity
@Table(name = "BRIDGE", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "customer_id"})})
public class Bridge {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false, updatable = false)
    private String name;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private String customerId;

    @Column(name = "submitted_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    private ZonedDateTime submittedAt;

    @Column(name = "published_at", columnDefinition = "TIMESTAMP")
    private ZonedDateTime publishedAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private BridgeStatus status;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "bridge", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<Processor> processors = new ArrayList<>();

    public Bridge() {
    }

    public void addProcessor(Processor processor) {
        this.processors.add(processor);
        processor.setBridge(this);
    }

    public void removeProcessor(Processor processor) {
        this.processors.remove(processor);
        processor.setBridge(null);
    }

    public Bridge(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getCustomerId() {
        return customerId;
    }

    public ZonedDateTime getPublishedAt() {
        return publishedAt;
    }

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public BridgeStatus getStatus() {
        return status;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setPublishedAt(ZonedDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public void setStatus(BridgeStatus status) {
        this.status = status;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<Processor> getProcessors() {
        return processors;
    }

    public void setProcessors(List<Processor> processors) {
        this.processors = processors;
    }

    public BridgeDTO toDTO() {
        BridgeDTO dto = new BridgeDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setEndpoint(endpoint);
        dto.setStatus(status);
        dto.setCustomerId(customerId);

        return dto;
    }

    public static Bridge fromDTO(BridgeDTO dto) {
        Bridge bridge = new Bridge();
        bridge.setId(dto.getId());
        bridge.setEndpoint(dto.getEndpoint());
        bridge.setCustomerId(dto.getCustomerId());
        bridge.setStatus(dto.getStatus());

        return bridge;
    }

    public BridgeResponse toResponse() {
        BridgeResponse response = new BridgeResponse();
        response.setId(id);
        response.setName(name);
        response.setEndpoint(endpoint);
        response.setSubmittedAt(submittedAt);
        response.setPublishedAt(publishedAt);
        response.setStatus(status);
        response.setHref("/api/v1/bridges/" + id);

        return response;
    }
}
