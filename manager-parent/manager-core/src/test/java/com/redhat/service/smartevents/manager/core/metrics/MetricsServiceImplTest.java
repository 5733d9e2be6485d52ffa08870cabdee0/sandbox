package com.redhat.service.smartevents.manager.core.metrics;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.core.mocks.ManagedResourceForTests;
import com.redhat.service.smartevents.manager.core.models.ManagedResource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class MetricsServiceImplTest {

    @Inject
    ManagerMetricsService metricsService;

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "rhose.metrics-name.operation-total-count")
    String operationTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-success-total-count")
    String operationTotalSuccessCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-duration-seconds")
    String operatonDurationMetricName;

    private List<Tag> createdExpectedTags(ManagedResource managedResource, MetricsOperation operation) {
        return List.of(Tag.of(MetricsServiceImpl.RESOURCE_TAG, managedResource.getClass().getSimpleName().toLowerCase()), operation.getMetricTag());
    }

    @BeforeEach
    public void beforeEach() {
        meterRegistry.clear();
    }

    @ParameterizedTest
    @EnumSource(value = MetricsOperation.class, names = { "MANAGER_RESOURCE_.+" }, mode = EnumSource.Mode.MATCH_ALL)
    public void onOperationStart(MetricsOperation metricsOperation) {
        ManagedResource resource = new ManagedResourceForTests();
        ManagedResourceStatus status = metricsOperation == MetricsOperation.MANAGER_RESOURCE_DELETE ? ManagedResourceStatus.DEPROVISION : ManagedResourceStatus.ACCEPTED;
        resource.setStatus(status);
        metricsService.onOperationStart(resource, metricsOperation);

        List<Tag> expectedTags = createdExpectedTags(resource, metricsOperation);
        assertThat(meterRegistry.counter(operationTotalCountMetricName, expectedTags).count()).isEqualTo(1.0);
    }

    @ParameterizedTest
    @EnumSource(value = MetricsOperation.class, names = { "MANAGER_RESOURCE_.+" }, mode = EnumSource.Mode.MATCH_ALL)
    public void onOperationComplete_Success(MetricsOperation metricsOperation) {
        ManagedResource resource = new ManagedResourceForTests();
        ManagedResourceStatus status = metricsOperation == MetricsOperation.MANAGER_RESOURCE_DELETE ? ManagedResourceStatus.DELETED : ManagedResourceStatus.READY;
        resource.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(4));
        resource.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(3));
        resource.setModifiedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(3));
        resource.setDeletionRequestedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        resource.setStatus(status);

        metricsService.onOperationComplete(resource, metricsOperation);

        List<Tag> expectedTags = createdExpectedTags(resource, metricsOperation);
        assertThat(meterRegistry.counter(operationTotalSuccessCountMetricName, expectedTags).count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer(operatonDurationMetricName, expectedTags).totalTime(TimeUnit.MINUTES)).isNotEqualTo(0);
    }

    @Test
    public void onOperationComplete_Failed() {
        ManagedResource resource = new ManagedResourceForTests();
        resource.setStatus(ManagedResourceStatus.FAILED);

        metricsService.onOperationComplete(resource, MetricsOperation.MANAGER_RESOURCE_PROVISION);

        List<Tag> expectedTags = createdExpectedTags(resource, MetricsOperation.MANAGER_RESOURCE_PROVISION);
        assertThat(meterRegistry.counter(operationTotalSuccessCountMetricName, expectedTags).count()).isEqualTo(0.0);
        assertThat(meterRegistry.timer(operatonDurationMetricName, expectedTags).totalTime(TimeUnit.MINUTES)).isEqualTo(0.0);
    }
}
