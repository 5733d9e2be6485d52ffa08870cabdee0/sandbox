package com.redhat.service.smartevents.shard.operator.v2.metrics;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.shard.operator.core.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.shard.operator.core.metrics.SupportsTimers;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static com.redhat.service.smartevents.infra.core.metrics.MetricsOperation.CONTROLLER_RESOURCE_PROVISION;
import static com.redhat.service.smartevents.infra.core.metrics.MetricsOperation.OPERATOR_MANAGER_UPDATE;
import static com.redhat.service.smartevents.shard.operator.core.metrics.BaseOperatorMetricsServiceImpl.HTTP_STATUS_CODE;
import static com.redhat.service.smartevents.shard.operator.core.metrics.BaseOperatorMetricsServiceImpl.REQUEST_STATUS;
import static com.redhat.service.smartevents.shard.operator.core.metrics.BaseOperatorMetricsServiceImpl.RESOURCE_ID_TAG;
import static com.redhat.service.smartevents.shard.operator.core.metrics.BaseOperatorMetricsServiceImpl.RESOURCE_TAG;
import static com.redhat.service.smartevents.shard.operator.core.metrics.BaseOperatorMetricsServiceImpl.SHARD_ID;
import static com.redhat.service.smartevents.shard.operator.core.metrics.BaseOperatorMetricsServiceImpl.VERSION_TAG;
import static com.redhat.service.smartevents.shard.operator.core.metrics.BaseOperatorMetricsServiceImpl.WILDCARD;
import static com.redhat.service.smartevents.shard.operator.core.metrics.ManagerRequestStatus.FAILURE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WithOpenShiftTestServer
@QuarkusTest
public class MetricsServiceImplTest {

    private static final SupportsTimers RESOURCE = new SupportsTimers() {
        @Override
        public String getId() {
            return "id";
        }

        @Override
        public ZonedDateTime getCreationTimestamp() {
            return ZonedDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS);
        }
    };

    @ConfigProperty(name = "rhose.metrics-name.manager-requests-total-count")
    String managerRequestsTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operator.operation-total-count")
    String operatorTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operator.operation-success-total-count")
    String operatorTotalSuccessCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operator.operation-failure-total-count")
    String operatorTotalFailureCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operator.operation-duration-seconds")
    String operationDurationMetricName;

    @V2
    @Inject
    OperatorMetricsService metricsService;

    @InjectMock(convertScopes = true)
    MeterRegistry meterRegistry;

    @Test
    public void testOnOperationStart() {
        assertCounter(operatorTotalCountMetricName,
                () -> metricsService.onOperationStart(RESOURCE, CONTROLLER_RESOURCE_PROVISION));
    }

    @Test
    public void testOnOperationComplete() {
        assertCounter(operatorTotalSuccessCountMetricName,
                () -> metricsService.onOperationComplete(RESOURCE, CONTROLLER_RESOURCE_PROVISION));
        assertTimer(operationDurationMetricName,
                () -> metricsService.onOperationComplete(RESOURCE, CONTROLLER_RESOURCE_PROVISION));
    }

    @Test
    public void testOnOperationFailed() {
        assertCounter(operatorTotalFailureCountMetricName,
                () -> metricsService.onOperationFailed(RESOURCE, CONTROLLER_RESOURCE_PROVISION));
        assertTimer(operationDurationMetricName,
                () -> metricsService.onOperationFailed(RESOURCE, CONTROLLER_RESOURCE_PROVISION));
    }

    private void assertCounter(String counterName, Runnable operation) {
        reset(meterRegistry);
        Counter counter = mock(Counter.class);
        when(meterRegistry.counter(anyString(), anyList())).thenReturn(counter);
        Timer timer = mock(Timer.class);
        when(meterRegistry.timer(anyString(), anyList())).thenReturn(timer);

        operation.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tag>> argumentCaptor = forClass(List.class);
        verify(counter).increment();
        verify(meterRegistry).counter(eq(counterName), argumentCaptor.capture());

        List<Tag> actualTags = argumentCaptor.getValue();
        assertThat(actualTags).size().isEqualTo(5);
        assertTag(actualTags, SHARD_ID, (v) -> assertThat(v).isNotNull());
        assertTag(actualTags, RESOURCE_TAG, (v) -> assertThat(v).isNotNull());
        assertTag(actualTags, RESOURCE_ID_TAG, (v) -> assertThat(v).isEqualTo(WILDCARD));
        assertTag(actualTags, VERSION_TAG, (v) -> assertThat(v).isEqualTo("V2"));
        assertTag(actualTags, "operation", (v) -> assertThat(v).isEqualTo(CONTROLLER_RESOURCE_PROVISION.getMetricTag().getValue()));
    }

    private void assertTimer(String timerName, Runnable operation) {
        reset(meterRegistry);
        Counter counter = mock(Counter.class);
        when(meterRegistry.counter(anyString(), anyList())).thenReturn(counter);
        Timer timer = mock(Timer.class);
        when(meterRegistry.timer(anyString(), anyList())).thenReturn(timer);

        operation.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Tag>> argumentCaptor = forClass(List.class);
        verify(counter).increment();
        verify(meterRegistry, times(2)).timer(eq(timerName), argumentCaptor.capture());

        List<List<Tag>> allTags = argumentCaptor.getAllValues();
        assertThat(allTags).size().isEqualTo(2);

        List<Tag> globalTimerTags = allTags.get(0);
        assertThat(globalTimerTags).size().isEqualTo(5);
        assertTag(globalTimerTags, SHARD_ID, (v) -> assertThat(v).isNotNull());
        assertTag(globalTimerTags, RESOURCE_TAG, (v) -> assertThat(v).isNotNull());
        assertTag(globalTimerTags, RESOURCE_ID_TAG, (v) -> assertThat(v).isEqualTo(WILDCARD));
        assertTag(globalTimerTags, VERSION_TAG, (v) -> assertThat(v).isEqualTo("V2"));
        assertTag(globalTimerTags, "operation", (v) -> assertThat(v).isEqualTo(CONTROLLER_RESOURCE_PROVISION.getMetricTag().getValue()));

        List<Tag> resourceTimerTags = allTags.get(1);
        assertThat(resourceTimerTags).size().isEqualTo(5);
        assertTag(resourceTimerTags, SHARD_ID, (v) -> assertThat(v).isNotNull());
        assertTag(resourceTimerTags, RESOURCE_TAG, (v) -> assertThat(v).isNotNull());
        assertTag(resourceTimerTags, RESOURCE_ID_TAG, (v) -> assertThat(v).isEqualTo(RESOURCE.getId()));
        assertTag(resourceTimerTags, VERSION_TAG, (v) -> assertThat(v).isEqualTo("V2"));
        assertTag(resourceTimerTags, "operation", (v) -> assertThat(v).isEqualTo(CONTROLLER_RESOURCE_PROVISION.getMetricTag().getValue()));
    }

    private void assertTag(List<Tag> tags, String tagName, Consumer<String> assertion) {
        String value = tags.stream().filter(tag -> Objects.equals(tag.getKey(), tagName)).map(Tag::getValue).findFirst().orElseThrow();
        assertion.accept(value);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateManagerRequestMetrics() {
        Counter counter = mock(Counter.class);
        when(meterRegistry.counter(anyString(), anyList())).thenReturn(counter);

        metricsService.updateManagerRequestMetrics(OPERATOR_MANAGER_UPDATE, FAILURE, "408");

        ArgumentCaptor<List<Tag>> argumentCaptor = forClass(List.class);
        verify(counter).increment();
        verify(meterRegistry).counter(eq(managerRequestsTotalCountMetricName), argumentCaptor.capture());

        List<Tag> actualTags = argumentCaptor.getValue();
        assertThat(actualTags).size().isEqualTo(4);
        assertTag(actualTags, SHARD_ID, (v) -> assertThat(v).isNotNull());
        assertTag(actualTags, REQUEST_STATUS, (v) -> assertThat(v).isEqualTo(FAILURE.name()));
        assertTag(actualTags, HTTP_STATUS_CODE, (v) -> assertThat(v).isEqualTo("408"));
        assertTag(actualTags, "operation", (v) -> assertThat(v).isEqualTo(OPERATOR_MANAGER_UPDATE.getMetricTag().getValue()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateManagerRequestMetrics_whenStatusIsNull() {
        Counter counter = mock(Counter.class);
        when(meterRegistry.counter(anyString(), anyList())).thenReturn(counter);

        metricsService.updateManagerRequestMetrics(OPERATOR_MANAGER_UPDATE, FAILURE, null);

        ArgumentCaptor<List<Tag>> argumentCaptor = forClass(List.class);
        verify(counter).increment();
        verify(meterRegistry).counter(eq(managerRequestsTotalCountMetricName), argumentCaptor.capture());

        List<Tag> actualTags = argumentCaptor.getValue();
        assertThat(actualTags).size().isEqualTo(3);
        assertTag(actualTags, SHARD_ID, (v) -> assertThat(v).isNotNull());
        assertTag(actualTags, REQUEST_STATUS, (v) -> assertThat(v).isEqualTo(FAILURE.name()));
        assertTag(actualTags, "operation", (v) -> assertThat(v).isEqualTo(OPERATOR_MANAGER_UPDATE.getMetricTag().getValue()));
    }

}
