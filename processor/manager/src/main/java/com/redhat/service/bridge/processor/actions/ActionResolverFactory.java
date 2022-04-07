package com.redhat.service.bridge.processor.actions;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ActionResolverFactory extends AbstractActionBeanFactory<ActionResolver> {

    public ActionResolverFactory() {
        super(ActionResolver.class);
    }
}
