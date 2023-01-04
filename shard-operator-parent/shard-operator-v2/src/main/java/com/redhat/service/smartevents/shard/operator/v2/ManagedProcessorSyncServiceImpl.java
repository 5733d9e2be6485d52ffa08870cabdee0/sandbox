package com.redhat.service.smartevents.shard.operator.v2;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.shard.operator.v2.converters.ResourceStatusConverter;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_PROCESSOR_DELETED_NAME;

@ApplicationScoped
public class ManagedProcessorSyncServiceImpl implements ManagedProcessorSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedProcessorSyncServiceImpl.class);

    @Inject
    ManagerClient managerClient;

    @Inject
    ManagedProcessorService managedProcessorService;

    @Override
    public void syncManagedProcessorWithManager() {
        managerClient.fetchProcessorsForDataPlane()
                .onItem()
                .invoke(this::processDelta)
                .subscribe().with(
                        success -> LOGGER.debug("Successfully processed ManagedProcessor deltas"),
                        error -> LOGGER.error("Failed to process ManagedProcessor deltas"));
    }

    @Override
    public void syncManagedProcessorStatusBackToManager() {
        managerClient.fetchProcessorsForDataPlane()
                .onItem()
                .transform(this::transformToProcessorStatus)
                .invoke(this::notifyProcessorStatus)
                .subscribe().with(
                        success -> LOGGER.debug("Successfully sync ManagedProcessor status with Manager"),
                        error -> LOGGER.error("Failed to sync ManagedProcessor status with Manager", error));
    }

    private void processDelta(List<ProcessorDTO> processorDTOList) {
        for (ProcessorDTO processorDTO : processorDTOList) {
            if (processorDTO.getOperationType() == OperationType.DELETE) {
                managedProcessorService.deleteManagedProcessor(processorDTO);
            } else {
                managedProcessorService.createManagedProcessor(processorDTO);
            }
        }
    }

    private List<ResourceStatusDTO> transformToProcessorStatus(List<ProcessorDTO> processorDTOList) {
        Map<String, ManagedProcessor> deployedManagedProcessors = managedProcessorService.fetchAllManagedProcessors()
                .stream().collect(Collectors.toMap(m -> m.getSpec().getId(), m -> m));

        List<ResourceStatusDTO> resourceStatusDTOs = new ArrayList<>(processorDTOList.size());
        for (ProcessorDTO processorDTO : processorDTOList) {
            ManagedProcessor deployedManagedProcessor = deployedManagedProcessors.get(processorDTO.getId());
            if (deployedManagedProcessor != null) {
                resourceStatusDTOs.add(ResourceStatusConverter.fromManagedProcessorToResourceStatusDTO(deployedManagedProcessor));
            } else {
                if (processorDTO.getOperationType() == OperationType.DELETE) {
                    resourceStatusDTOs.add(getDeletedProcessorStatus(processorDTO));
                }
            }
        }
        return resourceStatusDTOs;
    }

    private ResourceStatusDTO getDeletedProcessorStatus(ProcessorDTO processorDTO) {
        List<ConditionDTO> conditions = List.of(new ConditionDTO(DP_PROCESSOR_DELETED_NAME, ConditionStatus.TRUE, ZonedDateTime.now(ZoneOffset.UTC)));
        return new ResourceStatusDTO(processorDTO.getId(), processorDTO.getGeneration(), conditions);
    }

    private void notifyProcessorStatus(List<ResourceStatusDTO> resourceStatusDTOs) {
        managerClient.notifyProcessorStatus(resourceStatusDTOs).subscribe().with(
                success -> LOGGER.debug("Successfully sends ManagedProcessor status"),
                error -> LOGGER.error("Failed to send ManagedProcessor status", error));
    }
}
