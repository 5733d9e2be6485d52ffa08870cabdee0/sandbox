package com.redhat.service.bridge.executor;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;

public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private final ProcessorDTO processor;

    private final FilterEvaluator filterEvaluator;

    public Executor(ProcessorDTO processor, FilterEvaluator filterEvaluator) {
        this.processor = processor;
        this.filterEvaluator = filterEvaluator;
    }

    @SuppressWarnings("unchecked")
    public void onEvent(CloudEvent cloudEvent) {
        LOG.info("[executor] Received event with id '{}' for Processor with name '{}' on Bridge '{}", cloudEvent.getId(), processor.getName(), processor.getBridge().getId());

        if (filterEvaluator.evaluateFilters(CloudEventUtils.getMapper().convertValue(cloudEvent, Map.class))) {
            LOG.info("[executor] Filters of processor '{}' matched for event with id '{}'", processor.getId(), cloudEvent.getId());
            // TODO - CALL ACTIONS;
            // TODO - consider if the CloudEvent needs cleaning up from our extensions before it is handled by Actions
        } else {
            LOG.debug("[executor] Filters of processor '{}' did not match for event with id '{}'", processor.getId(), cloudEvent.getId());
            // DO NOTHING;
        }
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
