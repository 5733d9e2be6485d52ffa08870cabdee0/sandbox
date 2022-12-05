package com.redhat.service.smartevents.shard.operator.v2;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;

@QuarkusTest
public class ManagerBridgeSyncServiceTest {

    @Inject
    ManagedBridgeSyncService managedBridgeSyncService;

    @InjectMock
    ManagerClient managerClient;

    @InjectMock
    ManagedBridgeService managedBridgeService;

    @Test
    public void syncManagedBridgeWithManager() {

        // setup
        List<BridgeDTO> bridgeDTOList = new ArrayList<>();
        BridgeDTO bridgeDTO1 = new BridgeDTO();
        bridgeDTO1.setId("1");
        bridgeDTO1.setName("bridge1");
        bridgeDTO1.setCustomerId("cust1");
        bridgeDTO1.setEndpoint("http://testendpoint.com/test");
        bridgeDTO1.setKafkaConnection(new KafkaConnectionDTO());
        bridgeDTO1.setOwner("testOwner");
        bridgeDTOList.add(bridgeDTO1);

        BridgeDTO bridgeDTO2 = new BridgeDTO();
        bridgeDTO2.setId("2");
        bridgeDTO2.setName("bridge2");
        bridgeDTO2.setCustomerId("cust2");
        bridgeDTO2.setEndpoint("http://testendpoint.com/test");
        bridgeDTO2.setOperationType(OperationType.DELETE);
        bridgeDTO2.setKafkaConnection(new KafkaConnectionDTO());
        bridgeDTO2.setOwner("testOwner");
        bridgeDTOList.add(bridgeDTO2);

        Mockito.doNothing().when(managedBridgeService).createManagedBridge(bridgeDTO1);
        Mockito.doNothing().when(managedBridgeService).deleteManagedBridge(bridgeDTO2);
        Mockito.when(managerClient.fetchBridgesToDeployOrDelete()).thenReturn(Uni.createFrom().item(bridgeDTOList));

        // test
        managedBridgeSyncService.syncManagedBridgeWithManager();

        ArgumentCaptor<BridgeDTO> createBridgeArgumentCaptor = ArgumentCaptor.forClass(BridgeDTO.class);
        Mockito.verify(managedBridgeService).createManagedBridge(createBridgeArgumentCaptor.capture());
        BridgeDTO creationRequest = createBridgeArgumentCaptor.getValue();
        Assertions.assertThat(creationRequest.getId()).isEqualTo(bridgeDTO1.getId());

        ArgumentCaptor<BridgeDTO> deleteBridgeArgumentCaptor = ArgumentCaptor.forClass(BridgeDTO.class);
        Mockito.verify(managedBridgeService).deleteManagedBridge(deleteBridgeArgumentCaptor.capture());
        BridgeDTO deletionRequest = deleteBridgeArgumentCaptor.getValue();
        Assertions.assertThat(deletionRequest.getId()).isEqualTo(bridgeDTO2.getId());
    }
}
