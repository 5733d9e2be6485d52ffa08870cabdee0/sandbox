package com.redhat.service.bridge.executor;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.actions.ActionInvoker;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.actions.ActionProviderFactory;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;

public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private static final Engine engine = Engine.builder().addDefaults().build();

    private final ProcessorDTO processor;

    private final FilterEvaluator filterEvaluator;

    private final ActionInvoker actionInvoker;

    public Executor(ProcessorDTO processor, FilterEvaluatorFactory factory, ActionProviderFactory actionProviderFactory) {
        this.processor = processor;
        this.filterEvaluator = factory.build(processor.getFilters());

        ActionProvider actionProvider = actionProviderFactory.getActionProvider(processor.getAction().getType());
        this.actionInvoker = actionProvider.getActionInvoker(processor, processor.getAction());
    }

    @SuppressWarnings("unchecked")
    public void onEvent(CloudEvent cloudEvent) {
        LOG.info("[executor] Received event with id '{}' for Processor with name '{}' on Bridge '{}", cloudEvent.getId(), processor.getName(), processor.getBridge().getId());

        Map<String, Object> cloudEventData = CloudEventUtils.getMapper().convertValue(cloudEvent, Map.class);

        if (filterEvaluator.evaluateFilters(cloudEventData)) {
            LOG.info("[executor] Filters of processor '{}' matched for event with id '{}'", processor.getId(), cloudEvent.getId());

            String eventToSend;

            if (processor.getTransformationTemplate() != null) {
                Template template = engine.parse(processor.getTransformationTemplate());
                eventToSend = template.data(cloudEventData).render();
                LOG.info("[executor] Template of processor '{}' successfully applied", processor.getId());
            } else {
                eventToSend = CloudEventUtils.encode(cloudEvent);
            }

            // TODO - https://issues.redhat.com/browse/MGDOBR-49: consider if the CloudEvent needs cleaning up from our extensions before it is handled by Actions
            actionInvoker.onEvent(eventToSend);
        } else {
            LOG.info("[executor] Filters of processor '{}' did not match for event with id '{}'", processor.getId(), cloudEvent.getId());
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
