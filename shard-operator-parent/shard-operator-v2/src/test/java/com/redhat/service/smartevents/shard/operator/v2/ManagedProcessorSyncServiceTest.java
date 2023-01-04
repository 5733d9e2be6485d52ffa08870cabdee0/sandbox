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
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;
import com.redhat.service.smartevents.shard.operator.v2.utils.Fixtures;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;
import io.smallrye.mutiny.Uni;

import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_PROCESSOR_DELETED_NAME;

@QuarkusTest
@WithOpenShiftTestServer
public class ManagedProcessorSyncServiceTest {

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

        Mockito.doNothing().when(managedProcessorService).createManagedProcessor(processorDTO1);
        Mockito.doNothing().when(managedProcessorService).deleteManagedProcessor(processorDTO2);
        Mockito.when(managerClient.fetchProcessorsForDataPlane()).thenReturn(Uni.createFrom().item(processorDTOList));

        // test
        managedProcessorSyncService.syncManagedProcessorWithManager();

        ArgumentCaptor<ProcessorDTO> createProcessorArgumentCaptor = ArgumentCaptor.forClass(ProcessorDTO.class);
        Mockito.verify(managedProcessorService).createManagedProcessor(createProcessorArgumentCaptor.capture());
        ProcessorDTO creationRequest = createProcessorArgumentCaptor.getValue();
        Assertions.assertThat(creationRequest.getId()).isEqualTo(processorDTO1.getId());

        ArgumentCaptor<ProcessorDTO> deleteProcessorArgumentCaptor = ArgumentCaptor.forClass(ProcessorDTO.class);
        Mockito.verify(managedProcessorService).deleteManagedProcessor(deleteProcessorArgumentCaptor.capture());
        ProcessorDTO deletionRequest = deleteProcessorArgumentCaptor.getValue();
        Assertions.assertThat(deletionRequest.getId()).isEqualTo(processorDTO2.getId());
    }

    @Test
    public void TestSyncManagedProcessorStatusBackToManager() {
        // setup
        List<ProcessorDTO> processorDTOList = new ArrayList<>();
        ProcessorDTO processorDTO1 = Fixtures.createProcessor(OperationType.CREATE);
        processorDTO1.setId("1");
        processorDTO1.setGeneration(1);
        processorDTOList.add(processorDTO1);

        ProcessorDTO processorDTO2 = Fixtures.createProcessor(OperationType.DELETE);
        processorDTO2.setId("2");
        processorDTO2.setGeneration(2);
        processorDTOList.add(processorDTO2);

        Mockito.when(managerClient.fetchProcessorsForDataPlane()).thenReturn(Uni.createFrom().item(processorDTOList));
        ManagedProcessor managedProcessor = Fixtures.createManagedProcessor(processorDTO1, "test-namespace");
        Mockito.when(managedProcessorService.fetchAllManagedProcessors()).thenReturn(List.of(managedProcessor));

        // test
        managedProcessorSyncService.syncManagedProcessorStatusBackToManager();

        // assert
        ArgumentCaptor<List<ResourceStatusDTO>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(managerClient).notifyProcessorStatus(argumentCaptor.capture());
        List<ResourceStatusDTO> processorStatusDTOs = argumentCaptor.getValue();
        Assertions.assertThat(processorStatusDTOs).size().isEqualTo(2);

        ResourceStatusDTO processorStatusDTO1 = processorStatusDTOs.get(0);
        Assertions.assertThat(processorStatusDTO1.getId()).isEqualTo(processorDTO1.getId());
        Assertions.assertThat(processorStatusDTO1.getGeneration()).isEqualTo(processorDTO1.getGeneration());
        Assertions.assertThat(processorStatusDTO1.getConditions().size()).isEqualTo(managedProcessor.getStatus().getConditions().size());
        Assertions.assertThat(processorStatusDTO1.getConditions().stream().allMatch(c -> c.getStatus() == ConditionStatus.UNKNOWN)).isTrue();

        ResourceStatusDTO processorStatusDTO2 = processorStatusDTOs.get(1);
        Assertions.assertThat(processorStatusDTO2.getId()).isEqualTo(processorDTO2.getId());
        Assertions.assertThat(processorStatusDTO2.getGeneration()).isEqualTo(processorDTO2.getGeneration());
        Assertions.assertThat(processorStatusDTO2.getConditions().size()).isEqualTo(1);
        ConditionDTO conditionDTO = processorStatusDTO2.getConditions().iterator().next();
        Assertions.assertThat(conditionDTO.getType()).isEqualTo(DP_PROCESSOR_DELETED_NAME);
        Assertions.assertThat(conditionDTO.getStatus()).isEqualTo(ConditionStatus.TRUE);
    }
}
