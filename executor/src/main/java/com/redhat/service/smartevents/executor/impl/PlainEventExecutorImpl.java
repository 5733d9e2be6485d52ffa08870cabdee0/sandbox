package com.redhat.service.smartevents.executor.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.executor.Executor;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

public class PlainEventExecutorImpl implements Executor {

    private static final Logger LOG = LoggerFactory.getLogger(PlainEventExecutorImpl.class);

    // TODO: add filter, transformation and metrics
    private final ProcessorDTO processor;
    private final ActionInvoker actionInvoker;

    public PlainEventExecutorImpl(ProcessorDTO processor, ActionInvoker actionInvoker) {
        this.processor = processor;
        this.actionInvoker = actionInvoker;
    }

    @Override
    public void onEvent(String event) {
        LOG.info("PlainEventExecutorImpl::onEvent | {}", event);
        actionInvoker.onEvent(event);
    }

    @Override
    public ProcessorDTO getProcessor() {
        return processor;
    }
}
