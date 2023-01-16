package com.redhat.service.smartevents.manager.v2.metrics;

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
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.manager.core.models.ManagedResource;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.infra.core.metrics.MetricsOperation.MANAGER_RESOURCE_PROVISION;
import static com.redhat.service.smartevents.infra.core.metrics.MetricsOperation.MANAGER_RESOURCE_UPDATE;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createFailedConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorAcceptedConditions;
import static com.redhat.service.smartevents.manager.v2.utils.Fixtures.createProcessorDeprovisionConditions;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class MetricsServiceImplTest {

    @Inject
    ManagerMetricsServiceV2 metricsService;

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "rhose.metrics-name.operation-total-count")
    String operationTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-success-total-count")
    String operationTotalSuccessCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operation-duration-seconds")
    String operatonDurationMetricName;

    private List<Tag> createdExpectedTags(ManagedResource managedResource, MetricsOperation operation) {
        return List.of(Tag.of(MetricsServiceImpl.RESOURCE_TAG, managedResource.getClass().getSimpleName().toLowerCase()),
                Tag.of(MetricsServiceImpl.RESOURCE_ID_TAG, MetricsServiceImpl.WILDCARD),
                Tag.of(MetricsServiceImpl.VERSION_TAG, V2.class.getSimpleName()),
                operation.getMetricTag());
    }

    private List<Tag> createdExpectedInstanceTags(ManagedResource managedResource, MetricsOperation operation) {
        return List.of(Tag.of(MetricsServiceImpl.RESOURCE_TAG, managedResource.getClass().getSimpleName().toLowerCase()),
                Tag.of(MetricsServiceImpl.RESOURCE_ID_TAG, managedResource.getId()),
                Tag.of(MetricsServiceImpl.VERSION_TAG, V2.class.getSimpleName()),
                operation.getMetricTag());
    }

    @BeforeEach
    public void beforeEach() {
        meterRegistry.clear();
    }

    @ParameterizedTest
    @EnumSource(value = MetricsOperation.class, names = { "MANAGER_RESOURCE_.+" }, mode = EnumSource.Mode.MATCH_ALL)
    public void onOperationStart(MetricsOperation metricsOperation) {
        Processor resource = new Processor();
        resource.setConditions(makeConditions(metricsOperation));

        metricsService.onOperationStart(resource, metricsOperation);

        List<Tag> expectedTags = createdExpectedTags(resource, metricsOperation);
        assertThat(meterRegistry.counter(operationTotalCountMetricName, expectedTags).count()).isEqualTo(1.0);
    }

    @ParameterizedTest
    @EnumSource(value = MetricsOperation.class, names = { "MANAGER_RESOURCE_.+" }, mode = EnumSource.Mode.MATCH_ALL)
    public void onOperationComplete_Success(MetricsOperation metricsOperation) {
        Processor resource = new Processor();
        resource.setConditions(makeConditions(metricsOperation));
        resource.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(4));
        resource.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC).minusMinutes(3));
        Operation operation = new Operation();
        if (metricsOperation == MANAGER_RESOURCE_PROVISION) {
            operation.setType(OperationType.CREATE);
        } else if (metricsOperation == MANAGER_RESOURCE_UPDATE) {
            operation.setType(OperationType.UPDATE);
        } else {
            operation.setType(OperationType.DELETE);
        }
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        resource.setOperation(operation);

        metricsService.onOperationComplete(resource, metricsOperation);

        List<Tag> expectedTags = createdExpectedTags(resource, metricsOperation);
        List<Tag> expectedInstanceTags = createdExpectedInstanceTags(resource, metricsOperation);
        assertThat(meterRegistry.counter(operationTotalSuccessCountMetricName, expectedTags).count()).isEqualTo(1.0);
        assertThat(meterRegistry.timer(operatonDurationMetricName, expectedTags).totalTime(TimeUnit.MINUTES)).isNotEqualTo(0);
        assertThat(meterRegistry.timer(operatonDurationMetricName, expectedInstanceTags).totalTime(TimeUnit.MINUTES)).isNotEqualTo(0);
    }

    @Test
    public void onOperationComplete_Failed() {
        Processor resource = new Processor();
        resource.setConditions(createFailedConditions());
        Operation operation = new Operation();
        operation.setType(OperationType.CREATE);
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        resource.setOperation(operation);

        metricsService.onOperationComplete(resource, MANAGER_RESOURCE_PROVISION);

        List<Tag> expectedTags = createdExpectedTags(resource, MANAGER_RESOURCE_PROVISION);
        List<Tag> expectedInstanceTags = createdExpectedInstanceTags(resource, MANAGER_RESOURCE_PROVISION);
        assertThat(meterRegistry.counter(operationTotalSuccessCountMetricName, expectedTags).count()).isEqualTo(0.0);
        assertThat(meterRegistry.timer(operatonDurationMetricName, expectedTags).totalTime(TimeUnit.MINUTES)).isEqualTo(0.0);
        assertThat(meterRegistry.timer(operatonDurationMetricName, expectedInstanceTags).totalTime(TimeUnit.MINUTES)).isEqualTo(0);
    }

    private List<Condition> makeConditions(MetricsOperation metricsOperation) {
        List<Condition> conditions;
        if (metricsOperation == MANAGER_RESOURCE_PROVISION) {
            conditions = createProcessorAcceptedConditions();
        } else if (metricsOperation == MANAGER_RESOURCE_UPDATE) {
            conditions = createProcessorAcceptedConditions();
        } else {
            conditions = createProcessorDeprovisionConditions();
        }
        conditions.forEach(c -> c.setStatus(ConditionStatus.TRUE));
        return conditions;
    }
}
