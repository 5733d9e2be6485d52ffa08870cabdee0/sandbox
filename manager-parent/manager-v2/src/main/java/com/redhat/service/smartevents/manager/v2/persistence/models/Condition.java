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
public class Condition extends ConditionDTO {

    private String id = UUID.randomUUID().toString();

    private ComponentType component;

    public Condition() {
        super();
    }

    public Condition(String type, ConditionStatus status) {
        super(type, status);
    }

    public Condition(ConditionDTO dto, ComponentType component) {
        super(dto.getType(), dto.getStatus());
        setReason(dto.getReason());
        setMessage(dto.getMessage());
        setErrorCode(dto.getErrorCode());
        setLastTransitionTime(dto.getLastTransitionTime());
        setComponent(component);
    }

    public Condition(String type, ConditionStatus status, String reason, String message, String errorCode, ComponentType component, ZonedDateTime lastTransitionTime) {
        super(type, status, reason, message, errorCode, lastTransitionTime);
        this.component = component;
    }

    @Id
    public String getId() {
        return id;
    }

    // We don't want to expose this, but it is required for method-level @Column annotations.
    // We're using method-level @Column annotations as we're extending the underlying DTO.
    void setId(String id) {
        this.id = id;
    }

    @Override
    @Column(name = "type", nullable = false)
    public String getType() {
        return super.getType();
    }

    @Override
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public ConditionStatus getStatus() {
        return super.getStatus();
    }

    @Override
    @Column(name = "reason")
    public String getReason() {
        return super.getReason();
    }

    @Override
    @Column(name = "message")
    public String getMessage() {
        return super.getMessage();
    }

    @Override
    @Column(name = "errorCode")
    public String getErrorCode() {
        return super.getErrorCode();
    }

    @Override
    @Column(name = "last_transition_time", columnDefinition = "TIMESTAMP", nullable = false)
    public ZonedDateTime getLastTransitionTime() {
        return super.getLastTransitionTime();
    }

    @Column(name = "component", nullable = false)
    @Enumerated(EnumType.STRING)
    public ComponentType getComponent() {
        return component;
    }

    public void setComponent(ComponentType component) {
        this.component = component;
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
