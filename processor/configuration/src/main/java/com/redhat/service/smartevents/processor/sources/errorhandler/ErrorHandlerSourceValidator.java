package com.redhat.service.smartevents.processor.sources.errorhandler;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayValidator;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookValidator;

@ApplicationScoped
public class ErrorHandlerSourceValidator implements ErrorHandlerSource, GatewayValidator<Source> {

    private WebhookValidator validator = new WebhookValidator();

    @Override
    public ValidationResult isValid(Source source) {
        return validator.isValid(source);
    }

}
