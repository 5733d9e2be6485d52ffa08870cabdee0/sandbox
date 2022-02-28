package com.redhat.service.bridge.manager.workers;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

import com.redhat.service.bridge.manager.models.ManagedResource;

@NamedQueries({
        @NamedQuery(name = "Work.findByManagedResourceId", query = "from Work w where w.managedResourceId=:managedResourceId"),
        @NamedQuery(name = "Work.findByWorkerId", query = "from Work w where w.workerId=:workerId"),
        @NamedQuery(name = "Work.updateWorkerId", query = "update Work w set w.workerId=:workerId where w.scheduledAt >= :age")
})
@Entity
public class Work {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "managed_resource_id", nullable = false, updatable = false)
    private String managedResourceId;

    @Column(nullable = false, updatable = false)
    private String type;

    @Column(name = "worker_id", nullable = false)
    private String workerId;

    @Column(name = "scheduled_at", nullable = false)
    private ZonedDateTime scheduledAt;

    @Version
    @SuppressWarnings("unused")
    private long version;

    public String getId() {
        return id;
    }

    public String getManagedResourceId() {
        return managedResourceId;
    }

    public void setManagedResourceId(String entityId) {
        this.managedResourceId = entityId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public ZonedDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(ZonedDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static Work forResource(ManagedResource managedResource, String workerId) {
        Work w = new Work();
        w.setScheduledAt(ZonedDateTime.now());
        w.setType(managedResource.getClass().getSimpleName());
        w.setManagedResourceId(managedResource.getId());
        w.setWorkerId(workerId);
        return w;
    }
}