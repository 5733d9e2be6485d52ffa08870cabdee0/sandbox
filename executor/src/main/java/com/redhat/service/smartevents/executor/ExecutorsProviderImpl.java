package com.redhat.service.smartevents.executor;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactoryFEEL;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.smartevents.processor.actions.ActionRuntime;

import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class ExecutorsProviderImpl implements ExecutorsProvider {

    private static final FilterEvaluatorFactory filterEvaluatorFactory = new FilterEvaluatorFactoryFEEL();

    @Inject
    ActionRuntime actionRuntime;

    @Inject
    MeterRegistry registry;

    @ConfigProperty(name = "event-bridge.processor.definition")
    String processorDefinition;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TransformationEvaluatorFactory transformationEvaluatorFactory;

    private Executor executor;

    @PostConstruct
    void init() {
        ProcessorDTO dto = readProcessor(processorDefinition);
        this.executor = new Executor(dto, filterEvaluatorFactory, transformationEvaluatorFactory, actionRuntime, registry, objectMapper);
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    private ProcessorDTO readProcessor(String processorDefinition) {
        try {
            return objectMapper.readValue(processorDefinition, ProcessorDTO.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot deserialize processor definition.");
        }
    }
}
