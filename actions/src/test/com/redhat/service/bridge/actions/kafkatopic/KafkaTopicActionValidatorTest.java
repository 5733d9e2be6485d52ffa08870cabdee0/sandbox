package com.redhat.service.bridge.actions.kafkatopic;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.redhat.service.bridge.actions.ValidationResult;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        p.setAction(b);

        BridgeDTO bridge = new BridgeDTO();
        bridge.setId("myBridge");
        p.setBridge(bridge);

        return p;
    }

    @Test
    public void isValid() {
        ProcessorDTO processor = createProcessorWithActionForTopic("myTopic");
        Assertions.assertTrue(validator.isValid(processor.getAction()).isValid());
    }

    @Test
    public void isValid_noTopicIsNotValid() {
        ProcessorDTO processor = createProcessorWithActionForTopic("myTopic");
        processor.getAction().getParameters().remove(KafkaTopicAction.TOPIC_PARAM);
        ValidationResult validationResult = validator.isValid(processor.getAction());

        Assertions.assertFalse(validationResult.isValid());
        Assertions.assertEquals(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE, validationResult.getMessage());
    }

    @Test
    public void isValid_emptyTopicStringIsNotValid() {
        ProcessorDTO processor = createProcessorWithActionForTopic("myTopic");
        processor.getAction().getParameters().put(KafkaTopicAction.TOPIC_PARAM, "");
        ValidationResult validationResult = validator.isValid(processor.getAction());

        Assertions.assertFalse(validationResult.isValid());
        Assertions.assertEquals(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE, validationResult.getMessage());
    }
}
