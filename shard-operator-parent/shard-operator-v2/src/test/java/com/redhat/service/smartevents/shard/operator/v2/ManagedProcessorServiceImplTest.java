package com.redhat.service.smartevents.shard.operator.v2;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.CamelIntegration;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static com.redhat.service.smartevents.shard.operator.v2.utils.AwaitilityUtil.await;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
public class ManagedProcessorServiceImplTest {

    @Inject
    ManagedProcessorService bridgeIngressService;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {

        // Far from ideal... but each test assumes there are no other BridgeIngress instances in existence.
        // Unfortunately, however, some tests only check that provisioning either progressed to a certain
        // point of failed completely. There is therefore a good chance there's an incomplete BridgeIngress
        // in k8s when a subsequent test starts. This leads to non-deterministic behaviour of tests.
        // This ensures each test has a "clean" k8s environment.
        await(Duration.ofMinutes(1),
                Duration.ofSeconds(10),
                () -> assertThat(kubernetesClient.resources(ManagedBridge.class).inAnyNamespace().list().getItems().isEmpty()).isTrue());
    }

    @Test
    public void testCamelIntegrationIsProvisioned() throws JsonProcessingException {

        String namespace = "namespace";
        ObjectNode flow = objectMapper.readValue("{\"from\":{\"uri\":\"fromURI\",\"steps\":[{\"to\":\"toURI\"}]}}", ObjectNode.class);
        ProcessorDTO processorDTO = new ProcessorDTO("processorId", "processorName", flow, "bridgeId", "customerId", "owner", OperationType.CREATE);

        bridgeIngressService.createManagedProcessor(processorDTO, namespace);
        waitUntiManagedProcessorExists(processorDTO, namespace);
        ManagedProcessor managedProcessor = fetchManagedProcessorByDTONamespace(processorDTO, namespace);

        String integrationName = "integration-" + processorDTO.getName();
        CamelIntegration camelIntegration = bridgeIngressService.fetchOrCreateCamelIntegration(managedProcessor, integrationName);

        assertThat(camelIntegration).isNotNull();
        assertThat(camelIntegration.getSpec().getFlows().get(0)).isEqualTo(flow);
        waitUntiCamelIntegrationExists(namespace, integrationName);
    }

    private void waitUntiCamelIntegrationExists(String namespace, String name) {
        await(Duration.ofSeconds(30),
                Duration.ofMillis(200),
                () -> {
                    CamelIntegration bridgeIngress = kubernetesClient
                            .resources(CamelIntegration.class)
                            .inNamespace(namespace)
                            .withName(name)
                            .get();
                    assertThat(bridgeIngress).isNotNull();
                });
    }

    private void waitUntiManagedProcessorExists(ProcessorDTO processor, String namespace) {
        await(Duration.ofSeconds(30),
                Duration.ofMillis(200),
                () -> {
                    ManagedProcessor managedProcessor = fetchManagedProcessorByDTONamespace(processor, namespace);
                    assertThat(managedProcessor).isNotNull();
                });
    }

    private ManagedProcessor fetchManagedProcessorByDTONamespace(ProcessorDTO processor, String namespace) {
        return kubernetesClient
                .resources(ManagedProcessor.class)
                .inNamespace(namespace)
                .withName(ManagedProcessor.resolveResourceName(processor.getId()))
                .get();
    }
}
