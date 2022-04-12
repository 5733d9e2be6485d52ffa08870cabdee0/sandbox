package com.redhat.service.rhose.manager.utils;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.rhose.infra.models.actions.BaseAction;
import com.redhat.service.rhose.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.rhose.manager.TestConstants;
import com.redhat.service.rhose.manager.models.Bridge;
import com.redhat.service.rhose.manager.models.ConnectorEntity;
import com.redhat.service.rhose.manager.models.Processor;
import com.redhat.service.rhose.processor.actions.kafkatopic.KafkaTopicAction;

public class Fixtures {

    public static BaseAction createKafkaAction() {
        BaseAction action = new BaseAction();
        action.setType(KafkaTopicAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, "myTopic");
        action.setParameters(params);
        return action;
    }

    public static Processor createProcessor(Bridge b, ManagedResourceStatus status) {
        Processor p = new Processor();
        p.setName(TestConstants.DEFAULT_PROCESSOR_NAME);
        p.setStatus(status);
        p.setPublishedAt(ZonedDateTime.now());
        p.setSubmittedAt(ZonedDateTime.now());
        p.setBridge(b);
        p.setShardId(TestConstants.SHARD_ID);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("definitionKey", "definitionValue");
        p.setDefinition(objectNode);

        return p;
    }

    public static Bridge createBridge() {
        Bridge b = new Bridge();
        b.setPublishedAt(ZonedDateTime.now());
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setStatus(ManagedResourceStatus.READY);
        b.setName(TestConstants.DEFAULT_BRIDGE_NAME);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setEndpoint("https://bridge.redhat.com");
        return b;
    }

    public static ConnectorEntity createConnector(Processor p, ManagedResourceStatus status) {
        ConnectorEntity c = new ConnectorEntity();
        c.setName(TestConstants.DEFAULT_CONNECTOR_NAME);
        c.setProcessor(p);
        c.setStatus(status);
        c.setSubmittedAt(ZonedDateTime.now());
        c.setPublishedAt(ZonedDateTime.now());
        c.setDefinition(new TextNode("definition"));
        c.setTopicName(TestConstants.DEFAULT_KAFKA_TOPIC);
        c.setConnectorExternalId("connectorExternalId");

        return c;
    }

}
