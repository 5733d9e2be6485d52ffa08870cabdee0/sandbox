package com.redhat.service.smartevents.manager.v2.persistence.models;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;

import com.redhat.service.smartevents.manager.core.models.ManagedResource;

@MappedSuperclass
public abstract class ManagedResourceV2 extends ManagedResource {
    @Column(name = "owner", nullable = false, updatable = false)
    protected String owner;

    @Embedded
    protected Operation operation;

    public abstract List<Condition> getConditions();

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }
}
