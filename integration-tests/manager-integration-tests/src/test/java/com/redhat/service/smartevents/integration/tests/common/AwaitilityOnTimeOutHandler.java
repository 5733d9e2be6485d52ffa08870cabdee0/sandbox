package com.redhat.service.smartevents.integration.tests.common;

import org.awaitility.core.ConditionEvaluationListener;
import org.awaitility.core.EvaluatedCondition;
import org.awaitility.core.TimeoutEvent;

public class AwaitilityOnTimeOutHandler implements ConditionEvaluationListener {
    private final Runnable runnable;

    public AwaitilityOnTimeOutHandler(Runnable runnable) {
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


