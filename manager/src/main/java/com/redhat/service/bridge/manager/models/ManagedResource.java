package com.redhat.service.bridge.manager.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;

@MappedSuperclass
public class ManagedResource {

    public static final String ID_PARAM = "id";

    public static final String NAME_PARAM = "name";

    @Id
    protected String id = UUID.randomUUID().toString();

    @Version
    @SuppressWarnings("unused")
    protected long version;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    protected ManagedResourceStatus status;

    @Column(name = "dependency_status")
    @Enumerated(EnumType.STRING)
    protected ManagedResourceStatus dependencyStatus;

    @Column(nullable = false, name = "name")
    protected String name;

    @Column(name = "submitted_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    protected ZonedDateTime submittedAt;

    @Column(name = "published_at", columnDefinition = "TIMESTAMP")
    protected ZonedDateTime publishedAt;

    @Column(name = "modified_at", columnDefinition = "TIMESTAMP")
    private ZonedDateTime modifiedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ManagedResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ManagedResourceStatus status) {
        this.status = status;
    }

    public ManagedResourceStatus getDependencyStatus() {
        return dependencyStatus;
    }

    public void setDependencyStatus(ManagedResourceStatus dependencyStatus) {
        this.dependencyStatus = dependencyStatus;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

}
