package com.redhat.service.smartevents.shard.operator.exceptions;

public class ReconcilationFailedException extends RuntimeException {

    private String conditionType;

    public ReconcilationFailedException(String conditionType, Throwable cause) {
        super(cause);
        this.conditionType = conditionType;
    }

    public String getConditionType(){
        return conditionType;
    }
}
