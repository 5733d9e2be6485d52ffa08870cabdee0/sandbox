package com.redhat.service.smartevents.manager.v2.utils;

import java.util.List;
import java.util.Objects;

import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;

public class ConditionUtilities {

    private ConditionUtilities() {
        //Static utility functions class
    }

    public static boolean isOperationComplete(List<Condition> conditions) {
        if (Objects.isNull(conditions) || conditions.isEmpty()) {
            return true;
        }
        return conditions
                .stream()
                .allMatch(c -> c.getStatus() == ConditionStatus.TRUE);
    }

    public static boolean isOperationFailed(List<Condition> conditions) {
        if (Objects.isNull(conditions) || conditions.isEmpty()) {
            return false;
        }
        return conditions
                .stream()
                .anyMatch(c -> c.getStatus() == ConditionStatus.FAILED || Objects.nonNull(c.getErrorCode()));
    }

}
