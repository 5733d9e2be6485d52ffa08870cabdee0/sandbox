package com.redhat.service.smartevents.executor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.executor.filters.FilterEvaluator;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluator;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.ActionRuntime;

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
    private final ObjectMapper mapper;

    public Executor(ProcessorDTO processor, FilterEvaluatorFactory filterEvaluatorFactory, TransformationEvaluatorFactory transformationFactory,
            ActionRuntime actionRuntime,
            MeterRegistry registry, ObjectMapper mapper) {
        this.processor = processor;
        this.mapper = mapper;
        this.filterEvaluator = filterEvaluatorFactory.build(processor.getDefinition().getFilters());

        this.transformationEvaluator = transformationFactory.build(processor.getDefinition().getTransformationTemplate());

        Action action = processor.getDefinition().getResolvedAction();
        this.actionInvoker = actionRuntime.getInvokerBuilder(action.getType()).build(processor, action);

        initMetricFields(processor, registry);
    }

    public void onEvent(String event) {
        processorProcessingTime.record(() -> process(event));
    }

    private void process(String event) {
        Map<String, Object> eventMap = toEventMap(event);
        String eventIdForLog = Optional.ofNullable(eventMap.get("id")).map(Object::toString).orElse("<unknown>");

        LOG.info("Received event with id '{}' for Processor with name '{}' on Bridge '{}", eventIdForLog, processor.getName(), processor.getBridgeId());

        // Filter evaluation
        if (Boolean.TRUE.equals(filterTimer.record(() -> filterEvaluator.evaluateFilters(eventMap)))) {
            LOG.info("Filters of processor '{}' matched for event with id '{}'", processor.getId(), eventIdForLog);

            // Transformation
            String eventToSend = transformationTimer.record(() -> transformationEvaluator.render(eventMap));

            // Action
            actionTimer.record(() -> actionInvoker.onEvent(eventToSend));
        } else {
            LOG.debug("Filters of processor '{}' did not match for event with id '{}'", processor.getId(), eventIdForLog);
            // DO NOTHING;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toEventMap(String event) {
        try {
            return mapper.readValue(event, Map.class);
        } catch (JsonProcessingException e) {
            LOG.error("JsonProcessingException when generating event map for '{}'", event, e);
            throw new CloudEventDeserializationException("Failed to generate event map");
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
