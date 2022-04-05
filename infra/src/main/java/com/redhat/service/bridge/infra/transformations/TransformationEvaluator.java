package com.redhat.service.bridge.infra.transformations;

import java.util.Map;

public interface TransformationEvaluator {
    String render(Map<String, Object> data);
}
