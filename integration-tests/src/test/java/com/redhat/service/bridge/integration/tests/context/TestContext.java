package com.redhat.service.bridge.integration.tests.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.cucumber.java.Scenario;

/**
 * Shared scenario context
 */
public class TestContext {

    private String managerToken;

    private Map<String, BridgeContext> bridges = new HashMap<>();
    private Map<String, BridgeContext> removedBridges = new HashMap<>();

    private Scenario scenario;

    public TestContext() {
    }

    public String getManagerToken() {
        return this.managerToken;
    }

    public void setManagerToken(String managerToken) {
        this.managerToken = managerToken;
    }

    public BridgeContext newBridge(String testBridgeName, String systemBridgeName) {
        if (this.bridges.containsKey(testBridgeName)) {
            throw new RuntimeException("Bridge with name " + testBridgeName + " is already created in context.");
        } else {
            this.bridges.put(testBridgeName, new BridgeContext(systemBridgeName));
        }
        return getBridge(testBridgeName);
    }

    public void removeBridge(String testBridgeName) {
        BridgeContext bridgeContext = this.bridges.remove(testBridgeName);
        this.removedBridges.put(testBridgeName, bridgeContext);
    }

    public BridgeContext getBridge(String testBridgeName) {
        return getBridge(testBridgeName, false);
    }

    public BridgeContext getBridge(String testBridgeName, boolean includeRemovedBridges) {
        if (!this.bridges.containsKey(testBridgeName)) {
            if (includeRemovedBridges && this.removedBridges.containsKey(testBridgeName)) {
                return this.removedBridges.get(testBridgeName);
            }
            throw new RuntimeException("Bridge with name " + testBridgeName + " does not exist in context.");
        }
        return this.bridges.get(testBridgeName);
    }

    public List<String> getAllBridgeNames() {
        return this.bridges.keySet().stream().collect(Collectors.toList());
    }

    public Scenario getScenario() {
        return this.scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }
}
