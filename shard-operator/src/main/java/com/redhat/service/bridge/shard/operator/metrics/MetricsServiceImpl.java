package com.redhat.service.bridge.shard.operator.metrics;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class MetricsServiceImpl implements MetricsService {

    private static final String MANAGER_REQUEST_METRICS = "http.manager.request";

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "event-bridge.sso.grant-options.password.username")
    String username;

    @Override
    public void updateManagerRequestMetrics(ManagerRequestType requestType, ManagerRequestStatus status) {
        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("shardId", username));
        tags.add(Tag.of("type", requestType.name()));
        tags.add(Tag.of("status", status.name()));
        meterRegistry.counter(MANAGER_REQUEST_METRICS, tags).increment();
    }
}
