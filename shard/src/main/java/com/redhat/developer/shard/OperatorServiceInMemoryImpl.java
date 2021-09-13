package com.redhat.developer.shard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.ingress.IngressService;
import com.redhat.developer.shard.controllers.ProcessorController;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class OperatorServiceInMemoryImpl implements OperatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorServiceInMemoryImpl.class);

    private final List<BridgeDTO> bridges = new ArrayList<>();

    @Inject
    ManagerSyncService managerSyncService;

    //TODO: remove after we move to k8s
    @Inject
    IngressService ingressService;

    @Inject
    ProcessorController processorController;

    @Override
    public BridgeDTO createBridgeDeployment(BridgeDTO bridge) {
        bridges.add(bridge); // TODO: when we move to k8s, replace this with CRD
        LOGGER.info("[shard] Processing deployment of Bridge with id '{}' and name '{}' for customer '{}'",
                bridge.getId(), bridge.getName(), bridge.getCustomerId());
        return bridge;
    }

    @Override
    public BridgeDTO deleteBridgeDeployment(BridgeDTO bridge) {
        Optional<BridgeDTO> optionalBridgeToDelete = bridges.stream().filter(x -> x.getId().equals(bridge.getId())).findFirst(); // TODO: when we move to k8s, replace this with CRD removal
        if (!optionalBridgeToDelete.isPresent()) {
            LOGGER.info("[shard] could not find Bridge '{}' deployment for customer '{}', ignoring.", bridge.getId(), bridge.getCustomerId());
        } else {
            bridges.remove(optionalBridgeToDelete.get());
            ingressService.undeploy(bridge.getName()); // TODO: in k8s we just delete the deployment
            LOGGER.info("[shard] Bridge with id '{}' and name '{}' for customer '{}' has been deleted",
                    bridge.getId(), bridge.getName(), bridge.getCustomerId());
        }

        return bridge;
    }

    // TODO: replace with operator reconcile loop and logic
    @Scheduled(every = "30s")
    void reconcileLoopMock() {
        LOGGER.debug("[shard] Bridge reconcile loop mock wakes up");
        for (BridgeDTO dto : bridges) {
            if (BridgeStatus.PROVISIONING == dto.getStatus()) {
                reconcileBridge(dto);
            } else if (BridgeStatus.AVAILABLE == dto.getStatus()) {
                reconcileBridgeProcessors(dto);
            }
        }
    }

    private void reconcileBridge(BridgeDTO dto) {
        LOGGER.info("[shard] Creating deployment of ingress for Bridge with id '{}'", dto.getId());
        String endpoint = ingressService.deploy(dto.getId()); // TODO: replace with CR creation and fetch endpoint info from CRD
        dto.setStatus(BridgeStatus.AVAILABLE);
        dto.setEndpoint(endpoint);
        managerSyncService.notifyBridgeStatusChange(dto).subscribe().with(
                success -> LOGGER.info("[shard] Updating Bridge with id '{}' done", dto.getId()),
                failure -> LOGGER.warn("[shard] Updating Bridge with id '{}' FAILED", dto.getId()));
    }

    private void reconcileBridgeProcessors(BridgeDTO bridgeDTO) {
        processorController.reconcileProcessorsFor(bridgeDTO);
    }
}
