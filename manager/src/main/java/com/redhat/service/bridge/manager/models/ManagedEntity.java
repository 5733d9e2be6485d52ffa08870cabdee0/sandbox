package com.redhat.service.bridge.manager.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import io.quarkiverse.hibernate.types.json.JsonTypes;
import org.hibernate.annotations.Type;

@MappedSuperclass
public class ManagedEntity {

    @Id
    String id = UUID.randomUUID().toString();

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "dependencyStatus", columnDefinition = JsonTypes.JSON_BIN)
    DependencyStatus dependencyStatus = new DependencyStatus();

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    BridgeStatus status;

    @Version
    long version;

    @Column(name = "submitted_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    private ZonedDateTime submittedAt;

    @Column(name = "published_at", columnDefinition = "TIMESTAMP")
    private ZonedDateTime publishedAt;

    public DependencyStatus getDependencyStatus() {
        return dependencyStatus;
    }

    public BridgeStatus getStatus() {
        return status;
    }

    public void setStatus(BridgeStatus status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDependencyStatus(DependencyStatus dependencyStatus) {
        this.dependencyStatus = dependencyStatus;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
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
}
