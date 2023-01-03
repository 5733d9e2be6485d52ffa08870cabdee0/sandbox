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

import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;

@Entity
@Table(name = "CONDITION")
public class Condition {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConditionStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "message")
    private String message;

    @Column(name = "errorCode")
    private String errorCode;

    @Column(name = "component", nullable = false)
    @Enumerated(EnumType.STRING)
    private ComponentType component;

    @Column(name = "last_transition_time", columnDefinition = "TIMESTAMP", nullable = false)
    private ZonedDateTime lastTransitionTime;

    public Condition() {
    }

    public Condition(String type, ConditionStatus status, String reason, String message, String errorCode, ComponentType component, ZonedDateTime lastTransitionTime) {
        this.type = type;
        this.status = status;
        this.reason = reason;
        this.message = message;
        this.errorCode = errorCode;
        this.component = component;
        this.lastTransitionTime = lastTransitionTime;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ConditionStatus getStatus() {
        return status;
    }

    public void setStatus(ConditionStatus status) {
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

    public static void copy(Condition c1, Condition c2) {
        c2.setStatus(c1.getStatus());
        c2.setType(c1.getType());
        c2.setComponent(c1.getComponent());
        c2.setMessage(c1.getMessage());
        c2.setReason(c1.getReason());
        c2.setErrorCode(c1.getErrorCode());
        c2.setLastTransitionTime(c1.getLastTransitionTime());
    }

    public static Condition from(ConditionDTO dto, ComponentType component) {
        Condition c = new Condition();
        c.setStatus(dto.getStatus());
        c.setType(dto.getType());
        c.setComponent(component);
        c.setMessage(dto.getMessage());
        c.setReason(dto.getReason());
        c.setErrorCode(dto.getErrorCode());
        c.setLastTransitionTime(dto.getLastTransitionTime());
        return c;
    }

    public static ConditionDTO to(Condition condition) {
        ConditionDTO dto = new ConditionDTO();
        dto.setStatus(condition.getStatus());
        dto.setType(condition.getType());
        dto.setMessage(condition.getMessage());
        dto.setReason(condition.getReason());
        dto.setErrorCode(condition.getErrorCode());
        dto.setLastTransitionTime(condition.getLastTransitionTime());
        return dto;
    }
}
