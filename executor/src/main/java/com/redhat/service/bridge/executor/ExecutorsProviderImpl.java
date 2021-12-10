package com.redhat.service.bridge.executor;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.actions.ActionProviderFactory;
import com.redhat.service.bridge.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.bridge.executor.filters.FilterEvaluatorFactoryFEEL;
import com.redhat.service.bridge.executor.transformations.TransformationEvaluatorFactory;
import com.redhat.service.bridge.executor.transformations.TransformationEvaluatorFactoryQute;
import com.redhat.service.bridge.infra.api.models.dto.ProcessorDTO;

import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class ExecutorsProviderImpl implements ExecutorsProvider {

    private static final FilterEvaluatorFactory filterEvaluatorFactory = new FilterEvaluatorFactoryFEEL();
    private static final TransformationEvaluatorFactory transformationEvaluatorFactory = new TransformationEvaluatorFactoryQute();

    @Inject
    ActionProviderFactory actionProviderFactory;

    @Inject
    MeterRegistry registry;

    @ConfigProperty(name = "event-bridge.processor.definition")
    String processorDefinition;

    @Inject
    ObjectMapper objectMapper;

    private Executor executor;

    @PostConstruct
    void init() {
        ProcessorDTO dto = readProcessor(processorDefinition);
        this.executor = new Executor(dto, filterEvaluatorFactory, transformationEvaluatorFactory, actionProviderFactory, registry);
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
