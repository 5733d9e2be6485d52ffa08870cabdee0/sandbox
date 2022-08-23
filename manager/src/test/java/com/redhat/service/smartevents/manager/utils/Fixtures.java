package com.redhat.service.smartevents.manager.utils;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.bridges.BridgeDefinition;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

public class Fixtures {

    public static Action createKafkaAction() {
        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, "myTopic");
        action.setMapParameters(params);
        return action;
    }

    public static Processor createProcessor(Bridge b, ManagedResourceStatus status) {
        Processor p = new Processor();
        p.setType(ProcessorType.SINK);
        p.setName(TestConstants.DEFAULT_PROCESSOR_NAME);
        p.setStatus(status);
        p.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        p.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        p.setBridge(b);
        p.setShardId(TestConstants.SHARD_ID);
        p.setOwner(TestConstants.DEFAULT_USER_NAME);
        Action requestedAction = createKafkaAction();
        Action resolvedAction = createKafkaAction();
        p.setDefinition(new ProcessorDefinition(new HashSet<>(), "",
                requestedAction,
                resolvedAction));
        return p;
    }

    public static Bridge createBridge() {
        Bridge b = new Bridge();
        b.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setOrganisationId(TestConstants.DEFAULT_ORGANISATION_ID);
        b.setOwner(TestConstants.DEFAULT_USER_NAME);
        b.setStatus(ManagedResourceStatus.READY);
        b.setName(TestConstants.DEFAULT_BRIDGE_NAME);
        b.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        b.setEndpoint("https://bridge.redhat.com");
        b.setDefinition(new BridgeDefinition());
        b.setCloudProvider(TestConstants.DEFAULT_CLOUD_PROVIDER);
        b.setRegion(TestConstants.DEFAULT_REGION);
        return b;
    }

    public static ConnectorEntity createSourceConnector(Processor p, ManagedResourceStatus status) {
        return createConnector(p, status, ConnectorType.SOURCE, "test_source_0.1");
    }

    public static ConnectorEntity createSinkConnector(Processor p, ManagedResourceStatus status) {
        return createConnector(p, status, ConnectorType.SINK, "test_sink_0.1");
    }

    private static ConnectorEntity createConnector(Processor p, ManagedResourceStatus status, ConnectorType type, String connectorTypeId) {
        ConnectorEntity connector = new ConnectorEntity();
        connector.setType(type);
        connector.setName(TestConstants.DEFAULT_CONNECTOR_NAME);
        connector.setProcessor(p);
        connector.setStatus(status);
        connector.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        connector.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
        connector.setDefinition(new TextNode("definition"));
        connector.setTopicName(TestConstants.DEFAULT_KAFKA_TOPIC);
        connector.setConnectorTypeId(connectorTypeId);
        connector.setConnectorExternalId("connectorExternalId");
        return connector;
    }
}
