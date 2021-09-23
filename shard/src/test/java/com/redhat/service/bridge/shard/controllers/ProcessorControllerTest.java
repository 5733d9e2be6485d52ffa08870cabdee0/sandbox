package com.redhat.service.bridge.shard.controllers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.k8s.Action;
import com.redhat.service.bridge.infra.k8s.K8SBridgeConstants;
import com.redhat.service.bridge.infra.k8s.KubernetesClient;
import com.redhat.service.bridge.infra.k8s.KubernetesResourceType;
import com.redhat.service.bridge.infra.k8s.ResourceEvent;
import com.redhat.service.bridge.infra.k8s.crds.ProcessorCustomResource;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.AbstractShardWireMockTest;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.quarkus.test.junit.QuarkusTest;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class ProcessorControllerTest extends AbstractShardWireMockTest {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    Event<ResourceEvent> event;

    @Test
    public void reconcileProcessor() throws JsonProcessingException, InterruptedException {
        BridgeDTO bridge = new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.AVAILABLE);
        ProcessorDTO processor = createProcessor(bridge, BridgeStatus.REQUESTED);

        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(1);
        addProcessorUpdateRequestListener(latch);

        kubernetesClient.createOrUpdateCustomResource(processor.getId(), ProcessorCustomResource.fromDTO(processor), K8SBridgeConstants.PROCESSOR_TYPE);

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));

        processor.setStatus(BridgeStatus.AVAILABLE);

        WireMock.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(processor), true, true))
                .withHeader("Content-Type", equalTo("application/json")));

        Deployment deployment = kubernetesClient.getDeployment(processor.getId());
        Assertions.assertTrue(deployment.getStatus().getConditions().stream().anyMatch(x -> x.getStatus().equals("Ready")));
        Assertions.assertEquals(BridgeStatus.AVAILABLE, kubernetesClient.getCustomResource(processor.getId(), ProcessorCustomResource.class).getStatus());
    }

    @Test
    public void reconcileProcessor_withFailure() throws JsonProcessingException, InterruptedException {

        BridgeDTO bridge = new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.AVAILABLE);
        ProcessorDTO processor = createProcessor(bridge, BridgeStatus.REQUESTED);

        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(1);
        addProcessorUpdateRequestListener(latch);

        kubernetesClient.createOrUpdateCustomResource(processor.getId(), ProcessorCustomResource.fromDTO(processor), K8SBridgeConstants.PROCESSOR_TYPE);

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));

        latch = new CountDownLatch(1);
        addProcessorUpdateRequestListener(latch);
        event.fire(new ResourceEvent(KubernetesResourceType.DEPLOYMENT, K8SBridgeConstants.PROCESSOR_TYPE, processor.getId(), Action.ERROR));
        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));

        processor.setStatus(BridgeStatus.FAILED);

        WireMock.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(processor), true, true))
                .withHeader("Content-Type", equalTo("application/json")));

        Assertions.assertEquals(BridgeStatus.FAILED, kubernetesClient.getCustomResource(processor.getId(), ProcessorCustomResource.class).getStatus());
    }
}
