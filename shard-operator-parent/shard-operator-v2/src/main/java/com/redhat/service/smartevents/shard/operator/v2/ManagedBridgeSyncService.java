package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;

@ApplicationScoped
public class ManagedBridgeSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedBridgeSyncService.class);

    @Inject
    ManagerClient managerClient;

    @Inject
    ManagedBridgeService managedBridgeService;

    @Inject
    NamespaceProvider namespaceProvider;

    public void syncManagedBridgeWithManager() {
        managerClient.fetchBridgesToDeployOrDelete()
                .onItem()
                .invoke(this::processDelta)
                .subscribe().with(
                        success -> LOGGER.debug("Successfully processed ManagedBridge deltas"),
                        error -> LOGGER.error("Failed to process ManagedBridge deltas", error));
    }

    private void processDelta(List<BridgeDTO> bridgeDTOList) {
        for (BridgeDTO bridgeDTO : bridgeDTOList) {
            if (bridgeDTO.getOperationType() == OperationType.DELETE) {
                managedBridgeService.deleteManagedBridge(bridgeDTO);
            } else {
                managedBridgeService.createManagedBridge(bridgeDTO);
            }
        }
    }
}
