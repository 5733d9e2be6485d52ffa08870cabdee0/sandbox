package com.redhat.service.bridge.executor.actions.kafka;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.executor.actions.ActionInvoker;
import com.redhat.service.bridge.executor.actions.ActionInvokerException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.actions.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class KafkaTopicActionInvokerFactoryTest {

    private static final String TOPIC_NAME = "myTopic";

    @InjectMock
    AdminClient kafkaAdmin;

    @Inject
    KafkaTopicActionInvokerFactory factory;

    private Set<String> topics = Collections.singleton(TOPIC_NAME);

    private void mockKafkaAdmin() throws Exception {

        KafkaFuture<Set<String>> kafkaFuture = mock(KafkaFuture.class);
        when(kafkaFuture.get(KafkaTopicActionInvokerFactory.DEFAULT_LIST_TOPICS_TIMEOUT, KafkaTopicActionInvokerFactory.DEFAULT_LIST_TOPICS_TIMEUNIT)).thenReturn(topics);

        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(listTopicsResult.names()).thenReturn(kafkaFuture);
        when(kafkaAdmin.listTopics()).thenReturn(listTopicsResult);
    }

    private ProcessorDTO createProcessorWithActionForTopic(String topicName) {
        BaseAction b = new BaseAction();
        b.setType(KafkaTopicAction.KAFKA_ACTION_TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.KAFKA_ACTION_TOPIC_PARAM, topicName);
        b.setParameters(params);

        ProcessorDTO p = new ProcessorDTO();
        p.setId("myProcessor");
        p.setAction(b);

        BridgeDTO bridge = new BridgeDTO();
        bridge.setId("myBridge");
        p.setBridge(bridge);

        return p;
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        mockKafkaAdmin();
    }

    @Test
    public void accepts() {

        BaseAction b = new BaseAction();
        b.setType(KafkaTopicAction.KAFKA_ACTION_TYPE);

        assertThat(factory.accepts(b), is(true));
    }

    @Test
    public void accepts_notAMatchingAction() {
        BaseAction b = new BaseAction();
        b.setType("notASupportedType");

        assertThat(factory.accepts(b), is(false));
    }

    @Test
    public void buildActionInvoker() {
        ProcessorDTO p = createProcessorWithActionForTopic(TOPIC_NAME);
        ActionInvoker actionInvoker = factory.build(p, p.getAction());
        assertThat(actionInvoker, is(notNullValue()));

        verify(kafkaAdmin).listTopics();
    }

    @Test
    public void buildActionInvoker_requestedTopicDoesNotExist() {
        ProcessorDTO p = createProcessorWithActionForTopic("thisTopicDoesNotExist");
        Assertions.assertThrows(ActionInvokerException.class, () -> factory.build(p, p.getAction()));
        verify(kafkaAdmin).listTopics();
    }
}
