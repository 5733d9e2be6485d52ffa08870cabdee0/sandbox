package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class KafkaTopicActionValidatorTest {

    @Inject
    KafkaTopicActionValidator validator;

    private ProcessorDTO createProcessorWithActionForTopic(String topicName) {
        Action b = new Action();
        b.setType(KafkaTopicAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, topicName);
        b.setParameters(params);

        ProcessorDTO p = new ProcessorDTO();
        p.setId("myProcessor");
        p.setDefinition(new ProcessorDefinition(null, null, b));
        p.setBridgeId("myBridge");

        return p;
    }

    @Test
    void isValid() {
        ProcessorDTO processor = createProcessorWithActionForTopic("myTopic");
        assertThat(validator.isValid(processor.getDefinition().getResolvedAction()).isValid()).isTrue();
    }

    @Test
    void isValid_noTopicIsNotValid() {
        ProcessorDTO processor = createProcessorWithActionForTopic("myTopic");
        processor.getDefinition().getResolvedAction().getParameters().remove(KafkaTopicAction.TOPIC_PARAM);
        ValidationResult validationResult = validator.isValid(processor.getDefinition().getResolvedAction());

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).isEqualTo(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE);
    }

    @Test
    void isValid_emptyTopicStringIsNotValid() {
        ProcessorDTO processor = createProcessorWithActionForTopic("myTopic");
        processor.getDefinition().getResolvedAction().getParameters().put(KafkaTopicAction.TOPIC_PARAM, "");
        ValidationResult validationResult = validator.isValid(processor.getDefinition().getResolvedAction());

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).isEqualTo(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE);
    }
}
