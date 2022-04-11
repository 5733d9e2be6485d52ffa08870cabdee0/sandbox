package com.redhat.service.smartevents.shard.operator;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ManagerSyncServiceImpl implements ManagerSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSyncServiceImpl.class);

    private final BridgeHandler BRIDGE_HANDLER = new BridgeHandler();

    private final ProcessorHandler PROCESSOR_HANDLER = new ProcessorHandler();

    @Inject
    ManagerClient managerClient;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    BridgeExecutorService bridgeExecutorService;

    @Override
    @Scheduled(every = "30s")
    public void syncUpdatesFromManager() {
        LOGGER.debug("Fetching updates from Manager for Bridges and Processors to deploy and delete");
        doBridgeActions().subscribe().with(
                success -> processingComplete(BridgeDTO.class),
                failure -> processingFailed(BridgeDTO.class, failure));

        doProcessorActions().subscribe().with(
                success -> processingComplete(ProcessorDTO.class),
                failure -> processingFailed(ProcessorDTO.class, failure));
    }

    protected Uni<Object> doBridgeActions() {
        return managerClient.fetchAndProcessBridgesToDeployOrDelete(BRIDGE_HANDLER);
    }

    protected Uni<Object> doProcessorActions() {
        return managerClient.fetchAndProcessProcessorsToDeployOrDelete(PROCESSOR_HANDLER);
    }

    protected class BridgeHandler implements Function<List<BridgeDTO>, Uni<Object>> {
        @Override
        public Uni<Object> apply(List<BridgeDTO> bridges) {
            return Uni.createFrom().item(
                    bridges.stream()
                            .map(y -> {
                                if (y.getStatus().equals(ManagedResourceStatus.ACCEPTED)) { // Bridges to deploy
                                    y.setStatus(ManagedResourceStatus.PROVISIONING);
                                    return managerClient.notifyBridgeStatusChange(y)
                                            .subscribe().with(
                                                    success -> {
                                                        LOGGER.debug("Provisioning notification for Bridge '{}' has been sent to the manager successfully", y.getId());
                                                        bridgeIngressService.createBridgeIngress(y);
                                                    },
                                                    failure -> failedToSendUpdateToManager(y, failure));
                                }
                                if (y.getStatus().equals(ManagedResourceStatus.DEPROVISION)) { // Bridges to delete
                                    y.setStatus(ManagedResourceStatus.DELETING);
                                    return managerClient.notifyBridgeStatusChange(y)
                                            .subscribe().with(
                                                    success -> {
                                                        LOGGER.debug("Deleting notification for Bridge '{}' has been sent to the manager successfully", y.getId());
                                                        bridgeIngressService.deleteBridgeIngress(y);
                                                    },
                                                    failure -> failedToSendUpdateToManager(y, failure));
                                }
                                LOGGER.warn("Manager included a Bridge '{}' instance with an illegal status '{}'", y.getId(), y.getStatus());
                                return Uni.createFrom().voidItem();
                            }).collect(Collectors.toList()));
        }
    }

    protected class ProcessorHandler implements Function<List<ProcessorDTO>, Uni<Object>> {

        @Override
        public Uni<Object> apply(List<ProcessorDTO> processors) {
            return Uni.createFrom().item(processors.stream()
                    .map(y -> {
                        if (ManagedResourceStatus.ACCEPTED.equals(y.getStatus())) {
                            y.setStatus(ManagedResourceStatus.PROVISIONING);
                            return managerClient.notifyProcessorStatusChange(y)
                                    .subscribe().with(
                                            success -> {
                                                LOGGER.debug("Provisioning notification for Processor '{}' has been sent to the manager successfully", y.getId());
                                                bridgeExecutorService.createBridgeExecutor(y);
                                            },
                                            failure -> failedToSendUpdateToManager(y, failure));
                        }
                        if (ManagedResourceStatus.DEPROVISION.equals(y.getStatus())) { // Processor to delete
                            y.setStatus(ManagedResourceStatus.DELETING);
                            return managerClient.notifyProcessorStatusChange(y)
                                    .subscribe().with(
                                            success -> {
                                                LOGGER.debug("Deleting notification for Processor '{}' has been sent to the manager successfully", y.getId());
                                                bridgeExecutorService.deleteBridgeExecutor(y);
                                            },
                                            failure -> failedToSendUpdateToManager(y, failure));
                        }
                        return Uni.createFrom().voidItem();
                    }).collect(Collectors.toList()));
        }
    }

    private void failedToSendUpdateToManager(Object entity, Throwable t) {
        LOGGER.error("Failed to send updated status to Manager for entity of type '{}'", entity.getClass().getSimpleName(), t);
    }

    private void processingFailed(Class<?> entity, Throwable t) {
        LOGGER.error("Failure processing entities '{}' to be deployed or deleted", entity.getSimpleName(), t);
    }

    private void processingComplete(Class<?> entity) {
        LOGGER.debug("Successfully processed all entities '{}' to deploy or delete", entity.getSimpleName());
    }

}
