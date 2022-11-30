package com.redhat.service.smartevents.manager.v2.persistence.models;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.redhat.service.smartevents.infra.v2.api.models.OperationType;

@Embeddable
public class Operation {

    @Column(name = "operation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OperationType type;

    @Column(name = "operation_requested_at", columnDefinition = "TIMESTAMP", nullable = false)
    private ZonedDateTime requestedAt;

    public Operation() {
    }

    public Operation(OperationType type, ZonedDateTime requestedAt) {
        this.type = type;
        this.requestedAt = requestedAt;
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

}
