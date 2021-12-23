package com.redhat.service.bridge.shard.operator;

import org.eclipse.microprofile.health.HealthCheck;

public interface NetworkHealthCheck extends HealthCheck {

    void recordSuccess();

    void recordFailure();

}
