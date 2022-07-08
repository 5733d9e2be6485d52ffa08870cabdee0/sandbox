package com.redhat.service.smartevents.shard.operator;

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

    private enum FallibleOperation {
        PROVISIONING("Provisioning"),
        DELETING("Deleting");

        String prettyName;

        FallibleOperation(String prettyName) {
            this.prettyName = prettyName;
        }

        String getPrettyName() {
            return prettyName;
        }

    }

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
        doBridges().subscribe().with(
                success -> processingComplete(BridgeDTO.class),
                failure -> processingFailed(BridgeDTO.class, failure));

        doProcessors().subscribe().with(
                success -> processingComplete(ProcessorDTO.class),
                failure -> processingFailed(ProcessorDTO.class, failure));
    }

    protected Uni<Object> doBridges() {
        return managerClient.fetchBridgesToDeployOrDelete()
                .onItem().transformToUni(x -> Uni.createFrom().item(x.stream().map(y -> {
                    if (y.getStatus().equals(ManagedResourceStatus.PREPARING)) { // Bridges to deploy
                        y.setStatus(ManagedResourceStatus.PROVISIONING);
                        return managerClient.notifyBridgeStatusChange(y)
                                .subscribe().with(
                                        success -> {
                                            LOGGER.debug("Provisioning notification for Bridge '{}' has been sent to the manager successfully", y.getId());
                                            createBridgeIngress(y);
                                        },
                                        failure -> failedToSendUpdateToManager(y, failure));
                    }
                    if (y.getStatus().equals(ManagedResourceStatus.DEPROVISION)) { // Bridges to delete
                        y.setStatus(ManagedResourceStatus.DELETING);
                        return managerClient.notifyBridgeStatusChange(y)
                                .subscribe().with(
                                        success -> {
                                            LOGGER.debug("Deleting notification for Bridge '{}' has been sent to the manager successfully", y.getId());
                                            deleteBridgeIngress(y);
                                        },
                                        failure -> failedToSendUpdateToManager(y, failure));
                    }
                    if (y.getStatus().equals(ManagedResourceStatus.PROVISIONING)) { // Bridges that were being provisioned (before the Operator failed).
                        return Uni.createFrom().voidItem().invoke(() -> createBridgeIngress(y));
                    }
                    if (y.getStatus().equals(ManagedResourceStatus.DELETING)) { // Bridges that were being deleted (before the Operator failed).
                        return Uni.createFrom().voidItem().invoke(() -> deleteBridgeIngress(y));
                    }
                    LOGGER.warn("Manager included a Bridge '{}' instance with an illegal status '{}'", y.getId(), y.getStatus());
                    return Uni.createFrom().voidItem();
                }).collect(Collectors.toList())));
    }

    protected Uni<Object> doProcessors() {
        return managerClient.fetchProcessorsToDeployOrDelete()
                .onItem().transformToUni(x -> Uni.createFrom().item(x.stream().map(y -> {
                    if (ManagedResourceStatus.PREPARING.equals(y.getStatus())) {
                        y.setStatus(ManagedResourceStatus.PROVISIONING);
                        return managerClient.notifyProcessorStatusChange(y)
                                .subscribe().with(
                                        success -> {
                                            LOGGER.debug("Provisioning notification for Processor '{}' has been sent to the manager successfully", y.getId());
                                            createBridgeExecutor(y);
                                        },
                                        failure -> failedToSendUpdateToManager(y, failure));
                    }
                    if (ManagedResourceStatus.DEPROVISION.equals(y.getStatus())) { // Processor to delete
                        y.setStatus(ManagedResourceStatus.DELETING);
                        return managerClient.notifyProcessorStatusChange(y)
                                .subscribe().with(
                                        success -> {
                                            LOGGER.debug("Deleting notification for Processor '{}' has been sent to the manager successfully", y.getId());
                                            deleteBridgeExecutor(y);
                                        },
                                        failure -> failedToSendUpdateToManager(y, failure));
                    }
                    if (y.getStatus().equals(ManagedResourceStatus.PROVISIONING)) { // Processors that were being provisioned (before the Operator failed).
                        return Uni.createFrom().voidItem().invoke(() -> createBridgeExecutor(y));
                    }
                    if (y.getStatus().equals(ManagedResourceStatus.DELETING)) { // Processors that were being deleted (before the Operator failed).
                        return Uni.createFrom().voidItem().invoke(() -> deleteBridgeExecutor(y));
                    }
                    return Uni.createFrom().voidItem();
                }).collect(Collectors.toList())));
    }

    private void createBridgeIngress(BridgeDTO bridge) {
        LOGGER.debug("Provisioning Bridge '{}'", bridge.getId());
        doFallibleBridgeOperation(() -> bridgeIngressService.createBridgeIngress(bridge),
                FallibleOperation.PROVISIONING,
                ManagedResourceStatus.FAILED,
                bridge);
    }

    private void deleteBridgeIngress(BridgeDTO bridge) {
        LOGGER.debug("Deleting Processor '{}'", bridge.getId());
        doFallibleBridgeOperation(() -> bridgeIngressService.deleteBridgeIngress(bridge),
                FallibleOperation.DELETING,
                ManagedResourceStatus.DELETED,
                bridge);
    }

    private void doFallibleBridgeOperation(Runnable runnable,
            FallibleOperation operation,
            ManagedResourceStatus failedStatus,
            BridgeDTO bridge) {
        try {
            runnable.run();
        } catch (Exception e) {
            bridge.setStatus(failedStatus);
            managerClient.notifyBridgeStatusChange(bridge)
                    .subscribe()
                    .with(success -> LOGGER.debug(String.format("%s of Bridge '%s' failed", operation.getPrettyName(), bridge.getId()), e),
                            failure -> failedToSendUpdateToManager(bridge, e));
        }
    }

    private void createBridgeExecutor(ProcessorDTO processor) {
        LOGGER.debug("Provisioning Processor '{}'", processor.getId());
        doFallibleProcessorOperation(() -> bridgeExecutorService.createBridgeExecutor(processor),
                FallibleOperation.PROVISIONING,
                ManagedResourceStatus.FAILED,
                processor);
    }

    private void deleteBridgeExecutor(ProcessorDTO processor) {
        LOGGER.debug("Deleting Processor '{}'", processor.getId());
        doFallibleProcessorOperation(() -> bridgeExecutorService.deleteBridgeExecutor(processor),
                FallibleOperation.DELETING,
                ManagedResourceStatus.DELETED,
                processor);
    }

    private void doFallibleProcessorOperation(Runnable runnable,
            FallibleOperation operation,
            ManagedResourceStatus failedStatus,
            ProcessorDTO processor) {
        try {
            runnable.run();
        } catch (Exception e) {
            processor.setStatus(failedStatus);
            managerClient.notifyProcessorStatusChange(processor)
                    .subscribe()
                    .with(success -> LOGGER.debug(String.format("%s of Processor '%s' failed", operation.getPrettyName(), processor.getId()), e),
                            failure -> failedToSendUpdateToManager(processor, e));
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
