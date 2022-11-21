package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public interface ReconciliationResultService {

    UpdateControl<BridgeIngress> getReconciliationResultFor(BridgeIngress bridgeIngress, RuntimeException e);
    UpdateControl<BridgeIngress> getReconciliationResult(BridgeIngress updatedBridgeIngress);
}
