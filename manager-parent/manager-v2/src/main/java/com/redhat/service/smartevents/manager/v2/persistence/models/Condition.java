package com.redhat.service.smartevents.manager.v2.persistence.models;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;

@Entity
@Table(name = "CONDITION")
public class Condition {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "message")
    private String message;

    @Column(name = "errorCode")
    private String errorCode;

    @Column(name = "component", nullable = false)
    @Enumerated(EnumType.STRING)
    private ComponentType component;

    // The bridge is not null if the Condition refers to a Bridge
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bridge_id")
    private Bridge bridge;

    // The processor is not null if the Condition refers to a Processor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private Processor processor;

    @Column(name = "last_transition_time", columnDefinition = "TIMESTAMP", nullable = false)
    private ZonedDateTime lastTransitionTime;

    public String getId() {
        return id;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public ComponentType getComponent() {
        return component;
    }

    public void setComponent(ComponentType component) {
        this.component = component;
    }

    public ZonedDateTime getLastTransitionTime() {
        return lastTransitionTime;
    }

    public void setLastTransitionTime(ZonedDateTime lastTransitionTime) {
        this.lastTransitionTime = lastTransitionTime;
    }

    public Bridge getBridge() {
        return bridge;
    }

    public void setBridge(Bridge bridge) {
        this.bridge = bridge;
    }

    public Processor getProcessor() {
        return processor;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
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
        Condition condition = (Condition) o;
        return id.equals(condition.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
