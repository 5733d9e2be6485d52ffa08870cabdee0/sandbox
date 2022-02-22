package com.redhat.service.bridge.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.bridge.integration.tests.common.BridgeCommon;

import io.cucumber.java.After;

public class Hooks {
    @After
    public void cleanUp() {
        if (BridgeCommon.getBridgeList().getItems().stream().anyMatch(b -> b.getId().equals(StepsContext.bridgeId))) {
            if (BridgeCommon.listProcessors(StepsContext.bridgeId).getSize() > 0) {
                BridgeCommon.listProcessors(StepsContext.bridgeId).getItems().stream().forEach(p -> BridgeCommon.deleteProcessor(StepsContext.bridgeId, p.getId()));
                Awaitility.await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(5))
                        .until(() -> BridgeCommon.listProcessors(StepsContext.bridgeId).getSize() == 0);
            }
            BridgeCommon.deleteBridge(StepsContext.bridgeId);
        }
    }
}
