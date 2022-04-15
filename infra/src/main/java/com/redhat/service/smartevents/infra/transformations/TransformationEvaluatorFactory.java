package com.redhat.service.smartevents.infra.transformations;

import com.redhat.service.smartevents.infra.validations.ValidationResult;

public interface TransformationEvaluatorFactory {
    TransformationEvaluator build(String template);

    ValidationResult validate(String template);
}
