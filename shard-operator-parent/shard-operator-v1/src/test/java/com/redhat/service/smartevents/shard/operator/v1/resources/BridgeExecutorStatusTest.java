package com.redhat.service.smartevents.shard.operator.v1.resources;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionTypeConstants;

public class BridgeExecutorStatusTest {

    @Test
    public void testInferManagedResourceStatus_ready() {
        BridgeExecutorStatus resourceStatus = new BridgeExecutorStatus();
        resourceStatus.markConditionTrue(ConditionTypeConstants.READY);
        Assertions.assertThat(resourceStatus.inferManagedResourceStatus()).isEqualTo(ManagedResourceStatusV1.READY);
    }

    @Test
    public void testInferManagedResourceStatus_failed() {
        BridgeExecutorStatus resourceStatus = new BridgeExecutorStatus();
        resourceStatus.markConditionFalse(ConditionTypeConstants.READY);
        Assertions.assertThat(resourceStatus.inferManagedResourceStatus()).isEqualTo(ManagedResourceStatusV1.FAILED);
    }

    @Test
    public void testInferManagedResourceStatus_provisioning() {
        BridgeExecutorStatus resourceStatus = new BridgeExecutorStatus();
        Assertions.assertThat(resourceStatus.inferManagedResourceStatus()).isEqualTo(ManagedResourceStatusV1.PROVISIONING);
    }
}
