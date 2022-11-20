package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.CustomResourceStatus;

public interface StatusService {

    void updateStatus(CustomResourceStatus customResourceStatus, String conditionType, RuntimeException e);
}
