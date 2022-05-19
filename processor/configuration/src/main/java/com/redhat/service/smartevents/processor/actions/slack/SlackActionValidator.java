package com.redhat.service.smartevents.processor.actions.slack;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.processor.actions.generic.GenericJsonSchemaConnectorValidator;

@ApplicationScoped
public class SlackActionValidator extends GenericJsonSchemaConnectorValidator {

    @Override
    public String getType() {
        return "slack_sink_0.1";
    }
}
