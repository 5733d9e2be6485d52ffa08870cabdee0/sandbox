package com.redhat.service.bridge.executor;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.actions.ActionProviderFactory;
import com.redhat.service.bridge.executor.filters.FilterEvaluator;
import com.redhat.service.bridge.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.bridge.executor.transformations.TransformationEvaluator;
import com.redhat.service.bridge.executor.transformations.TransformationEvaluatorFactory;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;

public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private final ProcessorDTO processor;

    private final FilterEvaluator filterEvaluator;

    private final TransformationEvaluator transformationEvaluator;

    private final ActionInvoker actionInvoker;

    public Executor(ProcessorDTO processor, FilterEvaluatorFactory filterEvaluatorFactory, TransformationEvaluatorFactory transformationFactory, ActionProviderFactory actionProviderFactory) {
        this.processor = processor;
        this.filterEvaluator = filterEvaluatorFactory.build(processor.getFilters());

        this.transformationEvaluator = transformationFactory.build(processor.getTransformationTemplate());

        ActionProvider actionProvider = actionProviderFactory.getActionProvider(processor.getAction().getType());
        this.actionInvoker = actionProvider.getActionInvoker(processor, processor.getAction());
    }

    @SuppressWarnings("unchecked")
    public void onEvent(CloudEvent cloudEvent) {
        LOG.info("[executor] Received event with id '{}' for Processor with name '{}' on Bridge '{}", cloudEvent.getId(), processor.getName(), processor.getBridge().getId());

        Map<String, Object> cloudEventData = CloudEventUtils.getMapper().convertValue(cloudEvent, Map.class);

        if (filterEvaluator.evaluateFilters(cloudEventData)) {
            LOG.info("[executor] Filters of processor '{}' matched for event with id '{}'", processor.getId(), cloudEvent.getId());

            // TODO - https://issues.redhat.com/browse/MGDOBR-49: consider if the CloudEvent needs cleaning up from our extensions before it is handled by Actions
            String eventToSend = transformationEvaluator.render(cloudEventData);

            actionInvoker.onEvent(eventToSend);
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
