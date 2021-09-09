package com.redhat.developer.shard.processors;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.developer.executor.ExecutorsService;
import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.infra.dto.ProcessorDTO;
import com.redhat.developer.shard.ManagerSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ProcessorController {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessorController.class);

    private List<ProcessorDTO> processors = new ArrayList<>();

    @Inject
    ManagerSyncService managerSyncService;

    @Inject
    ExecutorsService executorsService;

    public void reconcileProcessorsFor(BridgeDTO bridgeDTO) {
        managerSyncService.fetchProcessorsForBridge(bridgeDTO)
                .subscribe()
                .with(item -> reconcileProcessor(item),
                      failure -> LOG.error("Failed to retrieve list of Processors from Manager for Bridge '{}'", bridgeDTO.getId()),
                      () -> LOG.info("Reconciled all Processors for bridge '{}' for customer '{}'.", bridgeDTO.getId(), bridgeDTO.getCustomerId())
                );
    }

    private void failedToSendUpdateToManager(ProcessorDTO dto, Throwable error) {
        LOG.error("Failed to send status update to Manager for Processor '{}' on Bridge '{}'", dto.getId(), dto.getBridge().getId(), error);
    }

    private void createExecutor(ProcessorDTO processorDTO) {
        try {
            executorsService.createExecutor(processorDTO);
            processorDTO.setStatus(BridgeStatus.AVAILABLE);
        } catch (Throwable t) {
            LOG.error("Failed to reconcile Executor for Processor '{}' for customer '{}' on Bridge '{}'", processorDTO.getId(), processorDTO.getBridge().getCustomerId(), processorDTO.getBridge().getId(), t);
            processorDTO.setStatus(BridgeStatus.FAILED);
        }
        managerSyncService.notifyProcessorStatusChange(processorDTO).subscribe()
                .with(success -> LOG.info("Reconciled Executor for Processor '{}' on Bridge '{}'. Final status: '{}'", processorDTO.getId(), processorDTO.getBridge().getId(), processorDTO.getStatus()),
                      failure -> failedToSendUpdateToManager(processorDTO, failure));
    }

    private void reconcileProcessor(ProcessorDTO processorDTO) {

        if (processorDTO.getStatus() == BridgeStatus.REQUESTED) {

            processorDTO.setStatus(BridgeStatus.PROVISIONING);
            this.processors.add(processorDTO);
            managerSyncService.notifyProcessorStatusChange(processorDTO)
                    .subscribe()
                    .with(item -> createExecutor(processorDTO),
                          failure -> failedToSendUpdateToManager(processorDTO, failure));
        } else if (processorDTO.getStatus() == BridgeStatus.PROVISIONING) {
            /*
                 If we're still provisioning and a new reconcile loop starts, try to provision again. This operation is
                 idempotent
             */
            createExecutor(processorDTO);
        }
    }
}
