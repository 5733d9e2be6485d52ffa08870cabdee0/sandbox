package com.redhat.service.smartevents.executor.filters;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.filters.NumberIn;
import com.redhat.service.smartevents.infra.models.filters.StringBeginsWith;
import com.redhat.service.smartevents.infra.models.filters.StringContains;
import com.redhat.service.smartevents.infra.models.filters.StringEquals;
import com.redhat.service.smartevents.infra.models.filters.StringIn;

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
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringBeginsWith("source", Arrays.asList("mySer"))));

        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "myService"))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "notMyService"))).isFalse();
    }

    @Test
    public void testStringContainsFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringContains("source", Arrays.asList("Ser"))));

        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "myService"))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "notMyApplication"))).isFalse();
    }

    @Test
    public void testStringContainsListFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringContains("source", Arrays.asList("Ser", "Tes"))));

        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "myService"))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "myTest"))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "notMyApplication"))).isFalse();
    }

    @Test
    public void testInFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new NumberIn("source", Arrays.asList(2.2, 3d, 4d))));

        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", 2.2))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", 3d))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", 4d))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", 2d))).isFalse();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", 2.22d))).isFalse();
    }

    @Test
    public void testStringInFilter() {
        FilterEvaluator evaluator = TEMPLATE_FACTORY_FEEL.build(Collections.singleton(new StringIn("source", Arrays.asList("hello", "kekkobar"))));

        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "kekkobar"))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "jack"))).isTrue();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "wrong"))).isFalse();
        assertThat(evaluator.evaluateFilters(Collections.singletonMap("source", "kermit"))).isFalse();
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
