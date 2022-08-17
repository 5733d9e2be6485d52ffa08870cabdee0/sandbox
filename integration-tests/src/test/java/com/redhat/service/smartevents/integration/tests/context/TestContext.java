package com.redhat.service.smartevents.integration.tests.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.cucumber.java.Scenario;

/**
 * Shared scenario context
 */
public class TestContext {

    private static final String KAFKA_TOPIC_PREFIX = "it-test-";

    private String managerToken;

    private Map<String, BridgeContext> bridges = new HashMap<>();
    private Map<String, String> cloudEvents = new HashMap<>();
    private Map<String, String> uuids = new HashMap<>();
    private Map<String, String> kafkaTopics = new HashMap<>();
    private Map<String, String> testData = new HashMap<>();

    private Scenario scenario;

    public TestContext() {
    }

    public String getManagerToken() {
        return this.managerToken;
    }

    public void setManagerToken(String managerToken) {
        this.managerToken = managerToken;
    }

    /**
     * This creates a new bridge in the test context
     * 
     * @param testBridgeName Name of the new bridge so you are able to easily
     *        reference it in your tests without having to care
     *        about the uniqueness of the name
     * @param systemBridgeName Used name of the bridge on the system which will so
     *        be unique on the system where the test happens
     * @return the new test bridge context
     */
    public BridgeContext newBridge(String testBridgeName, String bridgeId, String systemBridgeName) {
        if (this.bridges.containsKey(testBridgeName)) {
            throw new RuntimeException("Bridge with name " + testBridgeName + " is already created in context.");
        } else {
            scenario.log("Creating new Bridge context with test name '" + testBridgeName + "' and system name '"
                    + systemBridgeName + "'");
            BridgeContext bridgeContext = new BridgeContext(this.scenario, bridgeId, systemBridgeName);
            this.bridges.put(testBridgeName, bridgeContext);
        }
        return getBridge(testBridgeName);
    }

    public void removeBridge(String testBridgeName) {
        this.bridges.get(testBridgeName).setDeleted(true);
    }

    public BridgeContext getBridge(String testBridgeName) {
        if (!this.bridges.containsKey(testBridgeName)) {
            throw new RuntimeException("Bridge with name " + testBridgeName + " does not exist in context.");
        }
        return this.bridges.get(testBridgeName);
    }

    public Map<String, BridgeContext> getAllBridges() {
        return this.bridges;
    }

    public String getUuid(String uuidName) {
        if (!this.uuids.containsKey(uuidName)) {
            String uuidValue = UUID.randomUUID().toString();
            scenario.log("Generating new uuid '" + uuidName + "' value '" + uuidValue + "'");
            this.uuids.put(uuidName, uuidValue);
        }
        return this.uuids.get(uuidName);
    }

    public String getKafkaTopic(String topicName) {
        if (!this.kafkaTopics.containsKey(topicName)) {
            String uuidValue = UUID.randomUUID().toString().substring(0, 8);
            String uniqueTopicName = KAFKA_TOPIC_PREFIX + topicName + "-" + uuidValue;
            scenario.log("Generating new Kafka topic name '" + uniqueTopicName);
            this.kafkaTopics.put(topicName, uniqueTopicName);
        }

        return this.kafkaTopics.get(topicName);
    }

    public Collection<String> allKafkaTopics() {
        return this.kafkaTopics.values();
    }

    public Scenario getScenario() {
        return this.scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    /**
     * @param testCloudEventId ID of the new Cloud event sent so you are able to
     *        easily reference it in your tests without having to
     *        care about the uniqueness of the name
     */
    public void storeCloudEventInContext(String testCloudEventId) {
        if (cloudEvents.containsKey(testCloudEventId)) {
            throw new RuntimeException("Cloud event with id " + testCloudEventId + " is already created in context.");
        }
        String systemCloudEventId = UUID.randomUUID().toString();
        scenario.log("Store cloud event with test id '" + testCloudEventId + "' and system id '" + systemCloudEventId + "'");
        cloudEvents.put(testCloudEventId, systemCloudEventId);
    }

    public String getCloudEventSystemId(String testCloudEventId) {
        if (!cloudEvents.containsKey(testCloudEventId)) {
            throw new RuntimeException("Cloud event with id " + testCloudEventId + " not found.");
        }
        return cloudEvents.get(testCloudEventId);
    }

    public String getTestData(String key) {
        return this.testData.get(key);
    }

    public void setTestData(String key, String value) {
        this.testData.put(key, value);
    }
}
