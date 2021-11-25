package com.redhat.service.bridge.actions.kafkatopic;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.ValidationResult;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class KafkaTopicActionValidatorTest {

    @Inject
    KafkaTopicActionValidator validator;

    private ProcessorDTO createProcessorWithActionForTopic(String topicName) {
        BaseAction b = new BaseAction();
        b.setType(KafkaTopicAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, topicName);
        b.setParameters(params);

        ProcessorDTO p = new ProcessorDTO();
        p.setId("myProcessor");
        p.setDefinition(new ProcessorDefinition(null, null, b));

        BridgeDTO bridge = new BridgeDTO();
        bridge.setId("myBridge");
        p.setBridge(bridge);

        return p;
    }

    @Test
    public void isValid() {
        ProcessorDTO processor = createProcessorWithActionForTopic("myTopic");
        assertThat(validator.isValid(processor.getDefinition().getResolvedAction()).isValid()).isTrue();
    }

    @Test
    public void isValid_noTopicIsNotValid() {
        ProcessorDTO processor = createProcessorWithActionForTopic("myTopic");
        processor.getDefinition().getResolvedAction().getParameters().remove(KafkaTopicAction.TOPIC_PARAM);
        ValidationResult validationResult = validator.isValid(processor.getDefinition().getResolvedAction());

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).isEqualTo(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE);
    }

    @Test
    public void isValid_emptyTopicStringIsNotValid() {
        ProcessorDTO processor = createProcessorWithActionForTopic("myTopic");
        processor.getDefinition().getResolvedAction().getParameters().put(KafkaTopicAction.TOPIC_PARAM, "");
        ValidationResult validationResult = validator.isValid(processor.getDefinition().getResolvedAction());

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).isEqualTo(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE);
    }
}
