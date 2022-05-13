package com.redhat.service.smartevents.executor.filters;

import java.util.Map;
import java.util.Set;

import org.kie.dmn.feel.FEEL;

public class FilterEvaluatorFEEL implements FilterEvaluator {
    private static final FEEL feel = FEEL.newInstance();

    private final Set<String> templates;

    public FilterEvaluatorFEEL(Set<String> templates) {
        this.templates = templates;
    }

    @Override
    public boolean evaluateFilters(Map<String, Object> data) {
        if (templates != null) {
            for (String template : templates) {
                Object result = feel.evaluate(template, data);
                if (!result.equals(FilterEvaluatorFactoryFEEL.IS_VALID)) {
                    return false;
                }
            }
        }
        return true;
    }
}
