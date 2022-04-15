package com.redhat.service.smartevents.executor.filters;

import java.util.Map;

public interface FilterEvaluator {
    boolean evaluateFilters(Map<String, Object> data);
}
