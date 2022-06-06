package com.redhat.service.smartevents.executor;

import java.net.URI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.smartevents.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.utils.CloudEventUtils;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;

public class ExecutorTestUtils {

    public static final String PLAIN_EVENT_JSON = "{\"key\":\"value\"}";

    public static final URI CLOUD_EVENT_SOURCE = URI.create("mySource");
    public static final String CLOUD_EVENT_TYPE = "TestEvent";
    public static final String CLOUD_EVENT_ID = "myId";
    public static final String CLOUD_EVENT_SUBJECT = "subject";

    public static CloudEvent createCloudEvent() {
        try {
            JsonNode data = CloudEventUtils.getMapper().readTree(PLAIN_EVENT_JSON);
            return CloudEventUtils.builderFor(SpecVersion.V1, CLOUD_EVENT_ID, CLOUD_EVENT_SOURCE, CLOUD_EVENT_TYPE, CLOUD_EVENT_SUBJECT, data)
                    .withType(CLOUD_EVENT_TYPE)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String createCloudEventString() {
        return CloudEventUtils.encode(createCloudEvent());
    }

    public static ProcessorDTO createSourceProcessor() {
        Source requestedSource = new Source();
        requestedSource.setType(SlackSource.TYPE);

        Action resolvedAction = new Action();
        resolvedAction.setType(WebhookAction.TYPE);

        return createProcessor(ProcessorType.SOURCE, new ProcessorDefinition(null, null, requestedSource, resolvedAction));
    }

    public static ProcessorDTO createSinkProcessorWithSameAction() {
        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);

        return createProcessor(ProcessorType.SINK, new ProcessorDefinition(null, null, action));
    }

    public static ProcessorDTO createSinkProcessorWithResolvedAction() {
        Action requestedAction = new Action();
        requestedAction.setType(SendToBridgeAction.TYPE);

        Action resolvedAction = new Action();
        resolvedAction.setType(WebhookAction.TYPE);

        return createProcessor(ProcessorType.SINK, new ProcessorDefinition(null, null, requestedAction, resolvedAction));
    }

    public static ProcessorDTO createProcessor(ProcessorType type, ProcessorDefinition definition) {
        ProcessorDTO dto = new ProcessorDTO();
        dto.setType(type);
        dto.setId("processorId-1");
        dto.setName("processorName-1");
        dto.setDefinition(definition);
        dto.setBridgeId("bridgeId-1");
        dto.setCustomerId("jrota");
        dto.setStatus(ManagedResourceStatus.READY);
        dto.setKafkaConnection(createKafkaConnection());
        return dto;
    }

    public static KafkaConnectionDTO createKafkaConnection() {
        return new KafkaConnectionDTO(
                "fake:9092",
                "test",
                "test",
                "PLAINTEXT",
                "ob-bridgeid-1",
                "ob-bridgeid-1-errors");
    }
}
