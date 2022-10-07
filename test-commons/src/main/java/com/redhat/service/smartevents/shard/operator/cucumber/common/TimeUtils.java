package com.redhat.service.smartevents.shard.operator.cucumber.common;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

public final class TimeUtils {

    private static final Duration DEFAULT_WAIT_STEP = Duration.of(1, ChronoUnit.SECONDS);

    private TimeUtils() {
    }

    public static void wait(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException("Waiting was interrupted", e);
        }
    }

    public static void waitForCondition(Duration maxTime, BooleanSupplier conditionSupplier, String timeoutMessage) throws TimeoutException {
        waitForCondition(maxTime, DEFAULT_WAIT_STEP, conditionSupplier, timeoutMessage);
    }

    public static void waitForCondition(Duration maxDuration, Duration waitStep, BooleanSupplier conditionSupplier, String timeoutMessage) throws TimeoutException {
        Instant startTime = Instant.now();
        while (startTime.plus(maxDuration).isAfter(Instant.now()) && !conditionSupplier.getAsBoolean()) {
            wait(waitStep);
        }

        if (!conditionSupplier.getAsBoolean()) {
            throw new TimeoutException(timeoutMessage);
        }
    }
}
