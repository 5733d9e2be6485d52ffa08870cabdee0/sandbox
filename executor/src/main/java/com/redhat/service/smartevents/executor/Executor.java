package com.redhat.service.smartevents.executor;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.executor.filters.FilterEvaluator;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluator;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.smartevents.infra.utils.CloudEventUtils;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.ActionRuntime;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import static com.redhat.service.smartevents.processor.actions.source.SourceActionInvoker.CLOUD_EVENT_SOURCE;

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
        processorProcessingTime.record(() -> process(toCloudEvent(event)));
    }

    private CloudEvent toCloudEvent(String event) {
        return processor.getType() == ProcessorType.SOURCE
                ? wrapToCloudEvent(event)
                : CloudEventUtils.decode(event);
    }

    private CloudEvent wrapToCloudEvent(String event) {
        try {
            // try decoding as CloudEvent as received from Kafka
            return CloudEventUtils.decode(event);
        } catch (CloudEventDeserializationException e1) {
            // if the decode fails, wrap it in a CloudEvent
            try {
                return CloudEventBuilder.v1()
                        .withId(UUID.randomUUID().toString())
                        .withSource(URI.create(CLOUD_EVENT_SOURCE))
                        .withType(String.format("%sSource", processor.getDefinition().getRequestedSource().getType()))
                        .withData(JsonCloudEventData.wrap(mapper.readTree(event)))
                        .build();
            } catch (JsonProcessingException e2) {
                LOG.error("JsonProcessingException when generating CloudEvent for '{}'", event, e2);
                throw new CloudEventDeserializationException("Failed to generate event map");
            }
        }
    }

    private void process(CloudEvent event) {
        Map<String, Object> eventMap = toEventMap(event);

        LOG.debug("Received event with id '{}' and type '{}' in processor with name '{}' of bridge '{}", event.getId(), event.getType(), processor.getName(), processor.getBridgeId());

        // Filter evaluation
        if (!matchesFilters(eventMap)) {
            LOG.debug("Filters of processor '{}' did not match for event with id '{}' and type '{}'", processor.getId(), event.getId(), event.getType());
            return;
        }
        LOG.info("Filters of processor '{}' matched for event with id '{}' and type '{}'", processor.getId(), event.getId(), event.getType());
        // Transformation
        String eventToSend = applyTransformations(eventMap);
        // Action
        actionTimer.record(() -> actionInvoker.onEvent(eventToSend));
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
