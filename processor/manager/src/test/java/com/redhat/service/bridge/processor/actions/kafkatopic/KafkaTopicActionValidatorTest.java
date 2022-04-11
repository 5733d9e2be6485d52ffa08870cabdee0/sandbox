package com.redhat.service.bridge.processor.actions.kafkatopic;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class KafkaTopicActionValidatorTest {

    @Inject
    KafkaTopicActionValidator validator;

    private ProcessorDTO createProcessorWithActionForTopic(String topicName) {
        BaseAction b = new BaseAction();
        b.setType(KafkaTopicActionBean.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicActionBean.TOPIC_PARAM, topicName);
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
        Assertions.assertThat(validator.isValid(processor.getDefinition().getResolvedAction()).isValid()).isTrue();
    }

    @Test
    void isValid_noTopicIsNotValid() {
        ProcessorDTO processor = createProcessorWithActionForTopic("myTopic");
        processor.getDefinition().getResolvedAction().getParameters().remove(KafkaTopicActionBean.TOPIC_PARAM);
        ValidationResult validationResult = validator.isValid(processor.getDefinition().getResolvedAction());

        Assertions.assertThat(validationResult.isValid()).isFalse();
        Assertions.assertThat(validationResult.getMessage()).isEqualTo(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE);
    }

    @Test
    void isValid_emptyTopicStringIsNotValid() {
        ProcessorDTO processor = createProcessorWithActionForTopic("myTopic");
        processor.getDefinition().getResolvedAction().getParameters().put(KafkaTopicActionBean.TOPIC_PARAM, "");
        ValidationResult validationResult = validator.isValid(processor.getDefinition().getResolvedAction());

        Assertions.assertThat(validationResult.isValid()).isFalse();
        Assertions.assertThat(validationResult.getMessage()).isEqualTo(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE);
    }
}
