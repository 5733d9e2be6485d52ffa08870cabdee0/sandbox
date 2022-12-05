package com.redhat.service.smartevents.integration.tests.context;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.cucumber.java.Scenario;

/**
 * Shared bridge context
 */
public class BridgeContext {

    private String name;
    private String id;

    private Map<String, ProcessorContext> processors = new HashMap<>();

    private Optional<URL> endPoint = Optional.empty();
    private String errorHandlerEndpoint;

    private boolean deleted;

    @JsonIgnore
    private Scenario scenario;

    public BridgeContext() {
    }

    public BridgeContext(Scenario scenario, String id, String systemBridgeName) {
        this.scenario = scenario;
        this.id = id;
        this.name = systemBridgeName;
    }

    public String getName() {
        return this.name;
    }

    public String getId() {
        return id;
    }

    @JsonIgnore
    public String getEndPointAuthority() {
        return endPoint.map(URL::getAuthority).orElse(null);
    }

    public String getEndPoint() {
        return endPoint.map(URL::toString).orElse(null);
    }

    @JsonIgnore
    public String getEndPointBaseUrl() {
        return endPoint.map(url -> url.getProtocol() + "://" + url.getAuthority()).orElse(null);
    }

    @JsonIgnore
    public String getEndPointPath() {
        return endPoint.map(URL::getPath).orElse(null);
    }

    public String getErrorHandlerEndpoint() {
        return errorHandlerEndpoint;
    }

    public void setEndPoint(String endPoint) {
        try {
            this.endPoint = Optional.of(new URL(endPoint));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL provided for bridge endpoint", e);
        }
    }

    public void setErrorHandlerEndpoint(String errorHandlerEndpoint) {
        this.errorHandlerEndpoint = errorHandlerEndpoint;
    }

    public ProcessorContext newProcessor(String processorName, String processorId) {
        scenario.log("Bridge '" + this.name + "': Creating new Processor context with name '" + processorName + "' and id '"
                + processorId + "'");
        ProcessorContext processorContext = new ProcessorContext(this.scenario, processorId);
        this.processors.put(processorName, processorContext);
        return processorContext;
    }

    public void removeProcessor(String processorName) {
        this.processors.get(processorName).setDeleted(true);
    }

    public ProcessorContext getProcessor(String processorName) {
        if (!this.processors.containsKey(processorName)) {
            throw new RuntimeException("Processor with name " + processorName + " does not exist in bridge context.");
        }
        return this.processors.get(processorName);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
