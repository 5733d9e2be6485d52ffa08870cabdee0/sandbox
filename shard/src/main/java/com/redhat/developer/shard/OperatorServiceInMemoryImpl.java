package com.redhat.developer.shard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.ingress.IngressService;

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

    @Override
    public BridgeDTO createBridgeDeployment(BridgeDTO bridge) {
        bridges.add(bridge); // TODO: when we move to k8s, replace this with CRD
        LOGGER.info("[shard] Processing deployment of Bridge with id '{}' and name '{}' for customer '{}'",
                bridge.getId(), bridge.getName(), bridge.getCustomerId());
        return bridge;
    }

    // TODO: replace with operator reconcile loop and logic
    @Scheduled(every = "30s")
    void reconcileLoopMock() {
        LOGGER.debug("[shard] Bridge reconcile loop mock wakes up");
        for (BridgeDTO dto : bridges.stream().filter(x -> x.getStatus().equals(BridgeStatus.PROVISIONING)).collect(Collectors.toList())) {
            LOGGER.info("[shard] Updating Bridge with id '{}'", dto.getId());
            String endpoint = ingressService.deploy(dto.getName()); // TODO: replace with CR creation and fetch endpoint info from CRD
            dto.setStatus(BridgeStatus.AVAILABLE);
            dto.setEndpoint(endpoint);
            managerSyncService.notifyBridgeStatusChange(dto).subscribe().with(
                    success -> LOGGER.info("[shard] Updating Bridge with id '{}' done", dto.getId()),
                    failure -> LOGGER.warn("[shard] Updating Bridge with id '{}' FAILED", dto.getId()));
        }
    }
}
