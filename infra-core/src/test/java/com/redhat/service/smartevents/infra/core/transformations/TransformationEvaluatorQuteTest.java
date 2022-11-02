package com.redhat.service.smartevents.infra.core.transformations;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TransformationEvaluatorQuteTest {
    private static final TransformationEvaluatorFactoryQute FACTORY = new TransformationEvaluatorFactoryQute();

    @Test
    public void testNullTemplate() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        TransformationEvaluator evaluator = FACTORY.build(null);

        String rendered = evaluator.render(data);
        assertThat(rendered).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    public void testTemplate() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        String template = "Hi {key} how are you?";
        TransformationEvaluator evaluator = FACTORY.build(template);

        String rendered = evaluator.render(data);
        assertThat(rendered).isEqualTo("Hi value how are you?");
    }
}
