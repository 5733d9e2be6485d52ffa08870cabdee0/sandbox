package com.redhat.service.bridge.integration.tests.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared bridge context
 */
public class BridgeContext {

    private String bridgeName;
    private String bridgeId;

    private Map<String, String> processors = new HashMap<>();
    private Map<String, String> removedProcessors = new HashMap<>();

    private String endPoint;

    public BridgeContext(String bridgeName) {
    }

    public String getBridgeName() {
        return this.bridgeName;
    }

    public void setBridgeName(String bridgeName) {
        this.bridgeName = bridgeName;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    public String getEndPoint() {
        return this.endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public void newProcessor(String processorName, String processorId) {
        this.processors.put(processorName, processorId);
    }

    public void removeProcessor(String processorName) {
        String processorId = this.processors.remove(processorName);
        this.removedProcessors.put(processorName, processorId);
    }

    public String getProcessorId(String processorName) {
        return getProcessorId(processorName, false);
    }

    public String getProcessorId(String processorName, boolean includeRemovedProcessors) {
        if (!this.processors.containsKey(processorName)) {
            if (includeRemovedProcessors && this.removedProcessors.containsKey(processorName)) {
                return this.removedProcessors.get(processorName);
            }
            throw new RuntimeException("Processor with name " + removedProcessors + " does not exist in bridge context.");
        }
        return this.processors.get(processorName);
    }

    public String getRemovedProcessorId(String processorName) {
        return this.removedProcessors.get(processorName);
    }
}
