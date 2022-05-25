package com.redhat.service.smartevents.processor.actions.webhook;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayValidator;

@ApplicationScoped
public class WebhookActionValidator implements WebhookAction, GatewayValidator<Action> {

    private WebhookValidator validator = new WebhookValidator();

    @Override
    public ValidationResult isValid(Action action) {
        return validator.isValid(action);
    }

}
