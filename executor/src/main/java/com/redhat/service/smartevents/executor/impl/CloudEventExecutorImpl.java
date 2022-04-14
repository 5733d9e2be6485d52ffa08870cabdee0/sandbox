package com.redhat.service.smartevents.executor.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.executor.Executor;
import com.redhat.service.smartevents.executor.MetricsConstants;
import com.redhat.service.smartevents.executor.filters.FilterEvaluator;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluator;
import com.redhat.service.smartevents.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.smartevents.infra.utils.CloudEventUtils;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

public class CloudEventExecutorImpl implements Executor {

    private static final Logger LOG = LoggerFactory.getLogger(CloudEventExecutorImpl.class);

    private final ProcessorDTO processor;
    private final FilterEvaluator filterEvaluator;
    private final TransformationEvaluator transformationEvaluator;
    private final ActionInvoker actionInvoker;
    private Timer processorProcessingTime;
    private Timer filterTimer;
    private Timer actionTimer;
    private Timer transformationTimer;

    public CloudEventExecutorImpl(ProcessorDTO processor, FilterEvaluatorFactory filterEvaluatorFactory, TransformationEvaluatorFactory transformationFactory,
            ActionInvoker actionInvoker,
            MeterRegistry registry) {
        this.processor = processor;
        this.filterEvaluator = filterEvaluatorFactory.build(processor.getDefinition().getFilters());
        this.transformationEvaluator = transformationFactory.build(processor.getDefinition().getTransformationTemplate());
        this.actionInvoker = actionInvoker;
        initMetricFields(processor, registry);
    }

    @Override
    public void onEvent(String event) {
        onCloudEvent(CloudEventUtils.decode(event));
    }

    void onCloudEvent(CloudEvent cloudEvent) {
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

    @Override
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
        CloudEventExecutorImpl executor = (CloudEventExecutorImpl) o;
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
