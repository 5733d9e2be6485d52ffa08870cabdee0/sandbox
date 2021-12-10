package com.redhat.service.bridge.executor.filters;

import java.util.Set;

import com.redhat.service.bridge.infra.api.models.filters.BaseFilter;

public interface FilterEvaluatorFactory {
    FilterEvaluator build(Set<BaseFilter> filters);
}
