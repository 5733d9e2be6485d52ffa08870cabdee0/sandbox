package com.redhat.service.smartevents.shard.operator.v2;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeStatusDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.shard.operator.v2.converters.BridgeStatusConverter;
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

    private List<BridgeStatusDTO> transformToBridgeStatus(List<BridgeDTO> bridgeDTOList) {
        Map<String, ManagedBridge> deployedManagedBridges = managedBridgeService.fetchAllManagedBridges()
                .stream().collect(Collectors.toMap(m -> m.getSpec().getId(), m -> m));

        List<BridgeStatusDTO> bridgeStatusDTOs = new ArrayList<>(bridgeDTOList.size());
        for (BridgeDTO bridgeDTO : bridgeDTOList) {
            ManagedBridge deployedManagedBridge = deployedManagedBridges.get(bridgeDTO.getId());
            if (deployedManagedBridge != null) {
                bridgeStatusDTOs.add(BridgeStatusConverter.fromManagedBridgeToBridgeStatusDTO(deployedManagedBridge));
            } else {
                if (bridgeDTO.getOperationType() == OperationType.DELETE) {
                    bridgeStatusDTOs.add(getDeletedBridgeStatus(bridgeDTO));
                }
            }
        }
        return bridgeStatusDTOs;
    }

    private BridgeStatusDTO getDeletedBridgeStatus(BridgeDTO bridgeDTO) {
        Set<ConditionDTO> conditions = new HashSet<>();
        conditions.add(new ConditionDTO(DP_BRIDGE_DELETED_NAME, ConditionStatus.TRUE, ZonedDateTime.now(ZoneOffset.UTC)));
        return new BridgeStatusDTO(bridgeDTO.getId(), bridgeDTO.getGeneration(), conditions);
    }

    private void notifyBridgeStatus(List<BridgeStatusDTO> bridgeStatusDTOs) {
        managerClient.notifyBridgeStatus(bridgeStatusDTOs).subscribe().with(
                success -> LOGGER.debug("Successfully sends ManagedBridges status"),
                error -> LOGGER.error("Failed to send ManagedBridges status", error));
    }
}
