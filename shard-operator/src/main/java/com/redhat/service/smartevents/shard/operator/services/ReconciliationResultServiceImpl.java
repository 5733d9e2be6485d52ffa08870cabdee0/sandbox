package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.BridgeExecutorService;
import com.redhat.service.smartevents.shard.operator.BridgeIngressService;
import com.redhat.service.smartevents.shard.operator.exceptions.ReconciliationException;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ReconciliationResultServiceImpl implements ReconciliationResultService {

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    BridgeExecutorService bridgeExecutorService;

    @Override
    public UpdateControl<BridgeIngress> getReconciliationResultFor(BridgeIngress bridgeIngress, RuntimeException e) {
        if (e instanceof ReconciliationException) {
            ReconciliationException reconciliationException = (ReconciliationException) e;
            return UpdateControl.updateStatus(bridgeIngress).rescheduleAfter(reconciliationException.getReconciliationInterval());
        } else {
            return UpdateControl.updateStatus(bridgeIngress);
        }
    }

    @Override
    public UpdateControl<BridgeIngress> getReconciliationResult(BridgeIngress updatedBridgeIngress) {
        BridgeIngress originalBridgeIngress = bridgeIngressService.fetchBridgeIngress(updatedBridgeIngress.getMetadata().getName(), updatedBridgeIngress.getMetadata().getNamespace());

        if (updatedBridgeIngress.getStatus().equals(originalBridgeIngress.getStatus())) {
            return UpdateControl.noUpdate();
        } else {
            return UpdateControl.updateStatus(updatedBridgeIngress);
        }
    }

    @Override
    public UpdateControl<BridgeExecutor> getReconciliationResultFor(BridgeExecutor bridgeExecutor, RuntimeException e) {
        if (e instanceof ReconciliationException) {
            ReconciliationException reconciliationException = (ReconciliationException) e;
            return UpdateControl.updateStatus(bridgeExecutor).rescheduleAfter(reconciliationException.getReconciliationInterval());
        } else {
            return UpdateControl.updateStatus(bridgeExecutor);
        }
    }

    @Override
    public UpdateControl<BridgeExecutor> getReconciliationResult(BridgeExecutor updatedBridgeExecutor) {
        BridgeExecutor originalBridgeExecutor = bridgeExecutorService.fetchBridgeExecutor(updatedBridgeExecutor.getMetadata().getName(), updatedBridgeExecutor.getMetadata().getNamespace());

        if (updatedBridgeExecutor.getStatus().equals(originalBridgeExecutor.getStatus())) {
            return UpdateControl.noUpdate();
        } else {
            return UpdateControl.updateStatus(updatedBridgeExecutor);
        }
    }
}
