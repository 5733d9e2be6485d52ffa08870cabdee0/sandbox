package com.redhat.service.bridge.manager.models;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessorTest {

    @Test
    void toResponse() {
        Bridge b = new Bridge();
        b.setPublishedAt(ZonedDateTime.now());
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setStatus(BridgeStatus.AVAILABLE);
        b.setName(TestConstants.DEFAULT_BRIDGE_NAME);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setEndpoint("https://bridge.redhat.com");

        Processor p = new Processor();
        p.setName("foo");
        p.setStatus(BridgeStatus.AVAILABLE);
        p.setPublishedAt(ZonedDateTime.now());
        p.setSubmittedAt(ZonedDateTime.now());
        p.setBridge(b);
        p.setTransformationTemplate("");

        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);
        action.setName(TestConstants.DEFAULT_ACTION_NAME);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, "myTopic");
        action.setParameters(params);
        p.setAction(action);

        ProcessorResponse r = p.toResponse();
        assertThat(r).isNotNull();

        assertThat(r.getHref()).isEqualTo(APIConstants.USER_API_BASE_PATH + b.getId() + "/processors/" + p.getId());
        assertThat(r.getName()).isEqualTo(p.getName());
        assertThat(r.getStatus()).isEqualTo(p.getStatus());
        assertThat(r.getId()).isEqualTo(p.getId());
        assertThat(r.getSubmittedAt()).isEqualTo(p.getSubmittedAt());
        assertThat(r.getPublishedAt()).isEqualTo(p.getPublishedAt());
        assertThat(r.getKind()).isEqualTo("Processor");
        assertThat(r.getBridge()).isNotNull();
        assertThat(r.getTransformationTemplate()).isEmpty();
        assertThat(r.getAction().getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(r.getAction().getName()).isEqualTo(TestConstants.DEFAULT_ACTION_NAME);
    }
}
