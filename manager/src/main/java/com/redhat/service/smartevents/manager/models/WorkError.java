package com.redhat.service.smartevents.manager.models;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.redhat.service.smartevents.infra.exceptions.BridgeErrorType;

@NamedQueries({
        @NamedQuery(name = "WorkError.findByManagedResourceId", query = "from WorkError w where w.managedResourceId=:managedResourceId order by timestamp desc"),
        @NamedQuery(name = "WorkError.deleteByManagedResourceId", query = "delete from WorkError w where w.managedResourceId=:managedResourceId")
})
@Entity
@Table(name = "WORK_ERRORS")
public class WorkError {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "managed_resource_id", nullable = false, updatable = false)
    private String managedResourceId;

    @Column(nullable = false, updatable = false)
    private String code;

    @Column(nullable = false, updatable = false)
    private String reason;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private BridgeErrorType type;

    @Column(columnDefinition = "TIMESTAMP", nullable = false, updatable = false)
    private ZonedDateTime timestamp;

    protected WorkError() {
        // Serialisation support
    }

    public WorkError(String managedResourceId, String code, String reason, BridgeErrorType type, ZonedDateTime timestamp) {
        this.managedResourceId = Objects.requireNonNull(managedResourceId);
        this.code = Objects.requireNonNull(code);
        this.reason = Objects.requireNonNull(reason);
        this.type = Objects.requireNonNull(type);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public String getId() {
        return id;
    }

    public String getManagedResourceId() {
        return managedResourceId;
    }

    public String getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    public BridgeErrorType getType() {
        return type;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkError)) {
            return false;
        }
        WorkError that = (WorkError) o;
        return getId().equals(that.getId()) && getManagedResourceId().equals(that.getManagedResourceId()) && getCode().equals(that.getCode()) && getReason().equals(that.getReason())
                && getType().equals(
                        that.getType())
                && getTimestamp().equals(that.getTimestamp());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getManagedResourceId(), getCode(), getReason(), getType(), getTimestamp());
    }

    @Override
    public String toString() {
        return "ManagedResourceWorkStatus{" +
                "id='" + id + '\'' +
                ", managedResourceId='" + managedResourceId + '\'' +
                ", code='" + code + '\'' +
                ", reason='" + reason + '\'' +
                ", type='" + type + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}