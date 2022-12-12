package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;

@ApplicationScoped
public class ManagedProcessorSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedProcessorSyncService.class);

    @Inject
    ManagerClient managerClient;

    @Inject
    ManagedProcessorService managedProcessorService;

    public void syncManagedProcessorWithManager() {
        managerClient.fetchProcessorsToDeployOrDelete()
                .onItem()
                .invoke(this::processDelta)
                .subscribe().with(
                        success -> LOGGER.debug("Successfully processed ManagedProcessor deltas"),
                        failed -> LOGGER.debug("Failed to process ManagedProcessor deltas"));
    }

    private void processDelta(List<ProcessorDTO> processorDTOList) {
        for (ProcessorDTO processorDTO : processorDTOList) {
            if (processorDTO.getOperationType() == OperationType.DELETE) {
                managedProcessorService.deleteManagedProcessor(processorDTO);
            } else {
                managedProcessorService.createManagedProcessor(processorDTO, "namespace");
            }
        }
    }
}
