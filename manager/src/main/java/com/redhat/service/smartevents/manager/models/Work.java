package com.redhat.service.smartevents.manager.models;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class Work {

    private String managedResourceId;
    private String type;
    private ZonedDateTime submittedAt;
    private long attempts = 0;

    public String getManagedResourceId() {
        return managedResourceId;
    }

    public void setManagedResourceId(String entityId) {
        this.managedResourceId = entityId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public long getAttempts() {
        return attempts;
    }

    public void setAttempts(long attempts) {
        this.attempts = attempts;
    }

    public static Work forResource(ManagedResource managedResource) {
        Work w = new Work();
        w.setManagedResourceId(managedResource.getId());
        w.setType(managedResource.getClass().getName());
        w.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        return w;
    }

    @Override
    public String toString() {
        return "Work{" +
                "managedResourceId='" + getManagedResourceId() + '\'' +
                ", type='" + getType() + '\'' +
                ", submittedAt=" + getSubmittedAt() +
                ", attempts=" + getAttempts() +
                '}';
    }
}