package com.redhat.service.bridge.executor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.filters.StringBeginsWith;
import com.redhat.service.bridge.infra.models.filters.StringContains;
import com.redhat.service.bridge.infra.models.filters.StringEquals;

public class FilterEvaluatorFEELTest {

    private static final TemplateFactoryFEEL TEMPLATE_FACTORY_FEEL = new TemplateFactoryFEEL();
    private static final FilterEvaluatorFEEL FILTER_EVALUATOR_FEEL = new FilterEvaluatorFEEL();

    @Test
    public void testStringEqualsFilter() {
        String template = TEMPLATE_FACTORY_FEEL.build(new StringEquals("source", "myService"));

        Assertions.assertTrue(FILTER_EVALUATOR_FEEL.evaluateFilter(template, Collections.singletonMap("source", "myService")));
        Assertions.assertFalse(FILTER_EVALUATOR_FEEL.evaluateFilter(template, Collections.singletonMap("source", "notMyService")));
    }

    @Test
    public void testStringBeginsWithFilter() {
        String template = TEMPLATE_FACTORY_FEEL.build(new StringBeginsWith("source", "[\"mySer\"]"));

        Assertions.assertTrue(FILTER_EVALUATOR_FEEL.evaluateFilter(template, Collections.singletonMap("source", "myService")));
        Assertions.assertFalse(FILTER_EVALUATOR_FEEL.evaluateFilter(template, Collections.singletonMap("source", "notMyService")));
    }

    @Test
    public void testStringContainsFilter() {
        String template = TEMPLATE_FACTORY_FEEL.build(new StringContains("source", "[\"Ser\"]"));

        Assertions.assertTrue(FILTER_EVALUATOR_FEEL.evaluateFilter(template, Collections.singletonMap("source", "myService")));
        Assertions.assertFalse(FILTER_EVALUATOR_FEEL.evaluateFilter(template, Collections.singletonMap("source", "notMyApplication")));
    }

    @Test
    public void testStringContainsListFilter() {
        String template = TEMPLATE_FACTORY_FEEL.build(new StringContains("source", "[\"Ser\", \"Tes\"]"));

        Assertions.assertTrue(FILTER_EVALUATOR_FEEL.evaluateFilter(template, Collections.singletonMap("source", "myService")));
        Assertions.assertTrue(FILTER_EVALUATOR_FEEL.evaluateFilter(template, Collections.singletonMap("source", "myTest")));
        Assertions.assertFalse(FILTER_EVALUATOR_FEEL.evaluateFilter(template, Collections.singletonMap("source", "notMyApplication")));
    }

    @Test
    public void testFilterWithNestedObjects() {
        String template = TEMPLATE_FACTORY_FEEL.build(new StringEquals("data.name", "jacopo"));

        Map<String, Object> data = new HashMap<>();
        data.put("data", Collections.singletonMap("name", "jacopo"));
        Assertions.assertTrue(FILTER_EVALUATOR_FEEL.evaluateFilter(template, data));

        data = new HashMap<>();
        data.put("data", Collections.singletonMap("name", "notJacopo"));
        Assertions.assertFalse(FILTER_EVALUATOR_FEEL.evaluateFilter(template, data));
    }
}
