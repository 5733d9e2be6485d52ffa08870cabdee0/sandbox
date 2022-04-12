package com.redhat.service.rhose.executor.filters;

import java.util.Set;

import com.redhat.service.rhose.infra.models.filters.BaseFilter;

public interface FilterEvaluatorFactory {
    FilterEvaluator build(Set<BaseFilter> filters);
}
