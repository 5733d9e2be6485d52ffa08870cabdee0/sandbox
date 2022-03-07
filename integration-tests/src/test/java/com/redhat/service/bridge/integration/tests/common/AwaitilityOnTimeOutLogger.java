package com.redhat.service.bridge.integration.tests.common;

import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.TimeoutEvent;

public class AwaitilityOnTimeOutLogger implements ConditionEvaluationListener {
    private final Runnable runnable;

    public AwaitilityOnTimeOutLogger(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void conditionEvaluated(EvaluatedCondition condition) {
        // nothing to do
    }

    @Override
    public void onTimeout(TimeoutEvent timeoutEvent) {
        runnable.run();
    }
}
