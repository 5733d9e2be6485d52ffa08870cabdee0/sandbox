package com.redhat.service.bridge.integration.tests.context;

import io.cucumber.java.Scenario;

/**
 * Shared processor context
 */
public class ProcessorContext {

    private String id;

    private boolean deleted;

    private Scenario scenario;

    public ProcessorContext(Scenario scenario, String id) {
        this.scenario = scenario;
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

    public Scenario getScenario() {
        return scenario;
    }
}
