package com.redhat.service.smartevents.manager.v2.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.ManagedResourceV2;
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

        Bridge b = new Bridge();
        b.setOperation(operation);
        b.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
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
        return b;
    }

    public static Condition createCondition(ManagedResourceV2 managedResourceV2) {
        Condition condition = new Condition();
        condition.setComponent(ComponentType.MANAGER);
        condition.setStatus("True");
        condition.setType("DNSReady");
        condition.setManagedResourceId(managedResourceV2.getId());
        condition.setLastTransitionTime(ZonedDateTime.now(ZoneOffset.UTC));
        return condition;
    }
}
