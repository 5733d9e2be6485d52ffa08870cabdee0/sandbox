package com.redhat.service.smartevents.integration.tests.context;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.cucumber.java.Scenario;

/**
 * Shared processor context
 */
public class ProcessorContext {

    private String id;

    private boolean deleted;

    @JsonIgnore
    private Scenario scenario;

    public ProcessorContext() {
    }

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
