package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class KafkaTopicActionInvokerBuilderTest {

    private static final String TOPIC_NAME = "myTopic";

    @Inject
    KafkaTopicActionInvokerBuilder builder;

    @InjectMock
    AdminClient kafkaAdmin;

    private Set<String> topics = Collections.singleton(TOPIC_NAME);

    @BeforeEach
    public void beforeEach() throws Exception {
        mockKafkaAdmin();
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
