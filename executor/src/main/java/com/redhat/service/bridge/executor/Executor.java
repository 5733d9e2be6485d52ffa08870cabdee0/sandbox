package com.redhat.service.bridge.executor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.executor.filters.FilterEvaluator;
import com.redhat.service.bridge.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.transformations.TransformationEvaluator;
import com.redhat.service.bridge.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.processor.actions.ActionInvoker;
import com.redhat.service.bridge.processor.actions.ActionInvokerBuilderFactory;

import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

public class Executor {

    private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

    private final ProcessorDTO processor;
    private final FilterEvaluator filterEvaluator;
    private final TransformationEvaluator transformationEvaluator;
    private final ActionInvoker actionInvoker;
    private Timer processorProcessingTime;
    private Timer filterTimer;
    private Timer actionTimer;
    private Timer transformationTimer;

    public Executor(ProcessorDTO processor, FilterEvaluatorFactory filterEvaluatorFactory, TransformationEvaluatorFactory transformationFactory,
            ActionInvokerBuilderFactory actionInvokerBuilderFactory,
            MeterRegistry registry) {
        this.processor = processor;
        this.filterEvaluator = filterEvaluatorFactory.build(processor.getDefinition().getFilters());

        this.transformationEvaluator = transformationFactory.build(processor.getDefinition().getTransformationTemplate());

        BaseAction action = processor.getDefinition().getResolvedAction();
        this.actionInvoker = actionInvokerBuilderFactory.get(action.getType()).build(processor, action);

        initMetricFields(processor, registry);
    }

    public void onEvent(CloudEvent cloudEvent) {
        processorProcessingTime.record(() -> process(cloudEvent));
    }

    @SuppressWarnings("unchecked")
    private void process(CloudEvent cloudEvent) {
        LOG.info("Received event with id '{}' for Processor with name '{}' on Bridge '{}", cloudEvent.getId(), processor.getName(), processor.getBridgeId());

        Map<String, Object> cloudEventData = CloudEventUtils.getMapper().convertValue(cloudEvent, Map.class);

        // Filter evaluation
        if (Boolean.TRUE.equals(filterTimer.record(() -> filterEvaluator.evaluateFilters(cloudEventData)))) {
            LOG.info("Filters of processor '{}' matched for event with id '{}'", processor.getId(), cloudEvent.getId());

            // Transformation
            String eventToSend = transformationTimer.record(() -> transformationEvaluator.render(cloudEventData));

            // Action
            actionTimer.record(() -> actionInvoker.onEvent(eventToSend));
        } else {
            LOG.debug("Filters of processor '{}' did not match for event with id '{}'", processor.getId(), cloudEvent.getId());
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

    private void initMetricFields(ProcessorDTO processor, MeterRegistry registry) {
        List<Tag> tags = Arrays.asList(
                Tag.of(MetricsConstants.BRIDGE_ID_TAG, processor.getBridgeId()), Tag.of(MetricsConstants.PROCESSOR_ID_TAG, processor.getId()));
        this.processorProcessingTime = registry.timer(MetricsConstants.PROCESSOR_PROCESSING_TIME_METRIC_NAME, tags);
        this.filterTimer = registry.timer(MetricsConstants.FILTER_PROCESSING_TIME_METRIC_NAME, tags);
        this.actionTimer = registry.timer(MetricsConstants.ACTION_PROCESSING_TIME_METRIC_NAME, tags);
        this.transformationTimer = registry.timer(MetricsConstants.TRANSFORMATION_PROCESSING_TIME_METRIC_NAME, tags);
    }
}
