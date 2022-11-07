package com.redhat.service.smartevents.executor.filters;

import java.util.Set;

import com.redhat.service.smartevents.infra.v1.api.models.filters.BaseFilter;

public interface FilterEvaluatorFactory {
    FilterEvaluator build(Set<BaseFilter> filters);
}
