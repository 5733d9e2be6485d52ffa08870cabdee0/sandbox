package com.redhat.service.bridge.executor.actions;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

@ApplicationScoped
public class ActionInvokers {

    @Inject
    Instance<ActionInvokerFactory> factories;

    public ActionInvoker build(ProcessorDTO processor) {
        Optional<ActionInvokerFactory> invokerFactory = factories.stream().filter((f) -> f.accepts(processor.getAction())).findFirst();
        if (invokerFactory.isPresent()) {
            return invokerFactory.get().build(processor, processor.getAction());
        }

        throw new ActionInvokerException(String.format("No registered factory to build ActionInvoker for type '%s'", processor.getAction().getType()));
    }
}
