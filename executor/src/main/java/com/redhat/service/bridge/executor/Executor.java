package com.redhat.service.bridge.executor;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;

public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private final ProcessorDTO processor;

    private final Set<String> templates;

    private final FilterEvaluator filterEvaluator;

    private final TemplateFactory templateFactory;

    public Executor(ProcessorDTO processor, TemplateFactory templateFactory, FilterEvaluator filterEvaluator) {
        this.processor = processor;
        this.templates = buildTemplates(processor);
        this.filterEvaluator = filterEvaluator;
        this.templateFactory = templateFactory;
    }

    public void onEvent(CloudEvent cloudEvent) {
        LOG.info("[executor] Received event with id '{}' for Processor with name '{}' on Bridge '{}", cloudEvent.getId(), processor.getName(), processor.getBridge().getId());

        if (matchFilters(cloudEvent)) {
            LOG.info("[executor] Filters of processor '{}' matched for event with id '{}'", processor.getId(), cloudEvent.getId());
            // TODO - CALL ACTIONS;
            // TODO - consider if the CloudEvent needs cleaning up from our extensions before it is handled by Actions
            return;
        }
        LOG.debug("[executor] Filters of processor '{}' did not match for event with id '{}'", processor.getId(), cloudEvent.getId());
        // DO NOTHING;
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

    @SuppressWarnings("unchecked")
    private boolean matchFilters(CloudEvent cloudEvent) {
        if (templates == null) {
            return true;
        }

        for (String template : templates) {
            if (!filterEvaluator.evaluateFilter(template, CloudEventUtils.getMapper().convertValue(cloudEvent, Map.class))) {
                return false;
            }
        }
        return true;
    }

    private Set<String> buildTemplates(ProcessorDTO processorDTO) {
        Set<BaseFilter> filters = processorDTO.getFilters();
        return filters == null ? null : filters.stream().map(templateFactory::build).collect(Collectors.toSet());
    }
}
