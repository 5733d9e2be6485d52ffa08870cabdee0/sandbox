package com.redhat.service.bridge.executor;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.kie.dmn.feel.FEEL;

@ApplicationScoped
public class FilterEvaluatorFEEL implements FilterEvaluator {
    private static final FEEL feel = FEEL.newInstance();

    @Override
    public boolean evaluateFilter(String template, Map<String, Object> data) {
        Object result = feel.evaluate(template, data);
        return result.equals(TemplateFactoryFEEL.IS_VALID);
    }
}
