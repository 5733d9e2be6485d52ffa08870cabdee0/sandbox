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

public class Fixtures {

    public static Bridge createBridge() {
        return createBridge(null);
    }

    public static Bridge createBridge(List<Condition> conditions) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        Bridge b = new Bridge();
        b.setOperation(operation);
        b.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        b.setId(TestConstants.DEFAULT_BRIDGE_ID);
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setOrganisationId(TestConstants.DEFAULT_ORGANISATION_ID);
        b.setOwner(TestConstants.DEFAULT_USER_NAME);
        b.setName(TestConstants.DEFAULT_BRIDGE_NAME);
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
        return createProcessor(b, TestConstants.DEFAULT_PROCESSOR_NAME);
    }

    public static Processor createProcessor(Bridge b, List<Condition> conditions) {
        return createProcessor(b, TestConstants.DEFAULT_PROCESSOR_NAME, conditions);
    }

    public static Processor createProcessor(Bridge b, String name) {
        return createProcessor(b, name, null);
    }

    public static Processor createProcessor(Bridge b, String name, List<Condition> conditions) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        Processor p = new Processor();
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

    public static List<Condition> createAcceptedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createReadyConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.TRUE, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createPreparingConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_PERMISSIONS_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createProvisioningConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createDeprovisioningConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
        return conditions;
    }

    public static List<Condition> createDeletingConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(createCondition(DefaultConditions.CP_KAFKA_TOPIC_READY_NAME, ConditionStatus.TRUE, ComponentType.MANAGER));
        conditions.add(createCondition(DefaultConditions.DP_SECRET_READY_NAME, ConditionStatus.UNKNOWN, ComponentType.SHARD));
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
