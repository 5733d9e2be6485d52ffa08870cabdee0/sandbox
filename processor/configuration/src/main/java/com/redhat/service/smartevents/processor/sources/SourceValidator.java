package com.redhat.service.smartevents.processor.sources;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

public interface SourceValidator extends SourceBean {
    ValidationResult isValid(Source source);
}
