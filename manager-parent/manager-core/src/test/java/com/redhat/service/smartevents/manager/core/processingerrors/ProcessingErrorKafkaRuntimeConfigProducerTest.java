package com.redhat.service.smartevents.manager.core.processingerrors;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.core.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.core.services.RhoasService;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessingErrorKafkaRuntimeConfigProducerTest {

    public static final String TEST_GLOBAL_ERROR_TOPIC_NAME = "global-err";

    @Test
    void test() {
        ResourceNamesProvider resourceNamesProviderMock = mock(ResourceNamesProvider.class);
        when(resourceNamesProviderMock.getGlobalErrorTopicName()).thenReturn(TEST_GLOBAL_ERROR_TOPIC_NAME);

        RhoasService rhoasServiceMock = mock(RhoasService.class);

        ProcessingErrorKafkaRuntimeConfigProducer producer = new ProcessingErrorKafkaRuntimeConfigProducer();
        producer.resourceNamesProvider = resourceNamesProviderMock;
        producer.rhoasService = rhoasServiceMock;

        Map<String, Object> config = producer.createKafkaRuntimeConfig();

        assertThat(config)
                .hasSize(1)
                .containsEntry("topic", TEST_GLOBAL_ERROR_TOPIC_NAME);

        verify(resourceNamesProviderMock).getGlobalErrorTopicName();
        verify(rhoasServiceMock).createTopicAndGrantAccessFor(TEST_GLOBAL_ERROR_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
    }
}
