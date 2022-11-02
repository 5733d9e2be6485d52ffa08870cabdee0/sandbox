package com.redhat.service.smartevents.infra.core.transformations;

import com.redhat.service.smartevents.infra.core.validations.ValidationResult;

public interface TransformationEvaluatorFactory {
    TransformationEvaluator build(String template);

    ValidationResult validate(String template);
}
