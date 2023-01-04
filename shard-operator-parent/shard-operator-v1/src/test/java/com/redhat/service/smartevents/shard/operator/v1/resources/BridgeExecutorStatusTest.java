package com.redhat.service.smartevents.shard.operator.v1.resources;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionTypeConstants;

public class BridgeExecutorStatusTest {

    @Test
    public void testInferManagedResourceStatus_ready() {
        BridgeExecutorStatus resourceStatus = new BridgeExecutorStatus();
        resourceStatus.markConditionTrue(ConditionTypeConstants.READY);
        Assertions.assertThat(resourceStatus.inferManagedResourceStatus()).isEqualTo(ManagedResourceStatus.READY);
    }

    @Test
    public void testInferManagedResourceStatus_failed() {
        BridgeExecutorStatus resourceStatus = new BridgeExecutorStatus();
        resourceStatus.markConditionFalse(ConditionTypeConstants.READY);
        Assertions.assertThat(resourceStatus.inferManagedResourceStatus()).isEqualTo(ManagedResourceStatus.FAILED);
    }

    @Test
    public void testInferManagedResourceStatus_provisioning() {
        BridgeExecutorStatus resourceStatus = new BridgeExecutorStatus();
        Assertions.assertThat(resourceStatus.inferManagedResourceStatus()).isEqualTo(ManagedResourceStatus.PROVISIONING);
    }
}
