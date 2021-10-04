package com.redhat.service.bridge.actions.kafkatopic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.actions.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class KafkaTopicActionTest {

    private static final String TOPIC_NAME = "myTopic";

    @InjectMock
    AdminClient kafkaAdmin;

    @Inject
    KafkaTopicAction kafkaTopicAction;

    private Set<String> topics = Collections.singleton(TOPIC_NAME);

    private void mockKafkaAdmin() throws Exception {
        KafkaFuture<Set<String>> kafkaFuture = mock(KafkaFuture.class);
        when(kafkaFuture.get(KafkaTopicAction.DEFAULT_LIST_TOPICS_TIMEOUT, KafkaTopicAction.DEFAULT_LIST_TOPICS_TIMEUNIT)).thenReturn(topics);

        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(listTopicsResult.names()).thenReturn(kafkaFuture);
        when(kafkaAdmin.listTopics()).thenReturn(listTopicsResult);
    }

    private ProcessorDTO createProcessorWithActionForTopic(String topicName) {
        BaseAction b = new BaseAction();
        b.setType(KafkaTopicAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, topicName);
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
    public void getType() {
        assertThat(kafkaTopicAction.getType()).isEqualTo(KafkaTopicAction.TYPE);
    }

    @Test
    public void getValidator() {
        assertThat(kafkaTopicAction.getParameterValidator()).isNotNull();
    }

    @Test
    public void getActionInvoker() {
        ProcessorDTO p = createProcessorWithActionForTopic(TOPIC_NAME);
        ActionInvoker actionInvoker = kafkaTopicAction.getActionInvoker(p, p.getAction());
        assertThat(actionInvoker).isNotNull();

        verify(kafkaAdmin).listTopics();
    }

    @Test
    public void getActionInvoker_requestedTopicDoesNotExist() {
        ProcessorDTO p = createProcessorWithActionForTopic("thisTopicDoesNotExist");
        assertThatExceptionOfType(ActionProviderException.class).isThrownBy(() -> kafkaTopicAction.getActionInvoker(p, p.getAction()));
        verify(kafkaAdmin).listTopics();
    }
}
