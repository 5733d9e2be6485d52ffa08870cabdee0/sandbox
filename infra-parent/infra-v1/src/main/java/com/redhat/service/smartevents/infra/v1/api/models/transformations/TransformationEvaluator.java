package com.redhat.service.smartevents.infra.v1.api.models.transformations;

import java.util.Map;

public interface TransformationEvaluator {
    String render(Map<String, Object> data);
}
