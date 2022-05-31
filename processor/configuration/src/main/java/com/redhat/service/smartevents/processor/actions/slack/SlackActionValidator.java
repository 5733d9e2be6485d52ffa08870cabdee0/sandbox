package com.redhat.service.smartevents.processor.actions.slack;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.JsonSchemaService;

@ApplicationScoped
public class SlackActionValidator extends AbstractGatewayValidator<Action> implements SlackAction {
    public static final String INVALID_CHANNEL_MESSAGE =
            "The supplied " + CHANNEL_PARAM + " parameter is not valid";

    public static final String INVALID_WEBHOOK_URL_MESSAGE =
            "The supplied " + WEBHOOK_URL_PARAM + " parameter is not valid";

    @Inject
    public SlackActionValidator(JsonSchemaService jsonSchemaService) {
        super(jsonSchemaService);
    }
}
