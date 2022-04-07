package com.redhat.service.bridge.processor.actions;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ActionInvokerBuilderFactory extends AbstractActionBeanFactory<ActionInvokerBuilder> {

    public ActionInvokerBuilderFactory() {
        super(ActionInvokerBuilder.class);
    }
}
