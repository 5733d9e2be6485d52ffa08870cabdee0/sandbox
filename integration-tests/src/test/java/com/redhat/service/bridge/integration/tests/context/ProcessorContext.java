package com.redhat.service.bridge.integration.tests.context;

/**
 * Shared processor context
 */
public class ProcessorContext {

    private String id;

    private boolean deleted;

    public ProcessorContext(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
