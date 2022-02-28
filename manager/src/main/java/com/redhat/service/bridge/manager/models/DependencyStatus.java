package com.redhat.service.bridge.manager.models;

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

    @Override
    public String toString() {
        return "DependencyStatus{" +
                "ready=" + ready +
                ", deleted=" + deleted +
                ", attempts=" + attempts +
                '}';
    }
}
