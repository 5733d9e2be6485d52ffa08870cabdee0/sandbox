package com.redhat.service.rhose.executor.filters;

import java.util.Map;

public interface FilterEvaluator {
    boolean evaluateFilters(Map<String, Object> data);
}
