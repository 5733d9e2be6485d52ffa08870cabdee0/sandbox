package com.redhat.service.smartevents.infra.v1.api.models.transformations;

import com.redhat.service.smartevents.infra.core.validations.ValidationResult;

public interface TransformationEvaluatorFactory {
    TransformationEvaluator build(String template);

    ValidationResult validate(String template);
}
