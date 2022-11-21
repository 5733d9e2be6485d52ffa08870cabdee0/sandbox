package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.CustomResourceStatus;

public interface StatusService {

    void updateStatusForSuccessfulReconciliation(CustomResourceStatus customResourceStatus, String conditionType);

    void updateStatusForFailedReconciliation(CustomResourceStatus customResourceStatus, String conditionType, RuntimeException e);
}
