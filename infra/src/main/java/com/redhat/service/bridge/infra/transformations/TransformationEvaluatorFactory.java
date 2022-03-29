package com.redhat.service.bridge.infra.transformations;

import com.redhat.service.bridge.infra.validations.ValidationResult;

public interface TransformationEvaluatorFactory {
    TransformationEvaluator build(String template);

    ValidationResult validate(String template);
}
