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
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.shard.operator.v2.converters.ResourceStatusConverter;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_BRIDGE_DELETED_NAME;

@ApplicationScoped
public class ManagedBridgeSyncServiceImpl implements ManagedBridgeSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedBridgeSyncServiceImpl.class);

    @Inject
    ManagerClient managerClient;

    @Inject
    ManagedBridgeService managedBridgeService;

    @Override
    public void syncManagedBridgeWithManager() {
        managerClient.fetchBridgesForDataPlane()
                .onItem()
                .invoke(this::processDelta)
                .subscribe().with(
                        success -> LOGGER.debug("Successfully processed ManagedBridge deltas"),
                        error -> LOGGER.error("Failed to process ManagedBridge deltas", error));
    }

    @Override
    public void syncManagedBridgeStatusBackToManager() {
        managerClient.fetchBridgesForDataPlane()
                .onItem()
                .transform(this::transformToBridgeStatus)
                .invoke(this::notifyBridgeStatus)
                .subscribe().with(
                        success -> LOGGER.debug("Successfully sync ManagedBridge status with Manager"),
                        error -> LOGGER.error("Failed to sync ManagedBridge status with Manager", error));
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

    private List<ResourceStatusDTO> transformToBridgeStatus(List<BridgeDTO> bridgeDTOList) {
        Map<String, ManagedBridge> deployedManagedBridges = managedBridgeService.fetchAllManagedBridges()
                .stream().collect(Collectors.toMap(m -> m.getSpec().getId(), m -> m));

        List<ResourceStatusDTO> resourceStatusDTOs = new ArrayList<>(bridgeDTOList.size());
        for (BridgeDTO bridgeDTO : bridgeDTOList) {
            ManagedBridge deployedManagedBridge = deployedManagedBridges.get(bridgeDTO.getId());
            if (deployedManagedBridge != null) {
                resourceStatusDTOs.add(ResourceStatusConverter.fromManagedBridgeToResourceStatusDTO(deployedManagedBridge));
            } else {
                if (bridgeDTO.getOperationType() == OperationType.DELETE) {
                    resourceStatusDTOs.add(getDeletedBridgeStatus(bridgeDTO));
                }
            }
        }
        return resourceStatusDTOs;
    }

    private ResourceStatusDTO getDeletedBridgeStatus(BridgeDTO bridgeDTO) {
        List<ConditionDTO> conditions = List.of(new ConditionDTO(DP_BRIDGE_DELETED_NAME, ConditionStatus.TRUE, ZonedDateTime.now(ZoneOffset.UTC)));
        return new ResourceStatusDTO(bridgeDTO.getId(), bridgeDTO.getGeneration(), conditions);
    }

    private void notifyBridgeStatus(List<ResourceStatusDTO> resourceStatusDTOs) {
        managerClient.notifyBridgeStatus(resourceStatusDTOs).subscribe().with(
                success -> LOGGER.debug("Successfully sends ManagedBridges status"),
                error -> LOGGER.error("Failed to send ManagedBridges status", error));
    }
}
