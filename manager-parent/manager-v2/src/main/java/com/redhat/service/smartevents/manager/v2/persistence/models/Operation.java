package com.redhat.service.smartevents.manager.v2.persistence.models;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "OPERATION")
public class Operation {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "requested_at", columnDefinition = "TIMESTAMP", nullable = false)
    private ZonedDateTime requestedAt;

    @Column(name = "managed_resource_id", nullable = false)
    private String managedResourceId;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ZonedDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(ZonedDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getManagedResourceId() {
        return managedResourceId;
    }

    public void setManagedResourceId(String managedResourceId) {
        this.managedResourceId = managedResourceId;
    }
}
