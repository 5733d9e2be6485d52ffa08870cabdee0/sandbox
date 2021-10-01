package com.redhat.service.bridge.executor.filters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.filters.StringBeginsWith;
import com.redhat.service.bridge.infra.models.filters.StringContains;
import com.redhat.service.bridge.infra.models.filters.StringEquals;

public class FilterEvaluatorFEELTest {

    private static final FilterEvaluatorFactoryFEEL TEMPLATE_FACTORY_FEEL = new FilterEvaluatorFactoryFEEL();

    @Test
    public void testStringEqualsFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringEquals("source", "myService")));

        Assertions.assertTrue(evaluator.evaluateFilters(Collections.singletonMap("source", "myService")));
        Assertions.assertFalse(evaluator.evaluateFilters(Collections.singletonMap("source", "notMyService")));
    }

    @Test
    public void testStringBeginsWithFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringBeginsWith("source", "[\"mySer\"]")));

        Assertions.assertTrue(evaluator.evaluateFilters(Collections.singletonMap("source", "myService")));
        Assertions.assertFalse(evaluator.evaluateFilters(Collections.singletonMap("source", "notMyService")));
    }

    @Test
    public void testStringContainsFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringContains("source", "[\"Ser\"]")));

        Assertions.assertTrue(evaluator.evaluateFilters(Collections.singletonMap("source", "myService")));
        Assertions.assertFalse(evaluator.evaluateFilters(Collections.singletonMap("source", "notMyApplication")));
    }

    @Test
    public void testStringContainsListFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringContains("source", "[\"Ser\", \"Tes\"]")));

        Assertions.assertTrue(evaluator.evaluateFilters(Collections.singletonMap("source", "myService")));
        Assertions.assertTrue(evaluator.evaluateFilters(Collections.singletonMap("source", "myTest")));
        Assertions.assertFalse(evaluator.evaluateFilters(Collections.singletonMap("source", "notMyApplication")));
    }

    @Test
    public void testFilterWithNestedObjects() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringEquals("data.name", "jacopo")));

        Map<String, Object> data = new HashMap<>();
        data.put("data", Collections.singletonMap("name", "jacopo"));
        Assertions.assertTrue(evaluator.evaluateFilters(data));

        data = new HashMap<>();
        data.put("data", Collections.singletonMap("name", "notJacopo"));
        Assertions.assertFalse(evaluator.evaluateFilters(data));
    }
}
