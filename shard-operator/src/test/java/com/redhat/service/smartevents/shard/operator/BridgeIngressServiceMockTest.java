package com.redhat.service.smartevents.shard.operator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BridgeIngressServiceMockTest extends AbstractBridgeServiceMockTest<BridgeIngress> {

    private BridgeIngressServiceImpl bridgeIngressService;

    @Override
    Class<BridgeIngress> getResourceClass() {
        return BridgeIngress.class;
    }

    @Override
    void setupService() {
        bridgeIngressService = new BridgeIngressServiceImpl();
        bridgeIngressService.kubernetesClient = kubernetesClient;
        bridgeIngressService.customerNamespaceProvider = customerNamespaceProvider;
        bridgeIngressService.templateProvider = templateProvider;
        bridgeIngressService.managerClient = managerClient;

        when(templateProvider.loadBridgeIngressSecretTemplate(any(), any())).thenReturn(k8sSecret);
        when(managerClient.notifyBridgeStatusChange(any(BridgeDTO.class))).thenReturn(managerClientUniResponse);
    }

    @Test
    public void testBridgeIngressCreationWhenSpecAlreadyExists() {
        // Given
        BridgeDTO dto = TestSupport.newProvisioningBridgeDTO();
        BridgeIngress existingBridgeIngress1 = BridgeIngress.fromBuilder()
                .withBridgeId("bridgeId")
                .withBridgeName("bridge")
                .withCustomerId("customerId")
                .withNamespace("namespace")
                .build();
        BridgeIngress existingBridgeIngress2 = BridgeIngress.fromDTO(dto, "namespace");

        // First pass Specs will not match, second pass they will
        when(k8sBridgeResource.get()).thenReturn(existingBridgeIngress1, existingBridgeIngress2);
        when(k8sBridgeNamespace.createOrReplace(any())).thenReturn(existingBridgeIngress1);

        // The first pass should signal k8s to create the resource
        bridgeIngressService.createBridgeIngress(dto);
        verify(k8sBridgeNamespace).createOrReplace(any(BridgeIngress.class));

        // The second pass resources match so signal back to the Manager
        bridgeIngressService.createBridgeIngress(dto);
        assertThat(dto.getStatus()).isEqualTo(ManagedResourceStatus.READY);
        verify(managerClient).notifyBridgeStatusChange(dto);
    }
}
