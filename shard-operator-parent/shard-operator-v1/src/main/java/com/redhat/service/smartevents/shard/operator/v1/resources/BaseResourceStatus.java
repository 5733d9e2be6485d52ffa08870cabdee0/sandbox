package com.redhat.service.smartevents.shard.operator.v1.resources;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeError;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.CustomResourceStatus;

abstract class BaseResourceStatus extends CustomResourceStatus {

    public BaseResourceStatus(Set<Condition> initialConditions) {
        super(initialConditions);
    }

    @JsonIgnore
    public final boolean isReady() {
        return getConditions().stream().anyMatch(c -> ConditionTypeConstants.READY.equals(c.getType()) && ConditionStatus.True.equals(c.getStatus()));
    }

    @JsonIgnore
    public final boolean isAugmentingTrueOrUnknown() {
        return getConditions().stream()
                .anyMatch(c -> ConditionTypeConstants.AUGMENTING.equals(c.getType()) && (ConditionStatus.True.equals(c.getStatus()) || ConditionStatus.Unknown.equals(c.getStatus())));
    }

    @JsonIgnore
    public final void setStatusFromBridgeError(BridgeError bridgeError) {
        markConditionFalse(ConditionTypeConstants.READY,
                bridgeError.getReason(),
                bridgeError.getReason(),
                bridgeError.getCode());
    }

}
