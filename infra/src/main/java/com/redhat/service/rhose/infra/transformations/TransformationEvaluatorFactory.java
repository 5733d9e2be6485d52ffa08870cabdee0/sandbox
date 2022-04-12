package com.redhat.service.rhose.infra.transformations;

import com.redhat.service.rhose.infra.validations.ValidationResult;

public interface TransformationEvaluatorFactory {
    TransformationEvaluator build(String template);

    ValidationResult validate(String template);
}
