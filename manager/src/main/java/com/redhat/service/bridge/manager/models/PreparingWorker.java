package com.redhat.service.bridge.manager.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@NamedQueries({
        @NamedQuery(name = "PREPARINGWORKER.findByEntityId",
                query = "from PreparingWorker where entity_id=:entityId"),
        @NamedQuery(name = "PREPARINGWORKER.findByWorkerIdAndStatusAndType",
                query = "from PreparingWorker where worker_id=:workerId and status=:status and type=:type"),
        @NamedQuery(name = "PREPARINGWORKER.findByAgeAndStatusAndType",
                query = "from PreparingWorker where modified_at <= :age and status=:status and type=:type"),
})
@Entity
@Table(name = "PREPARINGWORKER")
public class PreparingWorker {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "submitted_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    private ZonedDateTime submittedAt;

    @Column(name = "modified_at", columnDefinition = "TIMESTAMP")
    private ZonedDateTime modifiedAt;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "type")
    private String type;

    @Column(name = "status")
    private String status;

    @Column(name = "desired_status")
    private String desiredStatus;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public ZonedDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(ZonedDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDesiredStatus() {
        return desiredStatus;
    }

    public void setDesiredStatus(String desiredStatus) {
        this.desiredStatus = desiredStatus;
    }
}
