package com.redhat.service.smartevents.manager.v2.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.service.smartevents.infra.core.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.connectors.ConnectorDefinition;
import com.redhat.service.smartevents.infra.v2.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_EXTERNAL_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_TOPIC_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CONNECTOR_TYPE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PROCESSOR_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PROCESSOR_NAME;

public class Fixtures {

    public static Bridge createBridge() {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createBridge(DEFAULT_BRIDGE_ID, DEFAULT_BRIDGE_NAME, operation, null);
    }

    public static Bridge createReadyBridge(String id, String name) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        Bridge b = createBridge(id, name, operation, createBridgeReadyConditions());
        b.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        return b;
    }

    public static Bridge createAcceptedBridge(String id, String name) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createBridge(id, name, operation, createBridgeAcceptedConditions());
    }

    public static Bridge createProvisioningBridge(String id, String name) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createBridge(id, name, operation, createBridgeProvisionConditions());
    }

    public static Bridge createDeprovisioningBridge(String id, String name) {
        Operation operation = new Operation();
        operation.setType(OperationType.DELETE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createBridge(id, name, operation, createBridgeDeprovisionConditions());
    }

    public static Bridge createFailedBridge(String id, String name) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createBridge(id, name, operation, createFailedConditions());
    }

    private static Bridge createBridge(String id, String name, Operation operation, List<Condition> conditions) {
        Bridge b = new Bridge();
        b.setId(id);
        b.setOperation(operation);
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setOrganisationId(TestConstants.DEFAULT_ORGANISATION_ID);
        b.setOwner(TestConstants.DEFAULT_USER_NAME);
        b.setName(name);
        b.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        b.setEndpoint(TestConstants.DEFAULT_BRIDGE_ENDPOINT);
        b.setCloudProvider(TestConstants.DEFAULT_CLOUD_PROVIDER);
        b.setRegion(TestConstants.DEFAULT_REGION);
        b.setSubscriptionId(UUID.randomUUID().toString());
        b.setShardId(TestConstants.SHARD_ID);
        b.setConditions(conditions);
        return b;
    }

    public static Processor createProcessor(Bridge b) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createProcessor(DEFAULT_PROCESSOR_ID, b, DEFAULT_PROCESSOR_NAME, operation, null);
    }

    public static Processor createAcceptedProcessor(Bridge b) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createProcessor(DEFAULT_PROCESSOR_ID, b, DEFAULT_PROCESSOR_NAME, operation, createProcessorAcceptedConditions());
    }

    public static Processor createReadyProcessor(Bridge b) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        Processor p = createProcessor(DEFAULT_PROCESSOR_ID, b, DEFAULT_PROCESSOR_NAME, operation, createProcessorReadyConditions());
        p.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        return p;
    }

    public static Processor createPreparingProcessor(Bridge b) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createProcessor(DEFAULT_PROCESSOR_ID, b, DEFAULT_PROCESSOR_NAME, operation, createProcessorPreparingConditions());
    }

    public static Processor createProvisioningProcessor(Bridge b) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createProcessor(DEFAULT_PROCESSOR_ID, b, DEFAULT_PROCESSOR_NAME, operation, createProcessorProvisioningConditions());
    }

    public static Processor createDeprovisionProcessor(Bridge b) {
        Operation operation = new Operation();
        operation.setType(OperationType.DELETE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createProcessor(DEFAULT_PROCESSOR_ID, b, DEFAULT_PROCESSOR_NAME, operation, createProcessorDeprovisionConditions());
    }

    public static Processor createFailedProcessor(Bridge b) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createProcessor(DEFAULT_PROCESSOR_ID, b, DEFAULT_PROCESSOR_NAME, operation, createFailedConditions());
    }

    public static Processor createProcessor(Bridge b, String name) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createProcessor(DEFAULT_PROCESSOR_ID, b, name, operation, null);
    }

    public static Processor createProcessor(String id, Bridge b, String name) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createProcessor(id, b, name, operation, null);
    }

    public static Processor createProcessor(String id, Bridge b, String name, Operation operation, List<Condition> conditions) {
        Processor p = new Processor();
        p.setId(id);
        p.setName(name);
        p.setOperation(operation);
        p.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        p.setBridge(b);
        p.setOwner(TestConstants.DEFAULT_USER_NAME);
        p.setDefinition(new ProcessorDefinition(JsonNodeFactory.instance.objectNode()));
        p.setConditions(conditions);
        return p;
    }

    public static Connector createConnector(Bridge b, ConnectorType type) {
        return createConnector(b, type, DEFAULT_CONNECTOR_ID, DEFAULT_CONNECTOR_NAME);
    }

    public static Connector createConnector(Bridge b, ConnectorType type, String id, String name) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createConnector(id, b, name, operation, type, null);
    }

    public static Connector createReadyConnector(Bridge b, ConnectorType type) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        Connector c = createConnector(DEFAULT_CONNECTOR_ID, b, DEFAULT_CONNECTOR_NAME, operation, type, createConnectorReadyConditions());
        c.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        return c;
    }

    public static Connector createConnector(String id, Bridge b, String name, Operation operation, ConnectorType type, List<Condition> conditions) {
        Connector c = new Connector();
        c.setId(id);
        c.setType(type);
        c.setName(name);
        c.setConnectorExternalId(DEFAULT_CONNECTOR_EXTERNAL_ID);
        c.setConnectorTypeId(DEFAULT_CONNECTOR_TYPE_ID);
        c.setTopicName(DEFAULT_CONNECTOR_TOPIC_NAME);
        c.setOperation(operation);
        c.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        c.setBridge(b);
        c.setOwner(TestConstants.DEFAULT_USER_NAME);
        c.setDefinition(new ConnectorDefinition(JsonNodeFactory.instance.objectNode()));
        c.setConditions(conditions);
        return c;
    }

    public static List<Condition> createBridgeAcceptedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DNS_RECORD_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createBridgePreparingConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.FALSE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DNS_RECORD_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createBridgeReadyConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DNS_RECORD_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.TRUE, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createBridgeProvisionConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_DNS_RECORD_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.FALSE, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createBridgeDeprovisionConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_DNS_RECORD_DELETED_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_DELETED_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_DELETED_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createBridgeDeletingConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_DNS_RECORD_DELETED_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_DELETED_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_DELETED_NAME, ConditionStatus.FALSE, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createProcessorAcceptedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_CONTROL_PLANE_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createProcessorReadyConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_CONTROL_PLANE_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.TRUE, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createProcessorPreparingConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_CONTROL_PLANE_READY_NAME, ConditionStatus.FALSE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createProcessorProvisioningConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_CONTROL_PLANE_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.FALSE, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createProcessorDeprovisionConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_CONTROL_PLANE_DELETED_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_DELETED_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createProcessorDeletingConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_CONTROL_PLANE_DELETED_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_DELETED_NAME, ConditionStatus.FALSE, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createConnectorAcceptedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_CONTROL_PLANE_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createConnectorReadyConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_CONTROL_PLANE_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.TRUE, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createConnectorFailedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_CONTROL_PLANE_READY_NAME, ConditionStatus.FAILED, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createFailedConditions() {
        List<Condition> conditions = new ArrayList<>();
        Condition failedCondition = createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.FAILED, ComponentType.MANAGER);
        failedCondition.setErrorCode(TestConstants.FAILED_CONDITION_ERROR_CODE);
        failedCondition.setMessage(TestConstants.FAILED_CONDITION_ERROR_MESSAGE);
        conditions.add(failedCondition);
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static Condition createCondition() {
        return createCondition(DefaultConditions.CP_DNS_RECORD_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER);
    }

    public static Condition createCondition(String type, ConditionStatus status, ComponentType component) {
        Condition condition = new Condition();
        condition.setComponent(component);
        condition.setStatus(status);
        condition.setType(type);
        condition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));
        return condition;
    }
}
