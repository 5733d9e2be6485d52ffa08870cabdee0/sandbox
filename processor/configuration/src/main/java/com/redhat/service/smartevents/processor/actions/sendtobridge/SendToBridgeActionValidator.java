package com.redhat.service.smartevents.processor.actions.sendtobridge;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.JsonSchemaService;

@ApplicationScoped
public class SendToBridgeActionValidator extends AbstractGatewayValidator<Action> implements SendToBridgeAction {

    public static final String INVALID_BRIDGE_ID_PARAM_MESSAGE =
            "The supplied " + BRIDGE_ID_PARAM + " parameter is not valid";

    @Inject
    public SendToBridgeActionValidator(JsonSchemaService jsonSchemaService) {
        super(jsonSchemaService);
    }
}
