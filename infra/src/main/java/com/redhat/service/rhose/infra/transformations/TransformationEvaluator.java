package com.redhat.service.rhose.infra.transformations;

import java.util.Map;

public interface TransformationEvaluator {
    String render(Map<String, Object> data);
}
