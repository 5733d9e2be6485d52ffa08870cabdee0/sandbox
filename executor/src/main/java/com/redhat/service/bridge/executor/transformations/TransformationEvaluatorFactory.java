package com.redhat.service.bridge.executor.transformations;

public interface TransformationEvaluatorFactory {
    TransformationEvaluator build(String template);
}
