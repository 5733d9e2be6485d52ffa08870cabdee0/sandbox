package com.redhat.developer.shard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.infra.dto.ConnectorDTO;
import com.redhat.developer.infra.dto.ConnectorStatus;
import com.redhat.developer.ingress.IngressService;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class OperatorServiceInMemoryImpl implements OperatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorServiceInMemoryImpl.class);

    private final List<ConnectorDTO> connectors = new ArrayList<>();

    @Inject
    ManagerSyncService managerSyncService;

    //TODO: remove after we move to k8s
    @Inject
    IngressService ingressService;

    @Override
    public ConnectorDTO createConnectorDeployment(ConnectorDTO connector) {
        connectors.add(connector); // TODO: when we move to k8s, replace this with CRD
        LOGGER.info("[shard] Processing deployment of Connector with id '{}' and name '{}' for customer '{}'",
                connector.getId(), connector.getName(), connector.getCustomerId());
        return connector;
    }

    // TODO: replace with operator reconcile loop and logic
    @Scheduled(every = "30s")
    void reconcileLoopMock() {
        LOGGER.debug("[shard] Connector reconcile loop mock wakes up");
        for (ConnectorDTO dto : connectors.stream().filter(x -> x.getStatus().equals(ConnectorStatus.PROVISIONING)).collect(Collectors.toList())) {
            LOGGER.info("[shard] Updating connector with id '{}'", dto.getId());
            String endpoint = ingressService.deploy(dto.getName()); // TODO: replace with CR creation and fetch endpoint info from CRD
            dto.setStatus(ConnectorStatus.AVAILABLE);
            dto.setEndpoint(endpoint);
            managerSyncService.notifyConnectorStatusChange(dto).subscribe().with(
                    success -> LOGGER.info("[shard] Updating connector with id '{}' done", dto.getId()),
                    failure -> LOGGER.warn("[shard] Updating connector with id '{}' FAILED", dto.getId()));
        }
    }
}
