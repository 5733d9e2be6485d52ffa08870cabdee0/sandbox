package com.redhat.service.bridge.executor.transformations;

import java.util.Map;

public interface TransformationEvaluator {
    String render(Map<String, Object> data);
}
