package com.redhat.service.smartevents.manager.v2.persistence.models;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.redhat.service.smartevents.infra.v2.api.models.OperationType;

@Entity
@Table(name = "OPERATION")
public class Operation {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OperationType type;

    @Column(name = "requested_at", columnDefinition = "TIMESTAMP", nullable = false)
    private ZonedDateTime requestedAt;

    @Column(name = "managed_resource_id", nullable = false)
    private String managedResourceId;

    public String getId() {
        return id;
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
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
        Operation operation = (Operation) o;
        return id.equals(operation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
