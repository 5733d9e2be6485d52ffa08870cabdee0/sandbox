package com.redhat.service.bridge.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.integration.tests.resources.BridgeResource;
import com.redhat.service.bridge.integration.tests.resources.ProcessorResource;

import io.cucumber.java.After;

public class Hooks {
    @After
    public void cleanUp() {
        String token = BridgeUtils.retrieveAccessToken();
        if (BridgeResource.getBridgeList(token).getItems().stream()
                .anyMatch(b -> b.getId().equals(StepsContext.bridgeId))) {
            if (ProcessorResource.getProcessorList(token, StepsContext.bridgeId).getSize() > 0) {
                ProcessorResource.getProcessorList(token, StepsContext.bridgeId).getItems().stream()
                        .forEach(p -> ProcessorResource.deleteProcessor(token, StepsContext.bridgeId, p.getId()));
                Awaitility.await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(5))
                        .until(() -> ProcessorResource.getProcessorList(token, StepsContext.bridgeId).getSize() == 0);
            }
            BridgeResource.deleteBridge(token, StepsContext.bridgeId);
        }
    }
}
