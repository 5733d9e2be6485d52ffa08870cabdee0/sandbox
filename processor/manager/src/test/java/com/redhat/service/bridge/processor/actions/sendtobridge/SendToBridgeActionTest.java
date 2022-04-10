package com.redhat.service.bridge.processor.actions.sendtobridge;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.processor.actions.ActionConfigurator;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SendToBridgeActionTest {

    @Inject
    SendToBridgeActionBean sendToBridgeAction;

    @Inject
    ActionConfigurator actionConfigurator;

    @Test
    void testType() {
        assertThat(sendToBridgeAction.getType()).isEqualTo(SendToBridgeAction.TYPE);
    }

    @Test
    void testValidator() {
        assertThat(actionConfigurator.getValidator(SendToBridgeAction.TYPE)).isNotNull();
    }
}
