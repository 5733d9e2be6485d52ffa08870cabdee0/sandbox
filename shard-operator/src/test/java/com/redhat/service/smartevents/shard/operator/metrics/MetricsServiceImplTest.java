package com.redhat.service.smartevents.shard.operator.metrics;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.redhat.service.smartevents.infra.metrics.MetricsOperation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
public class MetricsServiceImplTest {

    @Inject
    OperatorMetricsService metricsService;

    @InjectMock
    MeterRegistry meterRegistry;

    @Test
    public void testUpdateManagerRequestMetrics() {
        Mockito.doNothing().when(meterRegistry.counter(anyString(), anyList()));
        MetricsOperation operation = MetricsOperation.OPERATOR_MANAGER_UPDATE;
        ManagerRequestStatus status = ManagerRequestStatus.FAILURE;
        String statusCode = "408";
        metricsService.updateManagerRequestMetrics(operation, status, statusCode);
        ArgumentCaptor<List<Tag>> argumentCaptor = forClass(List.class);
        Mockito.verify(meterRegistry).counter(Mockito.eq("http.manager.request"), argumentCaptor.capture()).increment();
        List<Tag> actualTags = argumentCaptor.getValue();
        assertThat(actualTags).size().isEqualTo(4);
    }

    @Test
    public void testUpdateManagerRequestMetrics_whenStatusIsNull() {
        Mockito.doNothing().when(meterRegistry.counter(anyString(), anyList()));
        MetricsOperation operation = MetricsOperation.OPERATOR_MANAGER_UPDATE;
        ManagerRequestStatus status = ManagerRequestStatus.FAILURE;
        metricsService.updateManagerRequestMetrics(operation, status, null);
        ArgumentCaptor<List<Tag>> argumentCaptor = forClass(List.class);
        Mockito.verify(meterRegistry).counter(Mockito.eq("http.manager.request"), argumentCaptor.capture()).increment();
        List<Tag> actualTags = argumentCaptor.getValue();
        assertThat(actualTags).size().isEqualTo(3);
    }
}
