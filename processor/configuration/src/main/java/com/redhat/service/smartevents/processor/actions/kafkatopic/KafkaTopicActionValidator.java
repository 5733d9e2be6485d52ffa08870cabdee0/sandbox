package com.redhat.service.smartevents.processor.actions.kafkatopic;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.JsonSchemaService;

@ApplicationScoped
public class KafkaTopicActionValidator extends AbstractGatewayValidator<Action> implements KafkaTopicAction {

    @Inject
    public KafkaTopicActionValidator(JsonSchemaService jsonSchemaService) {
        super(jsonSchemaService);
    }
}
