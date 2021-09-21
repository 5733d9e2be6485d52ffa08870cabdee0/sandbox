package com.redhat.service.bridge.executor;

import java.util.Map;

public interface FilterEvaluator {
    boolean evaluateFilters(Map<String, Object> data);
}
