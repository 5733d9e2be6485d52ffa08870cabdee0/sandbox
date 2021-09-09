package com.redhat.developer.executor;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.developer.infra.dto.ProcessorDTO;

@ApplicationScoped
public class ExecutorFactory {

    public Executor createExecutor(ProcessorDTO processor) {
        return new Executor(processor);
    }
}
