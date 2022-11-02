package com.redhat.service.smartevents.infra.core.transformations;

import java.util.Map;

public interface TransformationEvaluator {
    String render(Map<String, Object> data);
}
