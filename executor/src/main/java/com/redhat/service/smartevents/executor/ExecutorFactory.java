package com.redhat.service.smartevents.executor;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactoryFEEL;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.smartevents.processor.actions.ActionRuntime;

import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class ExecutorFactory {

    private static final FilterEvaluatorFactory filterEvaluatorFactory = new FilterEvaluatorFactoryFEEL();

    @ConfigProperty(name = "event-bridge.processor.definition")
    String processorDefinition;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TransformationEvaluatorFactory transformationEvaluatorFactory;

    @Inject
    ActionRuntime actionRuntime;

    @Inject
    MeterRegistry meterRegistry;

    private ProcessorDTO processorDTO;

    @PostConstruct
    void init() {
        try {
            processorDTO = objectMapper.readValue(processorDefinition, ProcessorDTO.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot deserialize processor definition.");
        }
    }

    @Produces
    @ApplicationScoped
    public Executor buildExecutor() {
        return new ExecutorImpl(processorDTO,
                filterEvaluatorFactory,
                transformationEvaluatorFactory,
                actionRuntime,
                meterRegistry);
    }
}
