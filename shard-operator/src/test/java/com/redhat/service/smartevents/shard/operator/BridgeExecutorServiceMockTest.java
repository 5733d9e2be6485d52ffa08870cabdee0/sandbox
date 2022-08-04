package com.redhat.service.smartevents.shard.operator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BridgeExecutorServiceMockTest extends AbstractBridgeServiceMockTest<BridgeExecutor> {

    private BridgeExecutorServiceImpl bridgeExecutorService;

    @Override
    Class<BridgeExecutor> getResourceClass() {
        return BridgeExecutor.class;
    }

    @Override
    void setupService() {
        bridgeExecutorService = new BridgeExecutorServiceImpl();
        bridgeExecutorService.kubernetesClient = kubernetesClient;
        bridgeExecutorService.customerNamespaceProvider = customerNamespaceProvider;
        bridgeExecutorService.templateProvider = templateProvider;
        bridgeExecutorService.managerClient = managerClient;
        bridgeExecutorService.executorImage = "imageName";

        when(templateProvider.loadBridgeExecutorSecretTemplate(any(), any())).thenReturn(k8sSecret);
        when(managerClient.notifyProcessorStatusChange(any(ProcessorDTO.class))).thenReturn(managerClientUniResponse);
    }

    @Test
    public void testBridgeExecutorCreationWhenSpecAlreadyExists() {
        // Given
        ProcessorDTO dto = TestSupport.newRequestedProcessorDTO();
        BridgeExecutor existingBridgeExecutor1 = BridgeExecutor.fromBuilder()
                .withBridgeId("bridgeId")
                .withProcessorId("processorId")
                .withProcessorName("processorName")
                .withProcessorType(ProcessorType.SINK)
                .withCustomerId("customerId")
                .withNamespace("namespace")
                .withImageName("imageName")
                .withDefinition(new ProcessorDefinition())
                .build();
        BridgeExecutor existingBridgeExecutor2 = BridgeExecutor.fromDTO(dto, "namespace", "imageName");

        // First pass Specs will not match, second pass they will
        when(k8sBridgeResource.get()).thenReturn(existingBridgeExecutor1, existingBridgeExecutor2);
        when(k8sBridgeNamespace.createOrReplace(any())).thenReturn(existingBridgeExecutor1);

        // The first pass should signal k8s to create the resource
        bridgeExecutorService.createBridgeExecutor(dto);
        verify(k8sBridgeNamespace).createOrReplace(any(BridgeExecutor.class));

        // The second pass resources match so signal back to the Manager
        bridgeExecutorService.createBridgeExecutor(dto);
        assertThat(dto.getStatus()).isEqualTo(ManagedResourceStatus.READY);
        verify(managerClient).notifyProcessorStatusChange(dto);
    }
}
