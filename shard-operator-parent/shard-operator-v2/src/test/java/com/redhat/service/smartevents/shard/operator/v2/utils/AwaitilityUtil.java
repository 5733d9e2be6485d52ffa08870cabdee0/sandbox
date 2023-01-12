package com.redhat.service.smartevents.shard.operator.v2.utils;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;

public class AwaitilityUtil {

    private AwaitilityUtil() {
        //Static utility methods
    }

    public static void await(Duration timeout, Duration pollInterval, ThrowingRunnable assertion) {
        Awaitility.await()
                .atMost(timeout)
                .pollInterval(pollInterval)
                .untilAsserted(assertion);
    }

}
