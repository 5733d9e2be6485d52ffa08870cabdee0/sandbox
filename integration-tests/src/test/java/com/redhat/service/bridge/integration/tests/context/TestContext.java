package com.redhat.service.bridge.integration.tests.context;

import io.cucumber.java.Scenario;

/**
 * Shared scenario context
 */
public class TestContext {

    private String managerToken;

    private String randomBridgeName;
    private String bridgeId;
    private String processorId;
    private String endPoint;

    private Scenario scenario;

    public TestContext() {
    }

    public String getManagerToken() {
        return managerToken;
    }

    public void setManagerToken(String managerToken) {
        this.managerToken = managerToken;
    }

    public String getRandomBridgeName() {
        return randomBridgeName;
    }

    public void setRandomBridgeName(String randomBridgeName) {
        this.randomBridgeName = randomBridgeName;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    public String getProcessorId() {
        return processorId;
    }

    public void setProcessorId(String processorId) {
        this.processorId = processorId;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }
}
