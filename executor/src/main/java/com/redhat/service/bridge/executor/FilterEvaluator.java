package com.redhat.service.bridge.executor;

import java.util.Map;

public interface FilterEvaluator {
    boolean evaluateFilter(String template, Map<String, Object> data);
}
