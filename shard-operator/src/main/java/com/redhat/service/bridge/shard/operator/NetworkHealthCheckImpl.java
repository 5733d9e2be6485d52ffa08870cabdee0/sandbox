package com.redhat.service.bridge.shard.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@Default
@ApplicationScoped
public class NetworkHealthCheckImpl implements NetworkHealthCheck {

    private static final int FAILURES_THRESHOLD = 10;

    private int numberOfConsecutiveFailures = 0;

    @Override
    public HealthCheckResponse call() {
        if (numberOfConsecutiveFailures > FAILURES_THRESHOLD) {
            return HealthCheckResponse.builder()
                    .name("Network check")
                    .withData("consecutiveFailures", numberOfConsecutiveFailures)
                    .down()
                    .build();
        } else {
            return HealthCheckResponse.builder()
                    .name("Network check")
                    .withData("consecutiveFailures", numberOfConsecutiveFailures)
                    .up()
                    .build();
        }
    }

    @Override
    public synchronized void recordSuccess() {
        numberOfConsecutiveFailures = 0;
    }

    @Override
    public synchronized void recordFailure() {
        numberOfConsecutiveFailures++;
    }
}
