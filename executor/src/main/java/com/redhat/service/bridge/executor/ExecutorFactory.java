package com.redhat.service.bridge.executor;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

@ApplicationScoped
public class ExecutorFactory {

    public Executor createExecutor(ProcessorDTO processor) {
        return new Executor(processor);
    }
}
