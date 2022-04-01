package com.redhat.service.bridge.processor.actions.common;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ActionInvokerBuilderFactory extends AbstractActionAccepterFactory<ActionInvokerBuilder> {

    public ActionInvokerBuilderFactory() {
        super(ActionInvokerBuilder.class);
    }
}
