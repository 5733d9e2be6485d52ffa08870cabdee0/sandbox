package com.redhat.service.smartevents.manager.metrics;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.ManagedResource;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.utils.Fixtures;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class MetricsServiceImplTest {

    @Inject
    MetricsService metricsService;

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "rhose.metrics-name.operation-total-count")
    String operationTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-success-total-count")
    String operationTotalSuccessCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-duration-seconds")
    String operatonDurationMetricName;

    private List<Tag> createdExpectedTags(ManagedResource managedResource, MetricsOperation operation) {
        return List.of(Tag.of(MetricsServiceImpl.INSTANCE_TAG, managedResource.getClass().getSimpleName().toLowerCase()), operation.getMetricTag());
    }

    @BeforeEach
    public void beforeEach() {
        meterRegistry.clear();
    }

    @ParameterizedTest
    @EnumSource(MetricsOperation.class)
    public void onOperationStart_forBridge(MetricsOperation metricsOperation) {

        Bridge bridge = Fixtures.createBridge();
        ManagedResourceStatus status = metricsOperation == MetricsOperation.DELETE ? ManagedResourceStatus.DEPROVISION : ManagedResourceStatus.ACCEPTED;
        bridge.setStatus(status);
        metricsService.onOperationStart(bridge, metricsOperation);

        List<Tag> expectedTags = createdExpectedTags(bridge, metricsOperation);
        assertThat(meterRegistry.counter(operationTotalCountMetricName, expectedTags).count()).isEqualTo(1.0);
    }

    @ParameterizedTest
    @EnumSource(MetricsOperation.class)
    public void onOperationStart_forProcessor(MetricsOperation metricsOperation) {
        Bridge bridge = Fixtures.createBridge();
        ManagedResourceStatus status = metricsOperation == MetricsOperation.DELETE ? ManagedResourceStatus.DEPROVISION : ManagedResourceStatus.ACCEPTED;
        Processor processor = Fixtures.createProcessor(bridge, status);

        metricsService.onOperationStart(processor, metricsOperation);

        List<Tag> expectedTags = createdExpectedTags(processor, metricsOperation);
        assertThat(meterRegistry.counter(operationTotalCountMetricName, expectedTags).count()).isEqualTo(1.0);
    }

    @ParameterizedTest
    @EnumSource(MetricsOperation.class)
    public void onOperationComplete_forBridge(MetricsOperation metricsOperation) {
        Bridge bridge = Fixtures.createBridge();
        ManagedResourceStatus status = metricsOperation == MetricsOperation.DELETE ? ManagedResourceStatus.DELETED : ManagedResourceStatus.READY;
        bridge.setSubmittedAt(ZonedDateTime.now().minusMinutes(4));
        bridge.setPublishedAt(ZonedDateTime.now().minusMinutes(3));
        bridge.setModifiedAt(ZonedDateTime.now().minusMinutes(3));
        bridge.setDeletedAt(ZonedDateTime.now().minusMinutes(1));
        bridge.setStatus(status);

        metricsService.onOperationComplete(bridge, metricsOperation);

        List<Tag> expectedTags = createdExpectedTags(bridge, metricsOperation);
        assertThat(meterRegistry.counter(operationTotalSuccessCountMetricName, expectedTags).count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer(operatonDurationMetricName, expectedTags).totalTime(TimeUnit.MINUTES)).isNotEqualTo(0);
    }

    @ParameterizedTest
    @EnumSource(MetricsOperation.class)
    public void onOperationComplete_forProcessor(MetricsOperation metricsOperation) {
        Bridge bridge = Fixtures.createBridge();
        ManagedResourceStatus status = metricsOperation == MetricsOperation.DELETE ? ManagedResourceStatus.DELETED : ManagedResourceStatus.READY;
        Processor processor = Fixtures.createProcessor(bridge, status);
        processor.setSubmittedAt(ZonedDateTime.now().minusMinutes(4));
        processor.setPublishedAt(ZonedDateTime.now().minusMinutes(3));
        processor.setModifiedAt(ZonedDateTime.now().minusMinutes(2));
        processor.setDeletedAt(ZonedDateTime.now().minusMinutes(1));

        metricsService.onOperationComplete(processor, metricsOperation);

        List<Tag> expectedTags = createdExpectedTags(processor, metricsOperation);
        assertThat(meterRegistry.counter(operationTotalSuccessCountMetricName, expectedTags).count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer(operatonDurationMetricName, expectedTags).totalTime(TimeUnit.MINUTES)).isNotEqualTo(0);
    }

    @Test
    public void onOperationComplete_forFailedProcessor() {

        Bridge bridge = Fixtures.createBridge();
        Processor processor = Fixtures.createProcessor(bridge, ManagedResourceStatus.FAILED);

        metricsService.onOperationComplete(processor, MetricsOperation.PROVISION);

        List<Tag> expectedTags = createdExpectedTags(processor, MetricsOperation.PROVISION);
        assertThat(meterRegistry.counter(operationTotalSuccessCountMetricName, expectedTags).count()).isEqualTo(0.0);
        assertThat(meterRegistry.timer(operatonDurationMetricName, expectedTags).totalTime(TimeUnit.MINUTES)).isEqualTo(0.0);
    }

    @Test
    public void onOperationComplete_forFailedBridge() {

        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(ManagedResourceStatus.FAILED);

        metricsService.onOperationComplete(bridge, MetricsOperation.PROVISION);

        List<Tag> expectedTags = createdExpectedTags(bridge, MetricsOperation.PROVISION);
        assertThat(meterRegistry.counter(operationTotalSuccessCountMetricName, expectedTags).count()).isEqualTo(0.0);
        assertThat(meterRegistry.timer(operatonDurationMetricName, expectedTags).totalTime(TimeUnit.MINUTES)).isEqualTo(0.0);
    }
}
