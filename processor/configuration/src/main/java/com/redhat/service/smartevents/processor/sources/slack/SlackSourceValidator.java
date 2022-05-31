package com.redhat.service.smartevents.processor.sources.slack;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.JsonSchemaService;

@ApplicationScoped
public class SlackSourceValidator extends AbstractGatewayValidator<Source> implements SlackSource {
    public static final String INVALID_CHANNEL_MESSAGE =
            "The supplied " + CHANNEL_PARAM + " parameter is not valid";

    public static final String INVALID_TOKEN_MESSAGE =
            "The supplied " + TOKEN_PARAM + " parameter is not valid";

    @Inject
    public SlackSourceValidator(JsonSchemaService jsonSchemaService) {
        super(jsonSchemaService);
    }
}
