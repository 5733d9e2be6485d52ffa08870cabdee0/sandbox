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
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

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

    @InjectMock
    NamespaceProvider namespaceProvider;

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

        Mockito.doNothing().when(managedBridgeService).createManagedBridgeResources(Mockito.any(ManagedBridge.class));
        Mockito.doNothing().when(managedBridgeService).deleteManagedBridgeResources(Mockito.any(ManagedBridge.class));
        Mockito.when(managerClient.fetchBridgesToDeployOrDelete()).thenReturn(Uni.createFrom().item(bridgeDTOList));
        Mockito.when(namespaceProvider.getNamespaceName(Mockito.anyString())).thenReturn("testNamespace");

        // test
        managedBridgeSyncService.syncManagedBridgeWithManager();

        ArgumentCaptor<ManagedBridge> createBridgeArgumentCaptor = ArgumentCaptor.forClass(ManagedBridge.class);
        Mockito.verify(managedBridgeService).createManagedBridgeResources(createBridgeArgumentCaptor.capture());
        ManagedBridge creationRequest = createBridgeArgumentCaptor.getValue();
        Assertions.assertThat(creationRequest.getSpec().getId()).isEqualTo(bridgeDTO1.getId());

        ArgumentCaptor<ManagedBridge> deleteBridgeArgumentCaptor = ArgumentCaptor.forClass(ManagedBridge.class);
        Mockito.verify(managedBridgeService).deleteManagedBridgeResources(deleteBridgeArgumentCaptor.capture());
        ManagedBridge deletionRequest = deleteBridgeArgumentCaptor.getValue();
        Assertions.assertThat(deletionRequest.getSpec().getId()).isEqualTo(bridgeDTO2.getId());
    }
}
