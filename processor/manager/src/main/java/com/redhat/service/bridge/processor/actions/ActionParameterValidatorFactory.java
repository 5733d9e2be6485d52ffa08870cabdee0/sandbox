package com.redhat.service.bridge.processor.actions;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ActionParameterValidatorFactory extends AbstractActionBeanFactory<ActionParameterValidator> {

    public ActionParameterValidatorFactory() {
        super(ActionParameterValidator.class);
    }
}
