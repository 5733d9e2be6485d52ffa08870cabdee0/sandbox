package com.redhat.service.smartevents.shard.operator.exceptions;

public class ReconciliationException extends RuntimeException {

    private final long reconciliationInterval;

    public ReconciliationException(long reconciliationInterval, Throwable cause) {
        super(cause);
        this.reconciliationInterval = reconciliationInterval;
    }

    public long getReconciliationInterval() {
        return reconciliationInterval;
    }
}
