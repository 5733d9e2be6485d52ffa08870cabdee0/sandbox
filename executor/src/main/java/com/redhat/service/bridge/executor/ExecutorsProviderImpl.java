package com.redhat.service.bridge.executor;

import java.io.File;
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
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

import io.micrometer.core.instrument.MeterRegistry;

@ApplicationScoped
public class ExecutorsProviderImpl implements ExecutorsProvider {

    private static final FilterEvaluatorFactory filterEvaluatorFactory = new FilterEvaluatorFactoryFEEL();
    private static final TransformationEvaluatorFactory transformationEvaluatorFactory = new TransformationEvaluatorFactoryQute();

    @Inject
    ActionProviderFactory actionProviderFactory;

    @Inject
    MeterRegistry registry;

    @ConfigProperty(name = "event-bridge.processor.configuration.file.path")
    String processorPath;

    private Executor executor;

    @PostConstruct
    void init() {
        ProcessorDTO dto = readProcessor(processorPath);
        this.executor = new Executor(dto, filterEvaluatorFactory, transformationEvaluatorFactory, actionProviderFactory, registry);
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    private ProcessorDTO readProcessor(String path) {
        try {
            return new ObjectMapper().readValue(new File(path), ProcessorDTO.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot find processor on classpath: " + path);
        }
    }
}
