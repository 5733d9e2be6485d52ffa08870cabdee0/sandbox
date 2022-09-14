package com.redhat.service.smartevents.shard.operator;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.exceptions.BridgeErrorHelper;
import com.redhat.service.smartevents.infra.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.shard.operator.metrics.OperatorMetricsService;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ManagerSyncServiceImpl implements ManagerSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSyncServiceImpl.class);

    private enum FallibleOperation {
        PROVISIONING("Provisioning", MetricsOperation.RESOURCE_PROVISION),
        DELETING("Deleting", MetricsOperation.RESOURCE_DELETE);

        String prettyName;
        MetricsOperation metricsOperation;

        FallibleOperation(String prettyName, MetricsOperation metricsOperation) {
            this.prettyName = prettyName;
            this.metricsOperation = metricsOperation;
        }

        String getPrettyName() {
            return prettyName;
        }

        public MetricsOperation getMetricsOperation() {
            return metricsOperation;
        }
    }

    @Inject
    ManagerClient managerClient;

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    BridgeExecutorService bridgeExecutorService;

    @Inject
    BridgeErrorHelper bridgeErrorHelper;

    @Inject
    OperatorMetricsService metricsService;

    @Override
    @Scheduled(every = "30s")
    public void syncUpdatesFromManager() {
        LOGGER.info("Fetching updates from Manager for Bridges and Processors to deploy and delete");
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
                        LOGGER.info("Found Bridge '{}' in PREPARING state. Moving to PROVISIONING.", y.getId());
                        ManagedResourceStatusUpdateDTO updateDto = new ManagedResourceStatusUpdateDTO(y.getId(), y.getCustomerId(), ManagedResourceStatus.PROVISIONING);
                        return managerClient.notifyBridgeStatusChange(updateDto)
                                .subscribe().with(
                                        success -> {
                                            LOGGER.info("Provisioning notification for Bridge '{}' has been sent to the manager successfully", y.getId());
                                            createBridgeIngress(y);
                                        },
                                        failure -> failedToSendUpdateToManager(y, failure));
                    }
                    if (y.getStatus().equals(ManagedResourceStatus.PROVISIONING)) { // Bridges that were being provisioned (before the Operator failed).
                        LOGGER.info("Found Bridge '{}' in PROVISIONING state. Attempting to continue with PROVISIONING.", y.getId());
                        createBridgeIngress(y);
                        return Uni.createFrom().voidItem();
                    }
                    if (y.getStatus().equals(ManagedResourceStatus.DEPROVISION)) { // Bridges to delete
                        LOGGER.info("Found Bridge '{}' in DEPROVISION state. Moving to DELETING.", y.getId());
                        ManagedResourceStatusUpdateDTO updateDto = new ManagedResourceStatusUpdateDTO(y.getId(), y.getCustomerId(), ManagedResourceStatus.DELETING);
                        return managerClient.notifyBridgeStatusChange(updateDto)
                                .subscribe().with(
                                        success -> {
                                            LOGGER.info("Deleting notification for Bridge '{}' has been sent to the manager successfully", y.getId());
                                            deleteBridgeIngress(y);
                                        },
                                        failure -> failedToSendUpdateToManager(y, failure));
                    }
                    if (y.getStatus().equals(ManagedResourceStatus.DELETING)) { // Bridges that were being deleted (before the Operator failed).
                        LOGGER.info("Found Bridge '{}' in DELETING state. Attempting to continue with DELETING.", y.getId());
                        deleteBridgeIngress(y);
                        return Uni.createFrom().voidItem();
                    }
                    LOGGER.warn("Manager included a Bridge '{}' instance with an illegal status '{}'", y.getId(), y.getStatus());
                    return Uni.createFrom().voidItem();
                }).collect(Collectors.toList())));
    }

    protected Uni<Object> doProcessors() {
        return managerClient.fetchProcessorsToDeployOrDelete()
                .onItem().transformToUni(x -> Uni.createFrom().item(x.stream().map(y -> {
                    if (ManagedResourceStatus.PREPARING.equals(y.getStatus())) {
                        LOGGER.info("Found Processor '{}' in PREPARING state. Moving to PROVISIONING.", y.getId());
                        ProcessorManagedResourceStatusUpdateDTO updateDto =
                                new ProcessorManagedResourceStatusUpdateDTO(y.getId(), y.getCustomerId(), y.getBridgeId(), ManagedResourceStatus.PROVISIONING);
                        return managerClient.notifyProcessorStatusChange(updateDto)
                                .subscribe().with(
                                        success -> {
                                            LOGGER.info("Provisioning notification for Processor '{}' has been sent to the manager successfully", y.getId());
                                            createBridgeExecutor(y);
                                        },
                                        failure -> failedToSendUpdateToManager(y, failure));
                    }
                    if (y.getStatus().equals(ManagedResourceStatus.PROVISIONING)) { // Processors that were being provisioned (before the Operator failed).
                        LOGGER.info("Found Processor '{}' in PROVISIONING state. Attempting to continue with PROVISIONING.", y.getId());
                        createBridgeExecutor(y);
                        return Uni.createFrom().voidItem();
                    }
                    if (ManagedResourceStatus.DEPROVISION.equals(y.getStatus())) { // Processor to delete
                        LOGGER.info("Found Processor '{}' in DEPROVISION state. Moving to DELETING.", y.getId());
                        ProcessorManagedResourceStatusUpdateDTO updateDto = new ProcessorManagedResourceStatusUpdateDTO(y.getId(), y.getCustomerId(), y.getBridgeId(), ManagedResourceStatus.DELETING);
                        return managerClient.notifyProcessorStatusChange(updateDto)
                                .subscribe().with(
                                        success -> {
                                            LOGGER.info("Deleting notification for Processor '{}' has been sent to the manager successfully", y.getId());
                                            deleteBridgeExecutor(y);
                                        },
                                        failure -> failedToSendUpdateToManager(y, failure));
                    }
                    if (y.getStatus().equals(ManagedResourceStatus.DELETING)) { // Processors that were being deleted (before the Operator failed).
                        LOGGER.info("Found Processor '{}' in DELETING state. Attempting to continue with DELETING.", y.getId());
                        deleteBridgeExecutor(y);
                        return Uni.createFrom().voidItem();
                    }
                    return Uni.createFrom().voidItem();
                }).collect(Collectors.toList())));
    }

    private void createBridgeIngress(BridgeDTO bridge) {
        LOGGER.info("Provisioning Bridge '{}'", bridge.getId());
        doFallibleBridgeOperation(() -> bridgeIngressService.createBridgeIngress(bridge),
                FallibleOperation.PROVISIONING,
                ManagedResourceStatus.FAILED,
                bridge);
    }

    private void deleteBridgeIngress(BridgeDTO bridge) {
        LOGGER.info("Deleting Processor '{}'", bridge.getId());
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
            metricsService.onOperationStart(bridge, operation.getMetricsOperation());
            runnable.run();
            metricsService.onOperationComplete(bridge, operation.getMetricsOperation());
        } catch (Exception e) {
            LOGGER.warn("{} of Bridge '{}' failed", operation.getPrettyName(), bridge.getId(), e);
            ManagedResourceStatusUpdateDTO updateDto = new ManagedResourceStatusUpdateDTO(bridge.getId(),
                    bridge.getCustomerId(),
                    failedStatus,
                    bridgeErrorHelper.getBridgeErrorInstance(e));

            metricsService.onOperationFailed(bridge, operation.getMetricsOperation());
            managerClient.notifyBridgeStatusChange(updateDto)
                    .subscribe()
                    .with(success -> LOGGER.info("Failure notification for Bridge '{}' has been sent to the manager successfully", bridge.getId()),
                            failure -> failedToSendUpdateToManager(bridge, failure));
        }
    }

    private void createBridgeExecutor(ProcessorDTO processor) {
        LOGGER.info("Provisioning Processor '{}'", processor.getId());
        doFallibleProcessorOperation(() -> bridgeExecutorService.createBridgeExecutor(processor),
                FallibleOperation.PROVISIONING,
                ManagedResourceStatus.FAILED,
                processor);
    }

    private void deleteBridgeExecutor(ProcessorDTO processor) {
        LOGGER.info("Deleting Processor '{}'", processor.getId());
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
            metricsService.onOperationStart(processor, operation.getMetricsOperation());
            runnable.run();
            metricsService.onOperationComplete(processor, operation.getMetricsOperation());
        } catch (Exception e) {
            LOGGER.warn("{} of Processor '{}' failed", operation.getPrettyName(), processor.getId(), e);
            ProcessorManagedResourceStatusUpdateDTO updateDto = new ProcessorManagedResourceStatusUpdateDTO(processor.getId(),
                    processor.getCustomerId(),
                    processor.getBridgeId(),
                    failedStatus,
                    bridgeErrorHelper.getBridgeErrorInstance(e));

            metricsService.onOperationFailed(processor, operation.getMetricsOperation());
            managerClient.notifyProcessorStatusChange(updateDto)
                    .subscribe()
                    .with(success -> LOGGER.info("Failure notification for Processor '{}' has been sent to the manager successfully", processor.getId()),
                            failure -> failedToSendUpdateToManager(processor, failure));
        }
    }

    private void failedToSendUpdateToManager(Object entity, Throwable t) {
        LOGGER.error("Failed to send updated status to Manager for entity of type '{}'", entity.getClass().getSimpleName(), t);
    }

    private void processingFailed(Class<?> entity, Throwable t) {
        LOGGER.error("Failure processing entities '{}' to be deployed or deleted", entity.getSimpleName(), t);
    }

    private void processingComplete(Class<?> entity) {
        LOGGER.info("Successfully processed all entities '{}' to deploy or delete", entity.getSimpleName());
    }
}
