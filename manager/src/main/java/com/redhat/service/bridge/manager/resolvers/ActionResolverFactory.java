package com.redhat.service.bridge.manager.resolvers;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.processor.actions.AbstractActionBeanFactory;

@ApplicationScoped
public class ActionResolverFactory extends AbstractActionBeanFactory<ActionResolver> {

    public ActionResolverFactory() {
        super(ActionResolver.class);
    }
}
