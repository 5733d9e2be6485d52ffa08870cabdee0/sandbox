package com.redhat.service.smartevents.shard.operator.v2;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeStatusDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.utils.Fixtures;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;
import io.smallrye.mutiny.Uni;

import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_BRIDGE_DELETED_NAME;

@QuarkusTest
@WithOpenShiftTestServer
public class ManagedBridgeSyncServiceTest {

    @Inject
    ManagedBridgeSyncServiceImpl managedBridgeSyncService;

    @InjectMock
    ManagerClient managerClient;

    @InjectMock
    ManagedBridgeService managedBridgeService;

    @Test
    public void syncManagedBridgeWithManager() {

        // setup
        List<BridgeDTO> bridgeDTOList = new ArrayList<>();
        BridgeDTO bridgeDTO1 = Fixtures.createBridge(OperationType.CREATE);
        bridgeDTO1.setId("1");
        bridgeDTOList.add(bridgeDTO1);

        BridgeDTO bridgeDTO2 = Fixtures.createBridge(OperationType.DELETE);
        bridgeDTO2.setId("2");
        bridgeDTOList.add(bridgeDTO2);

        Mockito.when(managerClient.fetchBridgesForDataPlane()).thenReturn(Uni.createFrom().item(bridgeDTOList));

        // test
        managedBridgeSyncService.syncManagedBridgeWithManager();

        // assert
        ArgumentCaptor<BridgeDTO> createBridgeArgumentCaptor = ArgumentCaptor.forClass(BridgeDTO.class);
        Mockito.verify(managedBridgeService).createManagedBridge(createBridgeArgumentCaptor.capture());
        BridgeDTO creationRequest = createBridgeArgumentCaptor.getValue();
        Assertions.assertThat(creationRequest.getId()).isEqualTo(bridgeDTO1.getId());

        ArgumentCaptor<BridgeDTO> deleteBridgeArgumentCaptor = ArgumentCaptor.forClass(BridgeDTO.class);
        Mockito.verify(managedBridgeService).deleteManagedBridge(deleteBridgeArgumentCaptor.capture());
        BridgeDTO deletionRequest = deleteBridgeArgumentCaptor.getValue();
        Assertions.assertThat(deletionRequest.getId()).isEqualTo(bridgeDTO2.getId());
    }

    @Test
    public void TestSyncManagedBridgeStatusBackToManager() {
        // setup
        List<BridgeDTO> bridgeDTOList = new ArrayList<>();
        BridgeDTO bridgeDTO1 = Fixtures.createBridge(OperationType.CREATE);
        bridgeDTO1.setId("1");
        bridgeDTO1.setGeneration(1);
        bridgeDTOList.add(bridgeDTO1);

        BridgeDTO bridgeDTO2 = Fixtures.createBridge(OperationType.DELETE);
        bridgeDTO2.setId("2");
        bridgeDTO2.setGeneration(2);
        bridgeDTOList.add(bridgeDTO2);

        Mockito.when(managerClient.fetchBridgesForDataPlane()).thenReturn(Uni.createFrom().item(bridgeDTOList));
        ManagedBridge managedBridge = Fixtures.createManagedBridge(bridgeDTO1, "test-namespace");
        Mockito.when(managedBridgeService.fetchAllManagedBridges()).thenReturn(List.of(managedBridge));

        // test
        managedBridgeSyncService.syncManagedBridgeStatusBackToManager();

        // assert
        ArgumentCaptor<List<BridgeStatusDTO>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(managerClient).notifyBridgeStatus(argumentCaptor.capture());
        List<BridgeStatusDTO> bridgeStatusDTOs = argumentCaptor.getValue();
        Assertions.assertThat(bridgeStatusDTOs).size().isEqualTo(2);

        BridgeStatusDTO bridgeStatusDTO1 = bridgeStatusDTOs.get(0);
        Assertions.assertThat(bridgeStatusDTO1.getId()).isEqualTo(bridgeDTO1.getId());
        Assertions.assertThat(bridgeStatusDTO1.getGeneration()).isEqualTo(bridgeDTO1.getGeneration());
        Assertions.assertThat(bridgeStatusDTO1.getConditions().size()).isEqualTo(managedBridge.getStatus().getConditions().size());
        Assertions.assertThat(bridgeStatusDTO1.getConditions().stream().allMatch(c -> c.getStatus() == ConditionStatus.UNKNOWN)).isTrue();

        BridgeStatusDTO bridgeStatusDTO2 = bridgeStatusDTOs.get(1);
        Assertions.assertThat(bridgeStatusDTO2.getId()).isEqualTo(bridgeDTO2.getId());
        Assertions.assertThat(bridgeStatusDTO2.getGeneration()).isEqualTo(bridgeDTO2.getGeneration());
        Assertions.assertThat(bridgeStatusDTO2.getConditions().size()).isEqualTo(1);
        ConditionDTO conditionDTO = bridgeStatusDTO2.getConditions().iterator().next();
        Assertions.assertThat(conditionDTO.getType()).isEqualTo(DP_BRIDGE_DELETED_NAME);
        Assertions.assertThat(conditionDTO.getStatus()).isEqualTo(ConditionStatus.TRUE);
    }
}
