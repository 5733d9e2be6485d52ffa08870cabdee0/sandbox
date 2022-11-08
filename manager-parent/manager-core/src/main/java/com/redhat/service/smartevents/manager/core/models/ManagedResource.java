package com.redhat.service.smartevents.manager.core.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import com.redhat.service.smartevents.infra.core.exceptions.HasErrorInformation;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;

@MappedSuperclass
public class ManagedResource implements HasErrorInformation {

    public static final String ID_PARAM = "id";

    public static final String NAME_PARAM = "name";

    @Id
    protected String id = UUID.randomUUID().toString();

    @Version
    @SuppressWarnings("unused")
    protected long version;

    @Column(name = "generation")
    protected long generation;

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

    @Column(name = "deletion_requested_at", columnDefinition = "TIMESTAMP")
    private ZonedDateTime deletionRequestedAt;

    @Column(name = "error_id")
    private Integer errorId;

    @Column(name = "error_uuid")
    private String errorUUID;

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

    public ZonedDateTime getDeletionRequestedAt() {
        return deletionRequestedAt;
    }

    public void setDeletionRequestedAt(ZonedDateTime deletedAt) {
        this.deletionRequestedAt = deletedAt;
    }

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long generation) {
        this.generation = generation;
    }

    @Override
    public Integer getErrorId() {
        return errorId;
    }

    public void setErrorId(Integer errorId) {
        this.errorId = errorId;
    }

    @Override
    public String getErrorUUID() {
        return errorUUID;
    }

    public void setErrorUUID(String errorUUID) {
        this.errorUUID = errorUUID;
    }

    public boolean isActionable() {
        // A ManagedResource can only be modified or deleted if it's in READY or FAILED state
        return getStatus() == ManagedResourceStatus.READY || getStatus() == ManagedResourceStatus.FAILED;
    }

}
