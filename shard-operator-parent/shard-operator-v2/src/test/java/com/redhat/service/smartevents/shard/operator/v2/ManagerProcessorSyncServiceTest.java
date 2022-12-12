package com.redhat.service.smartevents.shard.operator.v2;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.v2.utils.Fixtures;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;
import io.smallrye.mutiny.Uni;

import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
@WithOpenShiftTestServer
public class ManagerProcessorSyncServiceTest {

    @Inject
    ManagedProcessorSyncService managedProcessorSyncService;

    @InjectMock
    ManagerClient managerClient;

    @InjectMock
    ManagedProcessorService managedProcessorService;

    @Test
    public void syncManagedProcessorWithManager() {

        // setup
        List<ProcessorDTO> processorDTOList = new ArrayList<>();
        ProcessorDTO processorDTO1 = Fixtures.createProcessor(OperationType.CREATE);
        processorDTO1.setId("1");
        processorDTOList.add(processorDTO1);

        ProcessorDTO processorDTO2 = Fixtures.createProcessor(OperationType.DELETE);
        processorDTO2.setId("2");
        processorDTOList.add(processorDTO2);

        Mockito.doNothing().when(managedProcessorService).createManagedProcessor(processorDTO1, anyString());
        Mockito.doNothing().when(managedProcessorService).deleteManagedProcessor(processorDTO2);
        Mockito.when(managerClient.fetchProcessorsToDeployOrDelete()).thenReturn(Uni.createFrom().item(processorDTOList));

        // test
        managedProcessorSyncService.syncManagedProcessorWithManager();

        ArgumentCaptor<ProcessorDTO> createProcessorArgumentCaptor = ArgumentCaptor.forClass(ProcessorDTO.class);
        Mockito.verify(managedProcessorService).createManagedProcessor(createProcessorArgumentCaptor.capture(), anyString());
        ProcessorDTO creationRequest = createProcessorArgumentCaptor.getValue();
        Assertions.assertThat(creationRequest.getId()).isEqualTo(processorDTO1.getId());

        ArgumentCaptor<ProcessorDTO> deleteProcessorArgumentCaptor = ArgumentCaptor.forClass(ProcessorDTO.class);
        Mockito.verify(managedProcessorService).deleteManagedProcessor(deleteProcessorArgumentCaptor.capture());
        ProcessorDTO deletionRequest = deleteProcessorArgumentCaptor.getValue();
        Assertions.assertThat(deletionRequest.getId()).isEqualTo(processorDTO2.getId());
    }
}
