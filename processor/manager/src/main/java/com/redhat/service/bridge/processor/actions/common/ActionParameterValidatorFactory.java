package com.redhat.service.bridge.processor.actions.common;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ActionParameterValidatorFactory extends AbstractActionAccepterFactory<ActionParameterValidator> {

    public ActionParameterValidatorFactory() {
        super(ActionParameterValidator.class);
    }
}
