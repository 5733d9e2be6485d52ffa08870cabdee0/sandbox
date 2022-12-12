package com.redhat.service.smartevents.manager.v2.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
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

        return createBridge(id, name, operation, createBridgeReadyConditions());
    }

    public static Bridge createAcceptedBridge(String id, String name) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createBridge(id, name, operation, createBridgeAcceptedConditions());
    }

    public static Bridge createDeprovisionBridge(String id, String name) {
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
        b.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setOrganisationId(TestConstants.DEFAULT_ORGANISATION_ID);
        b.setOwner(TestConstants.DEFAULT_USER_NAME);
        b.setName(name);
        b.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        b.setEndpoint("https://bridge.redhat.com");
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

    public static Processor createReadyProcessor(Bridge b) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createProcessor(DEFAULT_PROCESSOR_ID, b, DEFAULT_PROCESSOR_NAME, operation, createProcessorReadyConditions());
    }

    public static Processor createProvisioningProcessor(Bridge b) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createProcessor(DEFAULT_PROCESSOR_ID, b, DEFAULT_PROCESSOR_NAME, operation, createProcessorProvisioningConditions());
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
        p.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        p.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        p.setBridge(b);
        p.setOwner(TestConstants.DEFAULT_USER_NAME);
        p.setDefinition(new ProcessorDefinition(JsonNodeFactory.instance.objectNode()));
        p.setConditions(conditions);
        return p;
    }

    public static List<Condition> createBridgeAcceptedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
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

    public static List<Condition> createBridgeDeprovisionConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_DNS_RECORD_DELETED_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_DELETED_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_DATA_PLANE_DELETED_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createProcessorAcceptedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createProcessorReadyConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.TRUE, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createProcessorPreparingConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.FALSE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createProcessorProvisioningConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
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
