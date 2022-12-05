package com.redhat.service.smartevents.shard.operator.core.metrics;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WithOpenShiftTestServer
@QuarkusTest
public class MetricsServiceImplTest {

    @Inject
    OperatorMetricsService metricsService;

    @InjectMock(convertScopes = true)
    MeterRegistry meterRegistry;

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateManagerRequestMetrics() {
        Counter counter = mock(Counter.class);
        when(meterRegistry.counter(anyString(), anyList())).thenReturn(counter);
        MetricsOperation operation = MetricsOperation.OPERATOR_MANAGER_UPDATE;
        ManagerRequestStatus status = ManagerRequestStatus.FAILURE;
        String statusCode = "408";
        metricsService.updateManagerRequestMetrics(operation, status, statusCode);
        ArgumentCaptor<List<Tag>> argumentCaptor = forClass(List.class);
        verify(counter).increment();
        verify(meterRegistry).counter(eq("managed_services_api_rhose_manager_requests_count"), argumentCaptor.capture());
        List<Tag> actualTags = argumentCaptor.getValue();
        assertThat(actualTags).size().isEqualTo(4);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateManagerRequestMetrics_whenStatusIsNull() {
        Counter counter = mock(Counter.class);
        when(meterRegistry.counter(anyString(), anyList())).thenReturn(counter);
        MetricsOperation operation = MetricsOperation.OPERATOR_MANAGER_UPDATE;
        ManagerRequestStatus status = ManagerRequestStatus.FAILURE;
        metricsService.updateManagerRequestMetrics(operation, status, null);
        ArgumentCaptor<List<Tag>> argumentCaptor = forClass(List.class);
        verify(counter).increment();
        verify(meterRegistry).counter(eq("managed_services_api_rhose_manager_requests_count"), argumentCaptor.capture());
        List<Tag> actualTags = argumentCaptor.getValue();
        assertThat(actualTags).size().isEqualTo(3);
    }
}
