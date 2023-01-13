package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class KafkaTopicActionInvokerBuilderTest {

    private static final String TOPIC_NAME = "myTopic";

    @Inject
    KafkaTopicActionInvokerBuilder builder;

    @InjectMock
    @Named("outboundAdminClient")
    AdminClient kafkaAdmin;

    private Set<String> topics = Collections.singleton(TOPIC_NAME);

    @BeforeEach
    public void beforeEach() throws Exception {
        mockKafkaAdmin();
    }

    @Test
    void getActionInvoker() {
        ProcessorDTO p = createProcessorWithActionForTopic(TOPIC_NAME);
        ActionInvoker actionInvoker = builder.build(p, p.getDefinition().getResolvedAction());
        assertThat(actionInvoker).isNotNull();

        verify(kafkaAdmin).listTopics();
    }

    @Test
    void getActionInvoker_requestedTopicDoesNotExist() {
        ProcessorDTO p = createProcessorWithActionForTopic("thisTopicDoesNotExist");
        assertThatExceptionOfType(GatewayProviderException.class).isThrownBy(() -> builder.build(p, p.getDefinition().getResolvedAction()));
        verify(kafkaAdmin).listTopics();
    }

    private void mockKafkaAdmin() throws Exception {
        KafkaFuture<Set<String>> kafkaFuture = mock(KafkaFuture.class);
        when(kafkaFuture.get(KafkaTopicActionInvokerBuilder.DEFAULT_LIST_TOPICS_TIMEOUT, KafkaTopicActionInvokerBuilder.DEFAULT_LIST_TOPICS_TIMEUNIT)).thenReturn(topics);

        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(listTopicsResult.names()).thenReturn(kafkaFuture);
        when(kafkaAdmin.listTopics()).thenReturn(listTopicsResult);
    }

    private ProcessorDTO createProcessorWithActionForTopic(String topicName) {
        Action b = new Action();
        b.setType(KafkaTopicAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, topicName);
        b.setMapParameters(params);

        ProcessorDTO p = new ProcessorDTO();
        p.setType(ProcessorType.SINK);
        p.setId("myProcessor");
        p.setDefinition(new ProcessorDefinition(null, null, b));
        p.setBridgeId("myBridge");

        return p;
    }
}
