package com.redhat.service.bridge.shard.controllers;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.executor.ExecutorsService;
import com.redhat.service.bridge.infra.dto.BridgeStatus;
import com.redhat.service.bridge.infra.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.ManagerSyncService;

@ApplicationScoped
public class ProcessorController {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessorController.class);

    //TODO - In memory list of Processors to reconcile. As/when we move to K8S, this will be represented by CRDs
    private List<ProcessorDTO> processors = new ArrayList<>();

    @Inject
    ManagerSyncService managerSyncService;

    @Inject
    ExecutorsService executorsService;

    public ProcessorDTO deployProcessor(ProcessorDTO processorDTO) {
        this.processors.add(processorDTO);
        return processorDTO;
    }

    public void reconcileProcessors() {
        this.processors.forEach(this::reconcileProcessor);
    }

    private void failedToSendUpdateToManager(ProcessorDTO dto, Throwable error) {
        LOG.error("Failed to send status update to Manager for Processor '{}' on Bridge '{}'", dto.getId(), dto.getBridge().getId(), error);
    }

    private void createExecutor(ProcessorDTO processorDTO) {

        try {
            executorsService.createExecutor(processorDTO);
            processorDTO.setStatus(BridgeStatus.AVAILABLE);
        } catch (Throwable t) {
            LOG.error("Failed to reconcile Executor for Processor '{}' for customer '{}' on Bridge '{}'", processorDTO.getId(), processorDTO.getBridge().getCustomerId(),
                    processorDTO.getBridge().getId(), t);
            processorDTO.setStatus(BridgeStatus.FAILED);
        }
        managerSyncService.notifyProcessorStatusChange(processorDTO).subscribe()
                .with(success -> LOG.info("Completed reconcile of Executor for Processor '{}' on Bridge '{}'. Final status: '{}'", processorDTO.getId(), processorDTO.getBridge().getId(),
                        processorDTO.getStatus()),
                        failure -> failedToSendUpdateToManager(processorDTO, failure));
    }

    private void reconcileProcessor(ProcessorDTO processorDTO) {
        if (processorDTO.getStatus() == BridgeStatus.PROVISIONING) {
            createExecutor(processorDTO);
        }
    }
}
