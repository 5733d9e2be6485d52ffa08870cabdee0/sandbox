package com.redhat.service.smartevents.executor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.executor.filters.FilterEvaluator;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluator;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.smartevents.infra.utils.CloudEventUtils;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.ActionRuntime;

import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

public class ExecutorImpl implements Executor {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorImpl.class);

    private final ProcessorDTO processor;
    private final boolean isSourceProcessor;
    private final FilterEvaluator filterEvaluator;
    private final TransformationEvaluator transformationEvaluator;
    private final ActionInvoker actionInvoker;

    private Timer processorProcessingTime;
    private Timer filterTimer;
    private Timer actionTimer;
    private Timer transformationTimer;

    public ExecutorImpl(
            ProcessorDTO processor,
            FilterEvaluatorFactory filterEvaluatorFactory,
            TransformationEvaluatorFactory transformationFactory,
            ActionRuntime actionRuntime,
            MeterRegistry registry) {
        this.processor = processor;
        this.isSourceProcessor = processor.getType() == ProcessorType.SOURCE;
        this.filterEvaluator = filterEvaluatorFactory.build(processor.getDefinition().getFilters());
        this.transformationEvaluator = transformationFactory.build(processor.getDefinition().getTransformationTemplate());

        Action action = processor.getDefinition().getResolvedAction();
        this.actionInvoker = actionRuntime.getInvokerBuilder(action.getType()).build(processor, action);

        initMetricFields(processor, registry);
    }

    @Override
    public ProcessorDTO getProcessor() {
        return processor;
    }

    @Override
    public void onEvent(CloudEvent event, Map<String, String> traceHeaders) {
        processorProcessingTime.record(() -> process(event, traceHeaders));
    }

    private void process(CloudEvent event, Map<String, String> traceHeaders) {
        Map<String, Object> eventMap = toEventMap(event);

        LOG.debug("Received event with id '{}' and type '{}' in processor with name '{}' of bridge '{}", event.getId(), event.getType(), processor.getName(), processor.getBridgeId());

        // Filter evaluation
        if (!matchesFilters(eventMap)) {
            LOG.debug("Filters of processor '{}' did not match for event with id '{}' and type '{}'", processor.getId(), event.getId(), event.getType());
            return;
        }
        LOG.info("Filters of processor '{}' matched for event with id '{}' and type '{}'", processor.getId(), event.getId(), event.getType());
        // Transformation
        // transformations are currently supported only for sink processors
        String eventToSend = isSourceProcessor ? CloudEventUtils.encode(event) : applyTransformations(eventMap);
        // Action
        actionTimer.record(() -> actionInvoker.onEvent(eventToSend, traceHeaders));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toEventMap(CloudEvent event) {
        return CloudEventUtils.getMapper().convertValue(event, Map.class);
    }

    private boolean matchesFilters(Map<String, Object> eventMap) {
        return Boolean.TRUE.equals(filterTimer.record(() -> filterEvaluator.evaluateFilters(eventMap)));
    }

    private String applyTransformations(Map<String, Object> eventMap) {
        return transformationTimer.record(() -> transformationEvaluator.render(eventMap));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExecutorImpl executor = (ExecutorImpl) o;
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
