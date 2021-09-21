package com.redhat.service.bridge.executor;

import java.util.Set;

import com.redhat.service.bridge.infra.models.filters.BaseFilter;

public interface FilterEvaluatorFactory {
    FilterEvaluator build(Set<BaseFilter> filters);
}
