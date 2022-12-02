package com.redhat.service.smartevents.manager.v2.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

public class Fixtures {

    public static Processor createProcessor(Bridge b) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        Processor p = new Processor();
        p.setOperation(operation);
        p.setName(TestConstants.DEFAULT_PROCESSOR_NAME);
        p.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        p.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        p.setBridge(b);
        p.setOwner(TestConstants.DEFAULT_USER_NAME);
        p.setFlows(JsonNodeFactory.instance.objectNode());
        return p;
    }

    public static Bridge createBridge() {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        return createBridge(TestConstants.DEFAULT_BRIDGE_NAME, operation, null);
    }

    public static Bridge createReadyBridge(String name) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        Condition managerCondition = new Condition();
        managerCondition.setType("ManagerReady");
        managerCondition.setComponent(ComponentType.MANAGER);
        managerCondition.setStatus(ConditionStatus.TRUE);
        managerCondition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));

        Condition shardCondition = new Condition();
        shardCondition.setType("ShardReady");
        shardCondition.setComponent(ComponentType.SHARD);
        shardCondition.setStatus(ConditionStatus.TRUE);
        shardCondition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));

        List<Condition> conditions = List.of(managerCondition, shardCondition);

        return createBridge(name, operation, conditions);
    }

    public static Bridge createAcceptedBridge(String name) {
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        Condition managerCondition = new Condition();
        managerCondition.setType("ManagerReady");
        managerCondition.setComponent(ComponentType.MANAGER);
        managerCondition.setStatus(ConditionStatus.UNKNOWN);
        managerCondition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));

        Condition shardCondition = new Condition();
        shardCondition.setType("ShardReady");
        shardCondition.setComponent(ComponentType.SHARD);
        shardCondition.setStatus(ConditionStatus.UNKNOWN);
        shardCondition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));

        List<Condition> conditions = List.of(managerCondition, shardCondition);

        return createBridge(name, operation, conditions);
    }

    private static Bridge createBridge(String name, Operation operation, List<Condition> conditions) {
        Bridge b = new Bridge();
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

    public static Condition createCondition() {
        Condition condition = new Condition();
        condition.setComponent(ComponentType.MANAGER);
        condition.setStatus(ConditionStatus.TRUE);
        condition.setType("DNSReady");
        condition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));
        return condition;
    }
}
