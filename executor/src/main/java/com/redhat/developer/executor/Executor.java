package com.redhat.developer.executor;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.infra.dto.ProcessorDTO;

import io.cloudevents.CloudEvent;

public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private final ProcessorDTO processor;

    public Executor(ProcessorDTO processor) {
        this.processor = processor;
    }

    public void onEvent(CloudEvent cloudEvent) {
        LOG.info("Received event with id '{}' for Processor with name '{}' on Bridge '{}", cloudEvent.getId(), processor.getName(), processor.getBridge().getId());
    }

    public ProcessorDTO getProcessor() {
        return processor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Executor executor = (Executor) o;
        return Objects.equals(processor, executor.processor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processor);
    }
}
