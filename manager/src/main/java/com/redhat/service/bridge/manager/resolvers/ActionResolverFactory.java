package com.redhat.service.bridge.manager.resolvers;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.processor.actions.common.AbstractActionAccepterFactory;

@ApplicationScoped
public class ActionResolverFactory extends AbstractActionAccepterFactory<ActionResolver> {

    public ActionResolverFactory() {
        super(ActionResolver.class);
    }
}
