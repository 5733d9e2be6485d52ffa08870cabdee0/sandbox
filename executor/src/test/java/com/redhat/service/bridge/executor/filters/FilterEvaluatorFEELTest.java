package com.redhat.service.bridge.executor.filters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.filters.StringBeginsWith;
import com.redhat.service.bridge.infra.models.filters.StringContains;
import com.redhat.service.bridge.infra.models.filters.StringEquals;

import static org.assertj.core.api.Assertions.assertThat;

public class FilterEvaluatorFEELTest {

    private static final FilterEvaluatorFactoryFEEL TEMPLATE_FACTORY_FEEL = new FilterEvaluatorFactoryFEEL();

    @Test
    public void testStringEqualsFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringEquals("source", "myService")));

        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "myService"))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "notMyService"))).isFalse();
    }

    @Test
    public void testStringBeginsWithFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringBeginsWith("source", "[\"mySer\"]")));

        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "myService"))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "notMyService"))).isFalse();
    }

    @Test
    public void testStringContainsFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringContains("source", "[\"Ser\"]")));

        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "myService"))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "notMyApplication"))).isFalse();
    }

    @Test
    public void testStringContainsListFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringContains("source", "[\"Ser\", \"Tes\"]")));

        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "myService"))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "myTest"))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "notMyApplication"))).isFalse();
    }

    @Test
    public void testFilterWithNestedObjects() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringEquals("data.name", "jacopo")));

        Map<String, Object> data = new HashMap<>();
        data.put("data", Collections.singletonMap("name", "jacopo"));
        assertThat(evaluator.evaluateFilters(data)).isTrue();

        data = new HashMap<>();
        data.put("data", Collections.singletonMap("name", "notJacopo"));
        assertThat(evaluator.evaluateFilters(data)).isFalse();
    }
}
