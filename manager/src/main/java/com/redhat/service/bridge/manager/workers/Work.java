package com.redhat.service.bridge.manager.workers;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

import com.redhat.service.bridge.manager.models.ManagedEntity;

/*
    A work item. Encapsulates that we need to perform some work on an Entity to get all of the resources
    on which it depends ready.
 */
@NamedQueries({
        @NamedQuery(name = "Work.findByEntityId", query = "from Work w where w.entityId=:entityId"),
        @NamedQuery(name = "Work.findByWorkerId", query = "from Work w where w.workerId=:workerId"),
        @NamedQuery(name = "Work.updateWorkerId", query = "update Work w set w.workerId=:workerId where w.scheduledAt >= :age")
})
@Entity
public class Work {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, updatable = false)
    private String entityId;

    @Column(nullable = false, updatable = false)
    private String type;

    @Column(nullable = false)
    private String workerId;

    @Column(nullable = false)
    private ZonedDateTime scheduledAt;

    @Version
    private long version;

    public String getId() {
        return id;
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

    public static Work forResource(ManagedEntity managedEntity, String workerId) {
        Work w = new Work();
        w.setScheduledAt(ZonedDateTime.now());
        w.setType(managedEntity.getClass().getSimpleName());
        w.setEntityId(managedEntity.getId());
        w.setWorkerId(workerId);
        return w;
    }
}
