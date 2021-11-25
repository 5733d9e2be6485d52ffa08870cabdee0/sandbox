package com.redhat.service.bridge.manager.utils;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.Processor;

public class Fixtures {

    public static BaseAction createKafkaAction() {
        BaseAction action = new BaseAction();
        action.setType(KafkaTopicAction.TYPE);
        action.setName(TestConstants.DEFAULT_ACTION_NAME);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, "myTopic");
        action.setParameters(params);
        return action;
    }

    public static Processor createProcessor(Bridge b, String name) {
        Processor p = new Processor();
        p.setName(name);
        p.setStatus(BridgeStatus.AVAILABLE);
        p.setPublishedAt(ZonedDateTime.now());
        p.setSubmittedAt(ZonedDateTime.now());
        p.setBridge(b);
        p.setDefinition(new TextNode("definition"));
        return p;
    }

    public static Bridge createBridge() {
        Bridge b = new Bridge();
        b.setPublishedAt(ZonedDateTime.now());
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setStatus(BridgeStatus.AVAILABLE);
        b.setName(TestConstants.DEFAULT_BRIDGE_NAME);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setEndpoint("https://bridge.redhat.com");
        return b;
    }

}
