package com.redhat.service.bridge.manager.models;

import java.time.ZonedDateTime;

/*
    This class encapsulates the status of dependencies that are required to support a ManagedEntity.
    It includes whether the dependencies are ready or deleted, and the number of attempts we've made to
    get them into the required state.
 */
public class DependencyStatus {

    boolean ready;

    boolean deleted;

    int attempts;

    public boolean isReady() {
        return ready;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public int getAttempts() {
        return attempts;
    }

    public void recordAttempt() {
        this.attempts++;
    }

    public void resetAttempts() {
        this.attempts = 0;
    }
}